# Unidades de trabajo — CU48-BE

**Caso de uso:** Registrarse por WhatsApp: el chat reúne los datos del estudiante, Identidad guarda una solicitud y la cuenta se crea cuando la persona fija su contraseña en una página web.
**Microservicio(s) / micro-frontend(s):** `identidad`, `gateway` y `n8n` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU48 - Registrarse por WhatsApp.md`](../../../CasosDeUso/CU48%20-%20Registrarse%20por%20WhatsApp.md)
**Recurso HTTP / contrato:** rutas internas `/internal/v1/whatsapp/registros*` (n8n) y la pública `POST /api/v1/registros-whatsapp/completar` (web, por el gateway)

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [EXCEPCIONES_BACKEND.md](../../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md), [GUIA_ERRORES_BACKEND.md](../../../../.claude/rules/backend/GUIA_ERRORES_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md) y, para el gateway, [GATEWAY_BACKEND.md](../../../../.claude/rules/backend/GATEWAY_BACKEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Prerrequisito: CU47 implementado** (credencial de servicio de `/internal/**`, hash del teléfono, vínculo y servicio n8n). Estas UT lo reutilizan; no lo reconstruyen.
- **La contraseña no pasa por WhatsApp ni por n8n.** Llega solo a `POST /api/v1/registros-whatsapp/completar` desde la página web, por el gateway.
- **Una sola regla de creación de cuenta.** Completar la solicitud llama al caso de uso existente `RegistrarEstudianteUseCase` (CU01 + CU08a); no se copia su lógica. Las reglas de validación de cada dato (edad, CI, sexo, correo, coordenadas) se extraen a una clase de dominio compartida por el registro normal y este, para que no se desalineen.
- **Validar dato por dato.** Una ruta interna valida un campo a la vez, así el bot detecta el error apenas llega y no al final.
- **La solicitud guarda el teléfono en claro** (el perfil lo necesita al crear la cuenta) **y los datos personales solo hasta completarse o vencer;** un proceso programado borra las vencidas. El enlace se guarda hasheado.
- **El número se vincula solo si está libre.** Si ya pertenece a otra cuenta, la cuenta igual se crea y la respuesta indica `vinculado: false`; el bot manda a iniciar sesión por [CU47](../../CU47%20-%20Iniciar%20sesión%20por%20WhatsApp/UT-BACKEND/README.md).
- **Ruta nueva pública:** `registros-whatsapp` se registra en el gateway y en `MICROSERVICIOS.md` en el mismo cambio, y queda en la lista de públicas de `SecurityConfig` del gateway.
- **Idioma de la ruta web:** pública de `identidad`, en inglés (`/account/set-password`).
- Los códigos de error nuevos toman los siguientes números libres de `ERR_IDE_NN` al implementar.

## Contrato de datos / recurso HTTP

| Método | Ruta | Cuerpo | Respuesta |
|--------|------|--------|-----------|
| POST | `/internal/v1/whatsapp/registros/validaciones` | `{ campo, valor }` | 200 si es válido; 400 `ERR_SYS_01` con el motivo; 409 si el correo o el CI ya existen |
| POST | `/internal/v1/whatsapp/registros` | `{ telefono, nombres, apellidos, fechaNacimiento, sexo, ci, correo, latitud, longitud }` | 201, `data: { enlace, expiraEn }` |
| POST | `/api/v1/registros-whatsapp/completar` (pública) | `{ token, password }` | 201, `data: { vinculado }`; 400 si el enlace no sirve o la contraseña es corta; 409 si el correo o el CI se ocuparon |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU48-BE-01](./UT-CU48-BE-01.md) | Persistencia de la solicitud de registro | — |
| [UT-CU48-BE-02](./UT-CU48-BE-02.md) | Validar un dato del registro | — |
| [UT-CU48-BE-03](./UT-CU48-BE-03.md) | Crear la solicitud y su enlace | 01, 02 |
| [UT-CU48-BE-04](./UT-CU48-BE-04.md) | Completar el registro con la contraseña | 01, 03 |
| [UT-CU48-BE-05](./UT-CU48-BE-05.md) | Gateway: ruta pública de registros-whatsapp | 04 |
| [UT-CU48-BE-06](./UT-CU48-BE-06.md) | Flujo de n8n para registrarse | 02, 03, 04 |

**Cadena:** `01, 02 → 03 → 04 → 05, 06`. La página web es [UT-CU48-FE-01](../UT-FRONTEND/UT-CU48-FE-01.md).

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 2 y 3, A1, A2, A3 | Pedir y validar cada dato | 02, 06 |
| Flujo principal 4 a 6 | Resumen, solicitud y enlace | 03, 06 |
| Flujo principal 7 a 9, A5, A6, A7, A8 | Contraseña, creación de la cuenta y vínculo | 04, FE-01 |
| A4 | Descartar lo que no se completa | 01 |
| CA09, A9 | Ruta pública en el gateway y límite | 05 |

## Follow-ups (no se implementan en estas UT)

- Reenviar el enlace desde el chat (está solo como «generar otro» desde el bot).

## Qué NO incluyen estas UT

- El login por código ([CU47](../../CU47%20-%20Iniciar%20sesión%20por%20WhatsApp/UT-BACKEND/README.md)), la verificación del correo (CU04b) y el registro normal (CU01, sin cambios de comportamiento).
