# Unidades de trabajo — CU06-BE

**Caso de uso:** Gestionar el perfil de la cuenta propia: ver, editar datos personales, subir o quitar la foto (MinIO), cambiar la contraseña con sesión y cambiar el correo con un enlace al correo nuevo.
**Microservicio(s):** `identidad` (UT 01 y 03 a 14) e infraestructura (UT 02: `nginx`, `docker-compose.yml`, `.env.example`). El `gateway` no cambia — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU06 - Gestionar perfil`](../../../CasosDeUso/CU06%20-%20Gestionar%20perfil.md)
**Recurso HTTP / contrato:** `GET` y `PATCH /api/v1/usuarios/me`, `PUT` y `DELETE /api/v1/usuarios/me/foto`, `POST /api/v1/usuarios/me/cambiar-contrasena`, `POST /api/v1/usuarios/me/cambiar-correo` (todos con sesión) y `POST /api/v1/auth/cambiar-correo/confirmar` (público)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [ENDPOINTS_BACKEND.md](../../../../.claude/rules/backend/ENDPOINTS_BACKEND.md), [RESPONSES_BACKEND.md](../../../../.claude/rules/backend/RESPONSES_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) e [INFRAESTRUCTURA.md](../../../INFRAESTRUCTURA.md) (MinIO y Mailpit). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Recurso `usuarios/me`:** la persona se identifica por `@CurrentUserId` (el `sub` del JWT), nunca por un código en la URL ni en el cuerpo. `usuarios` ya está en el gateway: no hay recurso nuevo ni cambia `MICROSERVICIOS.md`.
2. **Foto en MinIO, bucket `drivehub-identidad` (privado):** la columna `usuario.foto_url` guarda la **clave** del objeto (`usuarios/{usuarioCodigo}/{uuid v7}.{ext}`), no una URL. Cada subida usa una clave nueva (así el navegador no muestra una foto cacheada) y se borra la anterior.
3. **La foto se lee con URL prefirmada** (como define `INFRAESTRUCTURA.md`): el backend la genera al responder y vale 60 minutos (`app.perfil.foto.url-ttl-minutos`). La sesión se renueva cada ~12 minutos y devuelve una URL fresca, así que el avatar nunca queda con una URL vencida.
4. **La URL prefirmada usa otro host que el interno:** la firma incluye el host, así que se firma con `MINIO_PUBLIC_URL` (el que alcanza el navegador o el teléfono), distinto de `MINIO_URL` (`drivehub-minio:9000`, solo dentro de Docker). En desarrollo web basta `http://localhost:9000`; para el emulador o un teléfono real hay que poner la IP de la PC en la red (`http://192.168.x.x:9000`).
5. **SDK de S3 de AWS v2** (`software.amazon.awssdk:s3`, con el BOM `software.amazon.awssdk:bom` y cliente HTTP `url-connection-client`): firma sin consultar a MinIO y soporta un endpoint de firma distinto del de escritura. Un cliente para subir/borrar (endpoint interno) y un `S3Presigner` (endpoint público), ambos con `forcePathStyle` y región fija `us-east-1`.
6. **Revisión básica de la foto, sin antivirus** (ver el CU): tamaño ≤ 5 MB, formato real por la firma del contenido (nunca por la extensión ni el `Content-Type`), imagen completa legible (decodificada de punta a punta), ≤ 4000×4000 px y **sin bytes después del final de la imagen** (rechaza un ejecutable pegado a una imagen). WebP se decodifica con el plugin `com.twelvemonkeys.imageio:imageio-webp` (el JDK no lo trae). Vive tras `ValidadorImagenPort`.
7. **Orden al subir:** validar → guardar el objeto nuevo en MinIO → actualizar la cuenta en la base → borrar el objeto anterior. Si la base falla, se borra el nuevo (compensación). Si borrar el anterior falla, queda un `WARN` y el huérfano no afecta a nadie.
8. **Cambiar contraseña conserva la sesión actual por dispositivo:** hay una sesión activa por cuenta y dispositivo (`WEB`, `MOVIL`) y el JWT no dice cuál es. El cuerpo lleva `dispositivo` (como `login`) y se revoca la sesión del **otro** dispositivo con `RefreshTokenRepositoryPort.revocarActivos` (ya existe). No se toca el JWT.
9. **Un enlace de cambio de correo = una fila de `token_recuperacion`** con `tipo = CAMBIO_CORREO` y la columna nueva `correo_nuevo`. El correo de la cuenta no cambia hasta confirmar. Un token de un tipo no sirve para los otros (mismo criterio que CU04b).
10. **La contraseña actual incorrecta es 400 (`ERR_IDE_14`), no 401:** un 401 haría que los clientes intenten renovar la sesión. No suma `intentos_fallidos` ni bloquea.
11. **Si Escuela no responde, el perfil igual se muestra:** `escuelaNombre` viaja `null` (se consulta con el `EscuelaConsultaPort` que ya existe) y se registra un `WARN`.
12. **`fotoUrl` también viaja en login y renovación,** igual que CU04b hizo con `correoVerificado`, para que la barra superior y la app muestren el avatar sin una llamada extra.
13. **Aviso al correo anterior:** si su envío falla, el cambio ya confirmado no se revierte; solo se registra.

## Contrato de datos / recurso HTTP

| Endpoint | Auth | Cuerpo | Respuesta |
|----------|------|--------|-----------|
| `GET /api/v1/usuarios/me` | JWT | — | `200` `data`: perfil (abajo); `401 ERR_SYS_04` |
| `PATCH /api/v1/usuarios/me` | JWT | `{ nombres?, apellidos?, telefono?, fechaNacimiento? }` | `200` `data`: perfil; `400 ERR_SYS_01`; `400 ERR_IDE_02` menor de 18 |
| `PUT /api/v1/usuarios/me/foto` | JWT | `multipart/form-data`, parte `archivo` | `200` `data`: perfil; `400 ERR_IDE_16` archivo inválido; `400 ERR_IDE_17` demasiado grande; `400 ERR_SYS_01` sin archivo |
| `DELETE /api/v1/usuarios/me/foto` | JWT | — | `200` `data`: perfil (sin foto); idempotente |
| `POST /api/v1/usuarios/me/cambiar-contrasena` | JWT | `{ passwordActual, passwordNueva, dispositivo }` | `200` `data: null`; `400 ERR_IDE_14` actual incorrecta; `400 ERR_IDE_15` igual a la actual; `400 ERR_SYS_01` |
| `POST /api/v1/usuarios/me/cambiar-correo` | JWT | `{ passwordActual, correoNuevo }` | `200` `data: null`; `400 ERR_IDE_14`; `409 ERR_IDE_01` en uso; `409 ERR_IDE_18` igual al actual; `400 ERR_SYS_01`; `500` si falla el correo |
| `POST /api/v1/auth/cambiar-correo/confirmar` | Público | `{ token }` | `200` `data: null`; `400 ERR_IDE_11` enlace inválido, vencido, usado, de otro tipo o con correo ya tomado; `400 ERR_SYS_01` |

`PATCH` modifica solo los campos presentes; un `telefono` vacío o `null` explícito lo borra. Reglas de campos (iguales al registro, [CU01](../../../CasosDeUso/CU01%20-%20Registrar%20estudiante.md)): nombres y apellidos no vacíos y ≤ 80; teléfono ≤ 20; fecha de nacimiento en el pasado y edad ≥ 18 (`app.registro.edad-minima`); contraseña nueva de 9 a 64 caracteres; correo válido y ≤ 120.

**Perfil** (`data` de `GET`, `PATCH`, `PUT` y `DELETE` de la foto): `codigo`, `nombres`, `apellidos`, `fechaNacimiento`, `telefono`, `email`, `correoVerificado`, `roles` (lista de nombres), `escuelaCodigo`, `escuelaNombre` (ambos `null` en el Super admin), `fotoUrl` (URL prefirmada o `null`).

También se suma `usuario.fotoUrl` a la respuesta de `login` y `refresh`.

### Códigos de error nuevos

| Código | HTTP | Excepción |
|--------|------|-----------|
| `ERR_IDE_14` | 400 | `ValidationException` |
| `ERR_IDE_15` | 400 | `ValidationException` |
| `ERR_IDE_16` | 400 | `ValidationException` |
| `ERR_IDE_17` | 400 | `ValidationException` |
| `ERR_IDE_18` | 409 | `ConflictException` |

Se reutilizan `ERR_IDE_01` (correo en uso), `ERR_IDE_02` (menor de edad), `ERR_IDE_11` (enlace inválido), `ERR_SYS_01` y `ERR_SYS_04`.

## Orden de ejecución

| Código | Nombre | Servicio | Depende de |
|--------|--------|----------|------------|
| [UT-CU06-BE-01](./UT-CU06-BE-01.md) | Migración y dominio del cambio de correo | `identidad` | — |
| [UT-CU06-BE-02](./UT-CU06-BE-02.md) | Infraestructura: URL pública de MinIO y límite de subida | infraestructura | — |
| [UT-CU06-BE-03](./UT-CU06-BE-03.md) | `AlmacenFotosPort` y adaptador MinIO | `identidad` | 02 |
| [UT-CU06-BE-04](./UT-CU06-BE-04.md) | `ValidadorImagenPort` y revisión de la foto | `identidad` | — |
| [UT-CU06-BE-05](./UT-CU06-BE-05.md) | Correos del cambio de correo | `identidad` | — |
| [UT-CU06-BE-06](./UT-CU06-BE-06.md) | Ver y editar el perfil | `identidad` | 03 |
| [UT-CU06-BE-07](./UT-CU06-BE-07.md) | Subir y quitar la foto | `identidad` | 03, 04, 06 |
| [UT-CU06-BE-08](./UT-CU06-BE-08.md) | Cambiar la contraseña | `identidad` | — |
| [UT-CU06-BE-09](./UT-CU06-BE-09.md) | Pedir el cambio de correo | `identidad` | 01, 05, 08 |
| [UT-CU06-BE-10](./UT-CU06-BE-10.md) | Confirmar el cambio de correo | `identidad` | 01, 05 |
| [UT-CU06-BE-11](./UT-CU06-BE-11.md) | `fotoUrl` en login y renovación | `identidad` | 03 |
| [UT-CU06-BE-12](./UT-CU06-BE-12.md) | Endpoints de perfil, foto y contraseña | `identidad` | 06, 07, 08 |
| [UT-CU06-BE-13](./UT-CU06-BE-13.md) | Endpoints del cambio de correo | `identidad` | 09, 10 |
| [UT-CU06-BE-14](./UT-CU06-BE-14.md) | Prueba de integración | `identidad` | 11, 12, 13 |

**Cadena:** `02 → 03 → 06 → 07 → 12`; `04 → 07`; `08 → 09, 12`; `01, 05 → 09, 10 → 13`; `03 → 11`; `11, 12, 13 → 14`. La 08 define `ERR_IDE_14`, que la 09 reutiliza.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 (datos), CA02, CA03 | Ver y editar datos personales; datos inválidos → 400 | 06, 12, 14 |
| CA04 a CA07 | Foto válida, formato/contenido, tamaño y dimensiones, archivo ilegible | 03, 04, 07, 12, 14 |
| CA09 | Quitar la foto, idempotente | 07, 12, 14 |
| CA10, CA11, CA12 | Cambiar la contraseña; actual incorrecta; reglas e igual a la actual | 08, 12, 14 |
| CA13, CA14, CA17 | Pedir el cambio de correo; en uso o igual; el correo falla | 05, 09, 13, 14 |
| CA15, CA16 | Confirmar el enlace; inválido, reemplazado o ajeno | 01, 10, 13, 14 |
| CA19 | Ningún secreto en los registros | 06 a 10, 14 |
| CA20 | Rate limit | ya cubierto por el gateway; sin UT nueva |
| CA01 (menú), CA18 | Entrada del menú y correo nuevo visible | las UT de frontend y mobile (con 11 para el avatar) |

## Follow-ups (no se implementan en estas UT)

- Job que borra los enlaces de correo vencidos y los objetos huérfanos del bucket.
- Enviar los correos por Notificaciones (CU36) en lugar de SMTP directo.
- Miniaturas de la foto (hoy se sirve la original, ≤ 5 MB).
- Servir la foto de otras personas (ej. la del instructor para sus estudiantes): hoy solo se lee la propia.

## Qué NO incluyen estas UT

- Recuperar la contraseña y verificar el correo ([CU04](../../CU04%20-%20Recuperar%20contrase%C3%B1a/UT-BACKEND/README.md), [CU04b](../../CU04b%20-%20Verificar%20correo/UT-BACKEND/README.md)).
- Los perfiles de estudiante e instructor de Escuela (CU08 y CU09).
- Administrar las cuentas de otras personas.
- Cambios en el `gateway` (la ruta `usuarios` y `auth` ya existen).

## Commits sugeridos

Un commit `[BACK]` para `identidad` (UT 01 y 03 a 14); `docker-compose.yml`, `.env.example` y `nginx.conf` (UT 02) son `[INFRA]`; `docs/`, `docs/errors/` y `DISENO_BASE_DE_DATOS.md` son `[DOCS]`.
