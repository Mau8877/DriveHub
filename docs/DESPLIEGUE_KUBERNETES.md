# Plan de despliegue en Kubernetes: microservicios repartidos en Google Cloud, Azure y AWS

> **Estado:** propuesta, nada implementado. `docker-compose.yml` sigue siendo el entorno de desarrollo. Cuando
> esto exista, se actualizan [`.claude/rules/CI_CD.md`](../.claude/rules/CI_CD.md) y la sección 12 de
> [INFRAESTRUCTURA.md](INFRAESTRUCTURA.md).

## 1. Idea

Un solo DriveHub, con sus microservicios **repartidos en tres clústeres de Kubernetes** (uno por nube) que se
comunican entre sí por una **red privada de Tailscale**. Se busca la forma **más simple** de lograrlo:

- **Cada base de datos corre en un contenedor dentro del clúster** donde está el servicio que la usa, como en
  `docker-compose.yml`. Ninguna base se consulta desde otra nube.
- **Tailscale une los tres clústeres** con túneles cifrados (WireGuard). Un servicio de otra nube aparece como un
  `Service` local con su nombre de siempre: el gateway llama a `http://pagos:8083` aunque Pagos esté en Azure.
  **Las URLs son las mismas que en Docker Compose** (sin el prefijo `drivehub-`).
- **Solo la entrada es pública** (la web, la API y los archivos). Ningún microservicio ni RabbitMQ tiene un
  nombre en internet.
- **Una réplica de cada servicio** y manifiestos de **Kustomize** (incluido en `kubectl`).

## 2. Qué va en cada nube

```
                              Navegador · app móvil · Stripe
                                          │  https://drivehub.<dominio>
 ┌──────────────────────────── GOOGLE CLOUD · GKE ────────────────────────────┐
 │  ingress público:  /api → gateway      / → web (shell + 6 remotos)         │
 │  identidad ⇄ escuela                                                       │
 │  postgres (bases identidad, escuela)                                       │
 └───────────┬──────────────────────────────────────────────┬────────────────┘
             │              red privada de Tailscale         │
 ┌───────────▼──────────── AZURE · AKS ──────┐   ┌────────────▼──────────── AWS · EKS ─────────────┐
 │  pagos ⇄ academico  (saga de horas)       │   │  notificaciones · asistente-ia · contenido      │
 │  postgres (base pagos) · mongo (academico)│◀─▶│  rabbitmq (bus de eventos de todos)             │
 │                                           │   │  mongo (contenido)                              │
 └───────────────────────────────────────────┘   │  DynamoDB (servicio de AWS)                     │
                                                 └─────────────────────────────────────────────────┘
          Archivos de las tres nubes: S3 de AWS (bucket amz-drivehub, ver §4.1)
```

| Nube | Servicios | Datos y apoyo (contenedores en el clúster) | Por qué juntos |
| --- | --- | --- | --- |
| **Google Cloud (GKE)** | `gateway`, `web`, `identidad`, `escuela` | `postgres` (bases `identidad` y `escuela`) | Es la entrada pública. Identidad y Escuela se llaman entre sí todo el tiempo, y el login no sale de GCP |
| **Azure (AKS)** | `pagos`, `academico` | `postgres` (base `pagos`), `mongo` (base `academico`, replica set) | Reservar una clase descuenta una hora de la billetera dentro de la misma request (CU14) y el orquestador retoma reservas a medias (CU44): esa saga queda dentro de una sola nube |
| **AWS (EKS)** | `notificaciones`, `asistente-ia`, `contenido` | `rabbitmq`, `mongo` (base `contenido`), **DynamoDB** (servicio de AWS) | DynamoDB solo existe en AWS. El asistente consulta Contenido (CU46) sin salir de AWS. Notificaciones consume la cola de RabbitMQ |

DynamoDB es la única pieza que no es un contenedor: no hay una versión para producción que corra en un
contenedor (`dynamodb-local` es solo un emulador de pruebas). Como `notificaciones` y `asistente-ia` corren en AWS,
la usan en la misma nube.

Las tres regiones se eligen **en el mismo lugar físico** para que una llamada entre nubes tarde pocos
milisegundos: GCP `us-east4`, Azure `eastus` y AWS `us-east-1` (las tres en el norte de Virginia).

## 3. Cómo se comunican: Tailscale

### 3.1 Cómo funciona

En cada clúster se instala el **operador de Tailscale para Kubernetes**. Hace dos cosas, y las dos se piden con
anotaciones en un `Service`:

1. **Publicar un servicio en la red privada (ingress).** En el clúster dueño, el `Service` del servicio lleva:

   ```yaml
   metadata:
     name: pagos
     annotations:
       tailscale.com/expose: "true"
       tailscale.com/hostname: "pagos-azure"     # queda como pagos-azure.<tailnet>.ts.net
   ```

2. **Usar un servicio de otra nube (egress).** En el clúster que lo llama, se crea un `Service` **con el mismo
   nombre** que apunta a ese nombre de Tailscale:

   ```yaml
   apiVersion: v1
   kind: Service
   metadata:
     name: pagos
     annotations:
       tailscale.com/tailnet-fqdn: "pagos-azure.<tailnet>.ts.net"
   spec:
     type: ExternalName
     externalName: no-se-usa               # el operador lo reemplaza por su proxy
     ports:
       - name: http
         port: 8083
   ```

Así, en GCP, `http://pagos:8083` llega a Pagos en Azure por el túnel cifrado. Ningún servicio sabe en qué nube
está el otro, y mover un servicio de nube es mover su `Service` de egress, no cambiar variables.

### 3.2 Servicios publicados y quién los usa

| Publicado en Tailscale | Nube dueña | Lo usan (Service de egress con el mismo nombre en...) |
| --- | --- | --- |
| `identidad` (8081) | GCP | Azure (pagos, academico), AWS (contenido, notificaciones, asistente-ia; también n8n si se suma) |
| `escuela` (8082) | GCP | Azure (pagos, academico) |
| `pagos` (8083) | Azure | GCP (gateway), AWS (asistente-ia) |
| `academico` (8084) | Azure | GCP (gateway, escuela), AWS (asistente-ia) |
| `contenido` (8085) | AWS | GCP (gateway) |
| `notificaciones` (8086) | AWS | GCP (gateway), Azure (pagos, academico) |
| `asistente-ia` (8087) | AWS | GCP (gateway) |
| `rabbitmq` (5672) | AWS | GCP y Azure (los servicios que publican eventos) |

Las variables de los servicios quedan iguales a las de Docker Compose sin el prefijo del contenedor:
`PAGOS_URL=http://pagos:8083`, `IDENTIDAD_URL=http://identidad:8081`, `SPRING_RABBITMQ_HOST=rabbitmq`... **en las
tres nubes por igual**. El gateway no reescribe la ruta, así que Pagos recibe el mismo `/api/v1/...` que pidió el
cliente.

### 3.3 Seguridad

- **Cifrado**: todo el tráfico entre nubes viaja por WireGuard. Dentro del túnel se usa HTTP y AMQP simples, sin
  certificados internos.
- **Nada interno en internet**: los servicios y RabbitMQ no tienen IP ni nombre públicos.
- **Quién puede hablar con quién**: la política de acceso de Tailscale (en su consola) solo permite que los
  dispositivos con la etiqueta `tag:k8s` (los proxies de los tres clústeres) se conecten entre sí y solo a los
  puertos de la tabla de §3.2. Tu laptop no entra salvo que se le dé permiso a propósito (útil para depurar).
- **La seguridad del código no cambia**: cada servicio valida el JWT por su cuenta y las rutas internas
  (`/internal/**`) siguen exigiendo `X-Service-Key`.

### 3.4 Si la otra nube no responde

| Desde → hacia | Qué | Si falla |
| --- | --- | --- |
| gateway (GCP) → Azure, AWS | Las rutas `/api/v1/...` de esos recursos | Esas pantallas fallan; el resto funciona |
| pagos, academico (Azure) → identidad, escuela (GCP) | Perfil y nombres de estudiantes, alumnos del instructor, JWKS | Esas operaciones responden 503 |
| escuela (GCP) → academico (Azure) | Resumen de franjas (CU11) | La disponibilidad de instructores responde 503 |
| pagos, academico (Azure) → notificaciones (AWS) | Avisos (CU36) | Nada se pierde: los avisos son de mejor esfuerzo |
| servicios de GCP y Azure → rabbitmq (AWS) | Publicar eventos | Los eventos esperan en el outbox y salen cuando vuelve la conexión |
| asistente-ia (AWS) → pagos, academico (Azure) | Chat y MCP (CU46, CU49) | El asistente responde que no puede consultar |
| contenido, notificaciones, asistente-ia (AWS) → identidad (GCP) | JWKS para validar el JWT | Siguen validando con la clave ya descargada |

## 4. Manifiestos (Kustomize)

```
infrastructure/k8s/
├── componentes/                  # Una carpeta por pieza, igual en cualquier nube
│   ├── gateway/  identidad/  escuela/  pagos/  academico/
│   ├── contenido/  notificaciones/  asistente-ia/  web/
│   └── postgres/  mongo/  rabbitmq/  minio/ (minio solo en el overlay local)
├── enlaces/                      # Un Service de egress de Tailscale por servicio (§3.1, punto 2)
│   └── identidad.yaml  escuela.yaml  pagos.yaml  academico.yaml  contenido.yaml
│       notificaciones.yaml  asistente-ia.yaml  rabbitmq.yaml
└── nubes/
    ├── gcp/kustomization.yaml    # componentes: gateway, web, identidad, escuela, postgres
    │                             # + publicar identidad y escuela en Tailscale (parche con las anotaciones)
    │                             # + enlaces: pagos, academico, contenido, notificaciones, asistente-ia, rabbitmq
    │                             # + ingress público
    ├── azure/kustomization.yaml  # componentes: pagos, academico, postgres, mongo
    │                             # + publicar pagos y academico · enlaces: identidad, escuela, notificaciones, rabbitmq
    └── aws/kustomization.yaml    # componentes: notificaciones, asistente-ia, contenido, rabbitmq, mongo
                                  # + publicar los cuatro · enlaces: identidad, pagos, academico
```

**Mover un servicio de nube** = pasar su carpeta de `componentes/` al `kustomization.yaml` de otra nube, publicarlo
desde ahí y ajustar qué nubes llevan su enlace. No cambia ninguna variable de ningún servicio.

| Pieza | En Kubernetes |
| --- | --- |
| Servicios y web | `Deployment` (1 réplica) + `Service`, con `readinessProbe` y `livenessProbe` sobre `/health` |
| `postgres` | `StatefulSet` `postgres:18-alpine` + disco de 10 Gi. El script de `infrastructure/postgres/init/` en un ConfigMap, adaptado para crear solo las bases de esa nube |
| `mongo` | `StatefulSet` `mongo:8` replica set de un nodo (`rs0`) + disco de 10 Gi; su readiness inicia el replica set la primera vez (como el healthcheck de docker-compose) |
| `rabbitmq` | `StatefulSet` + disco de 2 Gi |
| Archivos | **S3 de AWS**, fuera de los clústeres (§4.1). `componentes/minio` queda solo para el overlay local |

### 4.1 Archivos en S3

Los cuatro servicios con archivos ya usaban el cliente S3 contra MinIO; en la nube apuntan a S3 y comparten el bucket
`amz-drivehub` (`us-east-1`), una carpeta por servicio: `usuarios/` (identidad, privada), `comprobantes/` (pagos,
privada), `escuelas/` (contenido, pública), `ayudas-tablero/` (asistente-ia, pública) y `modelos-ia/` (los modelos que
descarga la app móvil, pública). Lo privado se lee con URLs prefirmadas.

- **Política del bucket**: `infrastructure/k8s/nubes/aws/politica-s3-archivos.json` (lectura pública solo de las tres
  carpetas públicas). Para aplicarla hay que desactivar en el bucket el bloqueo de acceso público por políticas.
- **Credenciales**: la access key y la secret key de un usuario IAM con acceso al bucket van en `drivehub-secretos` de
  cada nube como `MINIO_ROOT_USER` y `MINIO_ROOT_PASSWORD` (los nombres de siempre, así los servicios no cambian).
- **Bucket de cada servicio**, en el `entorno.env` de su nube: `APP_MINIO_BUCKETNAME` (identidad),
  `MINIO_BUCKET_COMPROBANTES` (pagos) y `MINIO_BUCKET` (contenido y asistente-ia).

**Ingress público** (Traefik + cert-manager con Let's Encrypt), lo único con nombre en internet. Se usa Traefik y
no `ingress-nginx` porque este último se retiró en marzo de 2026 y ya no recibe parches de seguridad; Traefik
acepta los mismos recursos `Ingress` estándar:

| Nombre | Va a |
| --- | --- |
| `drivehub.<dominio>` | `/api` → gateway, `/` → web (GKE) |

**Secretos**: no se commitean. En cada clúster se crean desde un archivo local con las claves que esa nube
necesita (mismo formato que `.env`):

```powershell
kubectl -n drivehub create secret generic drivehub-secretos --from-env-file=.env.gcp
```

Las credenciales compartidas (`PAGOS_CREDENCIAL_ACADEMICO`, `NOTIFICACIONES_CREDENCIAL_SERVICIOS`, el usuario y la
clave de RabbitMQ) llevan **el mismo valor** en los archivos de las nubes que las usan.

## 5. Cambios al proyecto antes de desplegar

| # | Qué | Por qué | Cambio |
| --- | --- | --- | --- |
| 1 | **Imagen de la web** | Las 7 apps no tienen Dockerfile de producción | `projects/frontend/Dockerfile`: compila las 7 apps con pnpm y las sirve con nginx sin root (`/` el shell, `/remotos/{app}/` cada remoto). Los manifiestos de federación se montan desde un ConfigMap |
| 2 | **IP real en el gateway** | El rate limit usa `getRemoteAddr()`, que detrás del ingress es la IP del ingress: todos los usuarios compartirían un cupo de 100 pedidos por minuto | `server.forward-headers-strategy=native` con los rangos del clúster como proxies confiables, y `externalTrafficPolicy: Local` en el ingress (Traefik) |
| 3 | **Salud sin depender de otra nube** | En Spring, `/health` incluye RabbitMQ. En GCP y Azure el broker está en AWS: si el túnel falla, Kubernetes sacaría de servicio a identidad, pagos, etc., aunque puedan seguir atendiendo | `management.endpoint.health.probes.enabled=true` y que la readiness solo mire la base propia del servicio, no RabbitMQ |
| 4 | **Contenido** | La imagen `prod` sirve con `php artisan serve` | Para empezar se acepta (una réplica). Recomendado: FrankenPHP. La migración como Job antes del Deployment |
| 5 | **Variables de producción** | Hoy apuntan a `localhost`, a los contenedores `drivehub-*` y a Mailpit | URLs internas sin prefijo (`http://pagos:8083`...); `app.cookies.secure=true`; `APP_WEB_URL=https://drivehub.<dominio>`; `PAGOS_URL_RETORNO=https://drivehub.<dominio>/api/v1/pagos/retorno`; `MINIO_URL`, `MINIO_ENDPOINT` y `MINIO_PUBLIC_URL` en `https://s3.us-east-1.amazonaws.com` (§4.1); `DYNAMODB_ENDPOINT` vacío y `AWS_REGION=us-east-1`; SMTP real; webhook de Stripe en `https://drivehub.<dominio>/api/v1/webhooks-stripe` |

Con **una réplica** por servicio no hace falta resolver todavía los procesos programados duplicados (los
`@Scheduled` de Académico, Pagos e Identidad) ni repartir el rate limit entre réplicas del gateway.

## 6. Preparar Tailscale y los clústeres

### 6.1 Tailscale (una vez)

1. Crear la cuenta (la *tailnet*) y activar **MagicDNS** (los nombres `*.ts.net`).
2. En la política de acceso, declarar las etiquetas `tag:k8s-operator` y `tag:k8s` (dueña la primera de la
   segunda) y la regla de §3.3: `tag:k8s` → `tag:k8s` solo en los puertos 5672 y 8081–8087.
3. Crear un **cliente OAuth** con permiso para crear dispositivos y claves, etiquetado `tag:k8s-operator`. Su id y
   su secreto se usan al instalar el operador en los tres clústeres; **nunca se commitean**.

**Plan**: el plan **Personal** (gratuito, sin tarjeta) incluye 50 dispositivos etiquetados y aquí se usan unos 25
(un proxy por servicio publicado, uno por enlace y un operador por clúster). Pero es **solo para uso no
comercial**, y Tailscale pone «clústeres de Kubernetes» y «clústeres de producción» como ejemplos de uso de
negocio. Sirve para el proyecto académico o una demo; si DriveHub pasa a atender escuelas reales, corresponde el
plan **Standard** (8 USD por usuario al mes; alcanza con un usuario).

### 6.2 Clústeres

Como Tailscale atraviesa la NAT por su cuenta, **no hace falta IP de salida fija ni rangos de red especiales**: se
crean con los valores por defecto.

| Nube | Clúster | Nodos |
| --- | --- | --- |
| Google Cloud | `gcloud container clusters create drivehub --zone us-east4-a --num-nodes 2 --machine-type e2-standard-2` | 2 × `e2-standard-2` (2 vCPU, 8 GB) |
| Azure | `az aks create -g drivehub -n drivehub --location eastus --node-count 2 --node-vm-size Standard_D2s_v5 --tier free --generate-ssh-keys` | 2 × `Standard_D2s_v5` |
| AWS | `eksctl create cluster --name drivehub --region us-east-1 --nodes 2 --node-type t3.large`, y después el complemento `aws-ebs-csi-driver` (sin él los discos quedan en `Pending`) | 2 × `t3.large` |

En cada clúster, una sola vez:

```powershell
helm install traefik traefik/traefik --version 42.2.0 -n traefik --create-namespace --set service.spec.externalTrafficPolicy=Local
helm install cert-manager jetstack/cert-manager -n cert-manager --create-namespace --set crds.enabled=true
helm install tailscale-operator tailscale/tailscale-operator -n tailscale --create-namespace --set-string oauth.clientId=<id> --set-string oauth.clientSecret=<secreto> --wait
```

En AKS y EKS el ingress público no hace falta: no publican nada (los archivos se sirven directo desde S3).

**DynamoDB** (en AWS): un usuario IAM `drivehub-dynamodb` con permiso solo sobre las 8 tablas, y su clave en el
secreto de EKS. Las tablas las crean los servicios al arrancar la primera vez (`DYNAMODB_CREAR_TABLAS=true`), en
modo bajo demanda. Más adelante se puede cambiar la clave por EKS Pod Identity.

**Imágenes**: se publican una vez en GitHub Container Registry (`ghcr.io/<usuario>/drivehub-<servicio>:<versión>`)
y cada clúster descarga solo las que corre.

## 7. Pasos

| Paso | Qué | Listo cuando |
| --- | --- | --- |
| 1 | Cambios de §5 y publicar las imágenes en GHCR | Las 9 imágenes arrancan con `docker run` y responden `/health` |
| 2 | Escribir `infrastructure/k8s/` y probar los componentes **en un solo clúster local** (Kubernetes de Docker Desktop, sin los enlaces: todos los servicios están en el mismo clúster y se encuentran por nombre) | Todo en `Running` y el login funciona en local |
| 3 | Tailscale (§6.1) y los tres clústeres con Traefik, cert-manager y el operador (§6.2) | Los tres operadores aparecen como dispositivos en la consola de Tailscale |
| 4 | DNS: registro `A` de `drivehub` hacia el ingress de GKE | `nslookup drivehub.<dominio>` devuelve la IP del ingress de GKE |
| 5 | Desplegar **AWS** primero (RabbitMQ y Notificaciones), después **GCP** (Identidad publica las claves del JWT) y por último **Azure**: `kubectl apply -k infrastructure/k8s/nubes/{nube}` | `kubectl -n drivehub get pods` en `Running` en las tres, y los servicios publicados aparecen en la consola de Tailscale |
| 6 | Comprobar la comunicación | Desde un pod de Azure, `curl http://identidad:8081/health` responde `UP`; desde internet, ningún nombre de servicio existe |
| 7 | Webhook de Stripe y prueba de humo | Registrarse y entrar (GCP), comprar horas con `4242 4242 4242 4242` (Azure), reservar una clase (Azure), recibir el aviso (AWS), ver un manual (AWS) y preguntar al asistente por las horas (AWS → Azure) |

**Actualizar una versión**: cambiar la etiqueta de la imagen en el componente y `kubectl apply -k
infrastructure/k8s/nubes/{nube}` en la nube donde corre ese servicio. Automatizarlo con GitHub Actions (un job por
nube) es el siguiente paso natural.

## 8. Lo que este enfoque deja para después

| Limitación | Consecuencia | Mejora posible |
| --- | --- | --- |
| Conexión por relé | Si los dos extremos están detrás de una NAT «difícil» no hay túnel directo. Es lo esperable entre AKS y EKS: la NAT de AWS y la de Azure asignan un puerto distinto por destino, y los pods además salen con SNAT del nodo. El tráfico pasa por los relés DERP de Tailscale: sigue cifrado, pero con más latencia y con un límite de ancho de banda (Tailscale no publica cifras). Para las llamadas JSON de DriveHub, con poco tráfico, alcanza | Medir después del paso 5 con `tailscale ping` desde un proxy (`via <ip>:<puerto>` es directa; `via DERP(...)` es relé). Si molesta: un **peer relay** propio dentro de GKE (el operador lo despliega con un recurso desde la v1.102; un puerto UDP abierto en el firewall; el plan gratuito incluye dos) o *static endpoints* con un `ProxyGroup` y `NodePort` |
| Depende de la coordinación de Tailscale | Si su servidor de coordinación cae, los túneles ya abiertos siguen andando, pero no se pueden agregar ni cambiar dispositivos ni reglas. En Kubernetes lo delicado es que **un pod proxy que se reinicia durante el corte pierde su mapa de red y queda fuera** hasta que vuelva la coordinación | No desplegar ni cambiar enlaces mientras [status.tailscale.com](https://status.tailscale.com) muestre un incidente. Activar el atributo `cache-network-maps` en la política (desde la v1.104 el cliente guarda su mapa de red y arranca sin la coordinación; en prueba y opcional; antes de confiar en él, verificar que las imágenes de los proxies del operador lo incluyen). Para eliminar la dependencia: VPN de cada nube (habría que recrear clústeres con rangos de red sin solapar) |
| Una réplica por servicio | Al actualizar un servicio se corta unos segundos | Más réplicas, después de agregar ShedLock a los procesos programados y un rate limit compartido (Redis) al gateway |
| Bases en contenedores | Los respaldos son responsabilidad propia | Un `CronJob` con `pg_dump`/`mongodump` a un bucket, o las bases administradas de cada nube |
| GCP es la única entrada | Si GCP cae, no se puede entrar al sistema | Un segundo gateway en otra nube con DNS de conmutación |
| Sin monitoreo | Solo `kubectl logs` y la consola de Tailscale | `kube-prometheus-stack` en cada clúster y un Grafana que lea los tres (por la misma tailnet) |

## 9. Costos

- **Nodos**: 2 nodos de 2 vCPU y 8 GB por nube. Para una demo, bajar los nodos a 0 cuando no se usan
  (`gcloud container clusters resize`, `az aks scale`, `eksctl scale nodegroup`); los discos se conservan.
- **Plano de control**: GKE da un crédito mensual que cubre un clúster zonal; AKS en nivel `free` no lo cobra;
  **EKS lo cobra siempre**, aunque los nodos estén en 0.
- **Load balancers públicos**: uno en GKE.
- **S3**: almacenamiento y descargas del bucket `amz-drivehub` (los modelos de IA, ~370 MB por teléfono, son lo que
  más pesa).
- **Tailscale**: gratis en este tamaño.
- **Tráfico entre nubes**: la salida de datos hacia otra nube se cobra por GB. Con poco uso es un monto chico, y
  las llamadas más frecuentes ya quedaron dentro de una misma nube.
- DynamoDB bajo demanda: casi nulo con poco tráfico.
- Antes de crear nada, configurar una alerta de presupuesto en cada cuenta.
