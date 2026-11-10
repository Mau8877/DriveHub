# Unidades de trabajo — CU53-FE

**Caso de uso:** La campana de la cabecera del panel con el contador de avisos sin leer y la pantalla «Avisos».
**Micro-frontend(s):** `shell` (Angular) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU53 - Consultar avisos de la escuela`](../../../CasosDeUso/CU53%20-%20Consultar%20avisos%20de%20la%20escuela.md)
**Recurso HTTP / contrato:** `POST /api/v1/graphql` (`notificaciones`, `conteoNoLeidas`, `marcarLeida`, `marcarTodasLeidas`), ver [UT-BACKEND](../UT-BACKEND/README.md). Pantalla `/avisos`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [UI_FRONTEND.md](../../../../.claude/rules/frontend/UI_FRONTEND.md) y [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Solo el shell**: la campana está en su cabecera y el shell ya consulta Notificaciones por GraphQL (push web, CU36).
2. **La campana lleva a una pantalla** (`/avisos`), no a un desplegable: `<dh-header>` solo muestra la campana y su número, y emite `dh-avisos`. Una pantalla usa las clases base (`dh-page`, `dh-card`, `dh-acordeon`) sin crear un componente nuevo en `packages/ui`.
3. **`AvisosService` en `core/notificaciones/`**: lo usan el layout (contador) y la pantalla; la pantalla vive en `features/avisos/`.
4. **El contador se refresca al navegar** y después de marcar; sin actualización en vivo.
5. **El Super admin no ve la campana** (no tiene bandeja).

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU53-FE-01](./UT-CU53-FE-01.md) | Campana con contador y pantalla de avisos | UT-CU53-BE-01 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 a 7, marcar todos | Contador, lista, filtro, más páginas, abrir y marcar | 01 |
| A1, A2, A4, A5 | Vacío, reintentar, aviso inexistente, fallo al marcar | 01 |
| CA08 | Sin campana para el Super admin | 01 |

## Follow-ups (no se implementan en estas UT)

- Ir desde un aviso a la pantalla relacionada (el pago, la clase).

## Qué NO incluyen estas UT

- Cambios en `packages/ui` o en los remotos.
- El backend.
