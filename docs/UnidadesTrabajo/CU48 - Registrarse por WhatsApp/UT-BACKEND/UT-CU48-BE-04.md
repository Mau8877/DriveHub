# UT-CU48-BE-04 — Completar el registro con la contraseña

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU48-BE-04` |
| **Nombre** | Completar el registro con la contraseña |
| **Caso de uso** | CU48-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU48-BE-01, UT-CU48-BE-03 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md) (la contraseña nunca se loguea), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md) y `RegistrarEstudianteService`.

## Solicitud de negocio

Al elegir su contraseña en la web, la persona queda con cuenta y con su WhatsApp reconocido.

## Objetivo

`POST /api/v1/registros-whatsapp/completar` crea la cuenta desde la solicitud y vincula el número si está libre.

## Alcance

- Puerto `CompletarRegistroWhatsappUseCase` y servicio (`@Transactional`, loguea):
  1. Busca la solicitud por el hash del token; si no existe, venció o está usada → `ValidationException` (400) con un código nuevo (`ERR_IDE_NN`, «El enlace no es válido o venció»).
  2. Valida la contraseña (mínimo 9 caracteres, regla de CU01).
  3. Crea la cuenta llamando a `RegistrarEstudianteUseCase` con los datos de la solicitud, la contraseña, el teléfono y la escuela del chat; si el correo o el CI se ocuparon entretanto, deja pasar el 409 normal (`ERR_IDE_19` para el CI).
  4. Marca la solicitud como usada y borra sus datos personales.
  5. Si el número no tiene vínculo, lo vincula a la cuenta nueva; si ya tiene otro, no lo toca.
- Controller público: `POST /api/v1/registros-whatsapp/completar` con `CompletarRegistroRequest` (`token`, `password`); `201` con `data: { vinculado }` (booleano). Sin sesión, sin devolver la contraseña ni el hash.
- `SecurityConfig` de Identidad: la ruta queda pública; el rate limit lo pone el gateway.
- Código de error nuevo: constante, sección en `docs/errors/identidad/ERRORES_IDENTIDAD.md` y link en `docs/errors/README.md`.
- Logs: INFO con el `codigo_usuario` creado; nunca token, contraseña, número ni correo.
- Tests: crea la cuenta y vincula, número ya vinculado a otra cuenta (`vinculado: false`), enlace inválido/vencido/usado, contraseña corta, correo o CI ocupados, y que el token no sirve dos veces.

Archivos esperados (crear/tocar): puerto y servicio nuevos, `CompletarRegistroRequest.java`, controller, `SecurityConfig.java`, `IdentidadErrorCodes.java`, `docs/errors/**` y los tests.

## Fuera de alcance (no tocar)

- El gateway (UT-05), la página web (FE-01) y cualquier cambio en el registro normal de CU01.

## Resultado esperado

Con un enlace válido y una contraseña aceptable existe la cuenta `ACTIVO` con rol `ESTUDIANTE`, con el número vinculado si estaba libre.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`.
2. Crear una solicitud (UT-03) y completarla: 201; iniciar sesión con ese correo y contraseña por `/api/v1/auth/login` funciona; el mismo enlace otra vez → 400.
