# Unidades de trabajo — CU53-BE

**Caso de uso:** La bandeja de avisos de la escuela: Notificaciones la guarda y la sirve al Administrador; Pagos y Académico la alimentan.
**Microservicio(s):** `notificaciones`, `pagos`, `academico` — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU53 - Consultar avisos de la escuela`](../../../CasosDeUso/CU53%20-%20Consultar%20avisos%20de%20la%20escuela.md)
**Recurso HTTP / contrato:** los de CU38 (`/api/v1/notificaciones`, `POST /api/v1/graphql`), ahora también para `ADMIN_ESCUELA`; y `escuelaCodigo` en el pedido interno `POST /internal/v1/notificaciones`.

## Antes de implementar cualquier UT

Leer [`.claude/rules/`](../../../../.claude/rules/): [NESTJS_BACKEND.md](../../../../.claude/rules/backend/NESTJS_BACKEND.md), [ARQUITECTURA_BACKEND.md](../../../../.claude/rules/ARQUITECTURA_BACKEND.md) y [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La bandeja es de la escuela, no de cada Administrador.** Pagos y Académico conocen el `escuelaCodigo` pero no quién la administra; buscarlo en Identidad sumaría una llamada entre servicios por aviso. El aviso se guarda con clave de partición = `escuelaCodigo` (un UUID: no choca con ninguna persona) y lo lee cualquier `ADMIN_ESCUELA` cuyo token traiga esa escuela (claim `escuela`). Los Administradores de una escuela comparten el estado de leído.
2. **Mismo almacenamiento y mismas operaciones que CU38**: tabla `Notificaciones`, índice `NoLeidas`, TTL de 180 días, idempotencia por `eventoCodigo`. No hay tabla ni endpoint nuevos.
3. **Solo bandeja**: un aviso a la escuela no lee preferencias ni manda correo o push.
4. **Destino exclusivo**: el pedido trae `usuarioCodigo` o `escuelaCodigo`, nunca los dos (400 si vienen ambos o ninguno).
5. **El Super admin no tiene bandeja** (no pertenece a una escuela): sigue con 403 `ERR_SYS_05`. Un `ADMIN_ESCUELA` sin claim `escuela`, también.
6. **Académico** no guarda la escuela en la reserva: la toma de su franja, con `FranjaRepository` (el mismo acceso que ya usa `CompensadorDeReserva`).

## Contrato de datos

| Campo del pedido interno | Regla |
|---|---|
| `usuarioCodigo` | UUID, obligatorio si no viene `escuelaCodigo` |
| `escuelaCodigo` | UUID, opcional; no puede venir con `usuarioCodigo` |

| Quién lee | Bandeja |
|---|---|
| `ESTUDIANTE`, `INSTRUCTOR` | la suya (`sub`) |
| `ADMIN_ESCUELA` | la de su escuela (claim `escuela`) |
| otro rol | 403 `ERR_SYS_05` |

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU53-BE-01](./UT-CU53-BE-01.md) | Notificaciones: avisos a la escuela y bandeja del Administrador | — |
| [UT-CU53-BE-02](./UT-CU53-BE-02.md) | Pagos: avisar a la escuela de compras y reembolsos | 01 |
| [UT-CU53-BE-03](./UT-CU53-BE-03.md) | Académico: avisar a la escuela de clases canceladas | 01 |

**Cadena:** `01 → 02, 03`.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, marcar todos, A3, A4, A5 | Bandeja de la escuela por REST y GraphQL | 01 |
| Generación 1 y 2 (CA01, CA02) | Compra confirmada y reembolso ejecutado | 02 |
| Generación 3 (CA03) | Clase confirmada cancelada | 03 |
| CA04, CA09 | Idempotencia y solo bandeja | 01, 02, 03 |
| A6 | Mejor esfuerzo | 02, 03 |

## Follow-ups (no se implementan en estas UT)

- Otros eventos para la escuela (examen programado, alumno nuevo...), con su caso de uso.
- Push al navegador del Administrador cuando llega un aviso a su escuela.

## Qué NO incluyen estas UT

- La campana y la pantalla del panel ([UT-FRONTEND](../UT-FRONTEND/README.md)).
- Cambios en la app móvil: su bandeja (CU38) no cambia.
