# UT-CU18-BE-09 — Prueba de punta a punta y documentación

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU18-BE-09` |
| **Nombre** | Prueba de punta a punta y documentación |
| **Caso de uso** | CU18-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU18-BE-07`, `UT-CU18-BE-08` |

## Antes de implementar

[TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md#mongodb) (todo cambio de esquema se documenta). Tomar como modelo UT-CU17-BE-10. Prerrequisito: `academico`, `identidad`, `pagos`, `escuela` y el gateway levantados con **este** código (`docker compose up -d --build academico`) y el token de un instructor de la escuela con clases ya cerradas con `PRESENTE`, una cerrada con `AUSENTE` y una con el cierre interrumpido.

## Solicitud de negocio

Antes de dar el CU18 por terminado hay que ver, con el servicio real y MongoDB real, que evaluar una clase funciona como dice el CU y que la nota llega al historial.

## Objetivo

El CU18 queda verificado de punta a punta contra el stack real y su documentación al día.

## Alcance

Preparar en `clases` y `reservas` de la base de desarrollo (con datos de prueba que se borran al terminar) clases del instructor de prueba y una de otro instructor. Con el token del instructor, por el gateway:

1. **Catálogo:** `GET /pruebas-clase/criterios` → `200` con los tres tipos y cinco criterios cada uno.
2. **Evaluar con todo bien** (`MANEJO`, los cinco criterios en 8) → `201`; total `80,00`, `APROBADO`; existe la prueba y su nota en `resultados` con `origen = PRUEBA_CLASE`, título «Prueba de clase: Manejo» y el mismo puntaje; la clase, la reserva, la franja y la billetera **no** cambian.
3. **Reprobar:** otro tipo con total bajo 70 → `201`, `REPROBADO`, nota con `aprobado: false`.
4. **Repetir el mismo tipo** → `409 ERR_ACA_40`, sin segunda prueba ni segunda nota.
5. **Criterio faltante, sobrante, repetido o de otro tipo** → `400 ERR_ACA_41`; nada se guarda.
6. **Puntaje de 11, de -1 y de 7,3** → `400 ERR_ACA_41`.
7. **Observaciones de 501 caracteres** → `400 ERR_SYS_01`.
8. **Clase `AUSENTE`** → `409 ERR_ACA_39`.
9. **Clase con el cierre interrumpido** (`Clase` guardada, reserva todavía `CONFIRMADA`) → `409 ERR_ACA_39`.
10. **Clase de otro instructor, o inexistente** → `404 ERR_ACA_35`.
11. **Prueba sin nota:** borrar a mano la nota de una prueba y repetir el `POST` → `201` con la prueba ya guardada (sus datos, no los del reintento), **una sola** prueba y la nota otra vez presente.
12. **Dos peticiones a la vez** del mismo tipo → una `201` y la otra `409 ERR_ACA_40`; una sola prueba.
13. **Consultar:** `GET /pruebas-clase?filter%5BclaseCodigo%5D=...` → `200` con las pruebas y su `meta`; sin el filtro → `400 ERR_SYS_01`; de una clase ajena → `404 ERR_ACA_35`.
14. **Agenda de CU16:** `GET /reservas/agenda` trae `evaluable: true` en la clase cerrada sin pruebas, `conPrueba: true` en la que ya tiene alguna, `evaluable: false` cuando tiene las tres, y `false` en las `NO_ASISTIO` y `CONFIRMADA`.
15. **Historial (CU23):** con el token del estudiante, `GET /resultados` (filtro por prueba de clase) muestra las notas de los pasos 2 y 3; con el del instructor, las ve en las notas de sus alumnos.
16. **Roles:** con el token de un estudiante → `403 ERR_SYS_05`; sin token → `401 ERR_SYS_04`.

Documentación, en el mismo cambio:

- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`, colección `pruebas_clase`: confirmar lo anotado en la UT 03 (índice único `(claseId, tipo)`), que `criterios` guarda copia de `descripcion` y `maximo`, que `fecha` es la de la clase y que cada prueba tiene su nota en `resultados`.
- `docs/CasosDeUso/CU18 - Gestionar prueba de clases.md`: estado `En progreso` (o `Hecho` cuando la app móvil también esté).
- Este README: la tabla «Estado de la implementación» y las «Notas de implementación» con lo que quedó distinto al construirlo y los resultados de esta prueba.
- CU16 (UT-BE README y CU): mencionar que la agenda ganó `evaluable` y `conPrueba`.
- `docs/CasosDeUso/CU23 - Consultar resultados y notas.md`: ya no dice «cuando exista» para la prueba de clase.

Archivos esperados (crear/tocar):

- `docs/DriveHub/DISENO_BASE_DE_DATOS.md`
- `docs/CasosDeUso/CU18 - Gestionar prueba de clases.md`, `CU23 - Consultar resultados y notas.md`
- `docs/UnidadesTrabajo/CU18 - Gestionar prueba de clases/UT-BACKEND/README.md`
- `docs/UnidadesTrabajo/CU16 - Consultar horario de clases/UT-BACKEND/README.md`

## Fuera de alcance (no tocar)

- Código de producción: si la prueba encuentra un fallo, se corrige en la UT que corresponda y se anota.
- La app móvil (`UT-MOBILE`) y su prueba manual.
- Datos de producción: todo lo que se crea para probar se borra al terminar.

## Resultado esperado

Cada escenario del CU18 se comprobó con el stack real, los datos de prueba se limpiaron y la documentación refleja lo construido.

## Validación

1. Los 16 escenarios dan el resultado indicado.
2. `./mvnw test` y `./mvnw checkstyle:check` en `academico` siguen pasando.
3. `DISENO_BASE_DE_DATOS.md` describe el uso real de la colección `pruebas_clase`.
