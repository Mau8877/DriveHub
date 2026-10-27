# UT-CU44-BE-13 — Documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-13` |
| **Nombre** | Documentación |
| **Caso de uso** | CU44-BE |
| **Microservicio** | — (solo documentos; scopes `DOCS` y `REPO`) |
| **Depende de** | `UT-CU44-BE-12` |

## Antes de implementar

[CONVENCIONES_GIT.md](../../../../.claude/rules/CONVENCIONES_GIT.md) (`docs/` es `DOCS`; `.claude/` es `REPO`: dos commits), [CASOS_DE_USO.md](../../../../.claude/rules/CASOS_DE_USO.md) (el estado se actualiza a mano). Haber terminado la UT 12 con sus notas.

## Solicitud de negocio

Que la documentación del proyecto describa lo que realmente quedó construido, para que nadie tenga que adivinarlo.

## Objetivo

Los documentos de contrato, el mapa de servicios y el caso de uso reflejan el orquestador, los endpoints internos y la credencial de servicio.

## Alcance

- `docs/DriveHub/CONTRATO_BILLETERA_HORAS.md`: una sección con las dos rutas internas (`/internal/v1/billeteras-horas/consumos` y `/devoluciones`), el header `X-Service-Key`, el cuerpo con `escuelaCodigo` y `estudianteCodigo`, y `ERR_PAG_19`. Aclarar que **no pasan por el gateway** y que las rutas `/api/v1` no cambian.
- `.claude/rules/CASOS_DE_USO.md`: la «excepción vigente» del CU44 ya está escrita (se agregó junto con la definición del CU); comprobar que siga coincidiendo con el CU.
- `.claude/rules/MICROSERVICIOS.md`: una fila en la tabla de llamadas REST entre servicios (Académico → Pagos por el job, con credencial de servicio, CU44). Es un cambio de scope `REPO`, en su propio commit.
- `docs/errors/README.md` y `ERRORES_PAGOS.md`: comprobar que `ERR_PAG_19` esté (lo agrega la UT 01); no se duplica.
- `docs/DriveHub/FLUJO_PAGOS_HORAS_Y_RESERVAS.md` y `docs/DriveHub/SAGA_RESERVA_PAGO.md`: comprobar que describan el orquestador con manejadores, el cliente `ClienteDePagos` y el contrato para CU15 tal como quedó construido (y la UT 16, si se hizo).
- `docs/DriveHub/SAGA_RESERVA_PAGO.md`: en «Pendiente (UT)» marcar lo que ya se hizo y dejar solo lo que queda (S2 y S3, a la espera de CU15).
- `docs/CasosDeUso/CU44 - Orquestar clases y horas.md`: pasar el **Estado** a `En progreso` (el flujo S1 está hecho; S2 y S3 esperan a CU15). Pasar a `Hecho` solo cuando CU15 esté terminado y sus UT del orquestador también.
- Anotar en `docs/UnidadesTrabajo/.../README.md` los follow-ups que sigan vigentes y, en la «Guía para Leonel», los nombres reales de las clases y el test plantilla (`OrquestadorExtensionTest`) tal como quedaron en el código.
- Dejar escrito si la UT 16 (migrar el flujo en vivo del CU14) se hizo o queda como follow-up.

Archivos esperados (crear/tocar):

- `docs/DriveHub/CONTRATO_BILLETERA_HORAS.md`, `docs/DriveHub/SAGA_RESERVA_PAGO.md`
- `docs/CasosDeUso/CU44 - Orquestar clases y horas.md`
- `.claude/rules/MICROSERVICIOS.md`
- `docs/UnidadesTrabajo/CU44 - Orquestar clases y horas/UT-BACKEND/README.md`

## Fuera de alcance (no tocar)

- Código fuente de cualquier servicio.
- Documentar CU15 ni sus estados.
- Reescribir documentos de otros CU.

## Resultado esperado

Quien lea el repositorio encuentra descritos el job, las rutas internas, la credencial y lo que falta.

## Validación

1. Todos los links relativos de los documentos tocados abren (probar con una búsqueda de enlaces rotos o a mano).
2. Los nombres de rutas, header, propiedades y códigos de error coinciden exactamente con el código.
3. `git diff` solo contiene archivos de `docs/` y de `.claude/rules/MICROSERVICIOS.md`.
