# Unidades de trabajo — CU04b-MOB

**Caso de uso:** Verificar el correo desde la app móvil: botón en la barra superior y refresco de la sesión al volver.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU04b - Verificar correo`](../../../CasosDeUso/CU04b%20-%20Verificar%20correo.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/verificar-correo` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El enlace se abre en el navegador del teléfono,** en la página pública de la web (`/account/verify-email`); la app no necesita deep links ni pantalla de confirmación.
2. **La app se entera al volver:** al pasar a `resumed` con el correo sin verificar, renueva la sesión y el backend devuelve `correoVerificado = true`.
3. **Sesiones ya guardadas:** el campo nuevo se lee con valor por defecto `false`, así que no hace falta cerrar la sesión de quienes ya la tenían.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU04b-MOB-01](./UT-CU04b-MOB-01.md) | Sesión con `correoVerificado` y pedir la verificación | — |
| [UT-CU04b-MOB-02](./UT-CU04b-MOB-02.md) | Botón "Verificar correo" en el `UsuarioShell` | 01 |

**Cadena:** `01 → 02`. Necesita el backend de CU04b solo para la validación manual.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | Botón visible solo sin verificar | 01, 02 |
| CA02 | Pedir el correo e informarlo | 01, 02 |
| CA07 | El botón se oculta al volver tras verificar | 02 |

## Follow-ups (no se implementan en estas UT)

- Abrir el enlace directamente en la app (deep links / App Links).
- Menú de perfil con el correo y su estado.

## Qué NO incluyen estas UT

- Recuperar la contraseña (CU04).
- Cambios en el login ni en el router.

## Commits sugeridos

Un commit `[MOBILE]` para todo CU04b-MOB.
