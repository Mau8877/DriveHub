# Unidades de trabajo — CU03-MOB

**Caso de uso:** Cerrar sesión en la app móvil: avisar al backend, borrar la sesión del almacenamiento seguro y volver al login.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU03 - Cerrar sesión`](../../../CasosDeUso/CU03%20-%20Cerrar%20sesi%C3%B3n.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/logout` con `dispositivo: 'MOVIL'` y el `refreshToken` en el cuerpo (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **`sesionProvider` es el único dueño de la sesión:** `cerrarSesion()` vive ahí; la pantalla solo lo llama.
2. **El borrado local no depende del backend:** se intenta `POST /auth/logout` y se ignora cualquier error; siempre se cancela la renovación, se borra el almacenamiento seguro y el estado pasa a `sinSesion` (sin `motivo`).
3. **Sin navegación manual:** el redirect global del router (CU02) lleva a `/login` al cambiar el estado.
4. **Botón en el `AppBar` del `UsuarioShell`,** visible en todas las secciones. Sin confirmación previa.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU03-MOB-01](./UT-CU03-MOB-01.md) | `AuthService.cerrar` y `SesionNotifier.cerrarSesion` | — |
| [UT-CU03-MOB-02](./UT-CU03-MOB-02.md) | Botón "Cerrar sesión" en el `UsuarioShell` | 01 |

**Cadena:** `01 → 02`. Necesita el backend de CU03 solo para la validación manual de punta a punta.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA03 | Almacenamiento seguro borrado y llevada al login | 01, 02 |
| CA06, A2, A3, CA10 | Si el backend falla, igual se borra la sesión local | 01 |
| CA08 | No se recupera al reabrir la app | 01 |

## Follow-ups (no se implementan en estas UT)

- Confirmación previa y menú de perfil con el nombre de la persona.

## Qué NO incluyen estas UT

- Cambios en el login, la renovación ni el router.
- `AdminShell`: la app móvil no es para administradores.

## Commits sugeridos

Un commit `[MOBILE]` para todo CU03-MOB.
