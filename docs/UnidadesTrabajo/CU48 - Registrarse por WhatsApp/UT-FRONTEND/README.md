# Unidades de trabajo — CU48-FE

**Caso de uso:** Registrarse por WhatsApp: la página pública donde la persona elige su contraseña.
**Microservicio(s) / micro-frontend(s):** `identidad` (web, Angular remoto) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU48 - Registrarse por WhatsApp.md`](../../../CasosDeUso/CU48%20-%20Registrarse%20por%20WhatsApp.md)
**Recurso HTTP / contrato:** pantalla `/account/set-password?token=...` que llama a `POST /api/v1/registros-whatsapp/completar`

## Antes de implementar cualquier UT

Leer [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) y [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md). Estas fichas **no** sustituyen esas reglas.

## Decisiones de diseño

- **Mismo patrón que las páginas públicas existentes de `identidad`** (`/account/verify-email`, `/account/reset-password`): ruta en inglés, `<dh-auth-layout>`, sin sesión, y declarada en las rutas públicas del remoto.
- **Una pantalla, tres estados:** formulario de contraseña, éxito («tu cuenta está lista, vuelve a WhatsApp») y enlace inválido o vencido («pídele otro al chat»).
- **Prerrequisito:** la ruta del gateway de [UT-CU48-BE-05](../UT-BACKEND/UT-CU48-BE-05.md) ya existe.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU48-FE-01](./UT-CU48-FE-01.md) | Página para fijar la contraseña | — |

**Cadena:** `01`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 7 a 9, A5, A6, A7 | Elegir la contraseña y ver el resultado | 01 |

## Qué NO incluyen estas UT

- El chat, la creación de la solicitud y la cuenta (backend), y cambios en el registro normal.
