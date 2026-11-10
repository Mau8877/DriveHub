# Kubernetes

Manifiestos (Kustomize) para desplegar DriveHub en Kubernetes. El plan completo, con el reparto de servicios en
Google Cloud, Azure y AWS, está en [docs/DESPLIEGUE_KUBERNETES.md](../../docs/DESPLIEGUE_KUBERNETES.md).
`docker-compose.yml` sigue siendo el entorno de desarrollo del día a día.

```
k8s/
├── componentes/   # Una carpeta por pieza (servicio, web o base), igual en cualquier clúster
└── local/         # Todo en UN clúster: el Kubernetes de Docker Desktop, para probar antes de ir a la nube
```

Las variables que cambian entre entornos (URLs públicas, región, SMTP...) vienen de la ConfigMap
`drivehub-entorno`, y las credenciales del Secret `drivehub-secretos`: cada entorno trae las suyas. Las URLs entre
servicios (`http://pagos:8083`) son iguales en todos lados; en la nube, un servicio de otra nube se alcanza con ese
mismo nombre por Tailscale.

## Probar todo en local (Docker Desktop)

Requisitos: Kubernetes activado en Docker Desktop, unos 6 GB libres para Docker y Helm. Detener antes el stack de
`docker compose` (usa los mismos puertos y la misma memoria).

```powershell
# 1. Las 9 imágenes de producción (etiqueta `local`). Con un nombre, solo esa: ... construir-imagenes.ps1 pagos
.\infrastructure\k8s\local\construir-imagenes.ps1

# 2. La entrada (una sola vez por clúster)
helm repo add traefik https://traefik.github.io/charts
helm install traefik traefik/traefik --version 42.2.0 -n traefik --create-namespace --set service.spec.externalTrafficPolicy=Local

# 3. DriveHub
kubectl apply -k infrastructure/k8s/local
kubectl -n drivehub get pods -w
```

| URL | Qué |
| --- | --- |
| http://localhost | La web (shell y remotos) |
| http://localhost/api/v1/... | La API, por el gateway |
| http://archivos.localhost | MinIO (fotos de perfil, manuales) |
| http://correo.localhost | Mailpit: los correos que envían los servicios |

El clúster local corre los servicios Spring con el perfil `dev`, así que tiene los mismos datos de ejemplo que
`docker compose` (Super Admin, escuela, instructores, planes...). En la nube nunca se activa ese perfil.

Para borrar todo, incluidos los datos: `kubectl delete namespace drivehub`.

> El Kubernetes de Docker Desktop en modo *kind* no ve las imágenes de Docker: `construir-imagenes.ps1` las importa
> al nodo (`desktop-control-plane`) después de construirlas. Después de reconstruir una imagen, reiniciar su pod:
> `kubectl -n drivehub rollout restart deployment/pagos`.
