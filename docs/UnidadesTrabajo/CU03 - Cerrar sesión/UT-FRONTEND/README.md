# Unidades de trabajo — CU03-FE

**Caso de uso:** Cerrar sesión en la web: el shell avisa al backend, olvida la sesión local y lleva al inicio de sesión.
**Micro-frontend(s):** `shell` (UT 01, 02) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU03 - Cerrar sesión`](../../../CasosDeUso/CU03%20-%20Cerrar%20sesi%C3%B3n.md)
**Recurso HTTP / contrato:** botón "Cerrar sesión" en el layout; consume `POST /api/v1/auth/logout` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md) (sesión compartida), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Solo el shell.** Cerrar sesión es una acción del layout del shell; los remotos no la reciben en `@drivehub/mf-contract` (no la necesitan, el shell ya tiene el botón).
2. **El borrado local no depende del backend:** `cerrarSesion()` llama al backend, ignora cualquier error (red, 429, 5xx) y siempre ejecuta `limpiar()` y navega a `/login`.
3. **Sin `redirect` ni `motivo`:** cerrar sesión es voluntario, así que el login se muestra limpio (no se vuelve a la pantalla actual tras ingresar de nuevo).
4. **Otras pestañas:** conservan su acceso en memoria hasta que venza; la siguiente renovación falla (401) y las lleva al login con el flujo de CU02. No se sincronizan entre pestañas.

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU03-FE-01](./UT-CU03-FE-01.md) | `AuthApiService.cerrar` y `SesionService.cerrarSesion` | `shell` | — |
| [UT-CU03-FE-02](./UT-CU03-FE-02.md) | Botón "Cerrar sesión" en el layout | `shell` | 01 |

**Cadena:** `01 → 02`. Necesita el backend de CU03 solo para la validación manual de punta a punta.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA02 | Acceso borrado de la memoria y llevada al inicio de sesión | 01, 02 |
| CA06, A2, A3, CA10 | Si el backend falla, igual se borra la sesión local | 01 |
| CA08 | No se recupera al recargar (la cookie ya no sirve) | 01 |

## Follow-ups (no se implementan en estas UT)

- Sincronizar el cierre entre pestañas.
- Mostrar el nombre de la persona junto al botón (menú de usuario).

## Qué NO incluyen estas UT

- Cambios en los remotos ni en `@drivehub/mf-contract`.
- Cambios en la renovación ni en los guards de CU02.

## Commits sugeridos

Un commit `[FRONT]: shell -- ...`.
