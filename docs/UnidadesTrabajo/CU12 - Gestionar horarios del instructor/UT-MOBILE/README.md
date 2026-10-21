# Unidades de trabajo — CU12-MOB

**Caso de uso:** Gestionar horarios del instructor en la app móvil: ver y crear horarios semanales, desactivarlos, y ver, bloquear y desbloquear las franjas de una fecha.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU12 - Gestionar horarios del instructor`](../../../CasosDeUso/CU12%20-%20Gestionar%20horarios%20del%20instructor.md)
**Recurso HTTP / contrato:** `/api/v1/plantillas-horario` y `/api/v1/franjas` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (incluye la convención de `models/`), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto. Tomar como modelo la feature `features/auth/` (models, service, provider, validators, screens y tests).

## Decisiones de diseño

1. **Una feature nueva: `lib/features/horarios/`** con `models/`, `services/`, `providers/`, `validators/` y `screens/`. Los widgets propios de una pantalla se agregan en `widgets/` solo si hacen falta.
2. **Solo para el rol `INSTRUCTOR`.** La app es una sola para estudiante e instructor, así que la sección "Horarios" del `UsuarioShell` aparece solo si los `roles` de la sesión incluyen `INSTRUCTOR`; el router también redirige a `/inicio` si otro rol entra a una ruta de horarios. La autorización real sigue en el backend.
3. **Rutas en inglés**, como el resto de la app (`/register`, `/forgot-password`): `/schedules/new` (formulario) y `/schedules/slots` (franjas). El listado de horarios es la sección "Horarios" del shell, no una ruta aparte.
4. **Sin paginar en pantalla:** el backend pagina siempre, y la app pide una sola página de 50 horarios (un instructor tiene muy pocos: como máximo unos pocos por día de la semana) y de 50 franjas por fecha. Si el volumen real lo pide, se agrega scroll infinito (follow-up).
5. **`Pagina<T>` vive en `features/horarios/models/`** por ahora. Se mueve a `core/network/` cuando una segunda feature la necesite (mismo criterio que `shared/`).
6. **Mismas reglas de validación que el backend** (FORMULARIOS_MOBILE.md): hora fin posterior a la de inicio, duración 15–240 y ventana múltiplo exacto, vigencia no anterior a hoy. Los mensajes de error de negocio del backend (`ERR_ACA_*`) ya vienen en español y se muestran tal cual.
7. **Fechas y horas:** el backend usa `yyyy-MM-dd` para fechas y `HH:mm` (24 horas) para horas; la app las manda y las lee en ese formato y las muestra igual. La hora "ahora" para ocultar acciones sobre franjas ya empezadas es la del teléfono (solo UX; el backend es quien rechaza con `ERR_ACA_10`).
8. **Colores solo del tema** (`Theme.of(context).colorScheme` / `AppColors`): nada de `Color(0x...)` sueltos (ver WIDGETS_UI_MOBILE.md).

## Prerrequisito para la prueba manual

Hace falta una cuenta con rol `INSTRUCTOR` para entrar a la app, y **hoy no existe ninguna**: el seeder de desarrollo de Identidad solo crea al Super admin y al Administrador de escuela, y el registro (CU01) solo crea estudiantes. Opciones: pedirle a Mauro que agregue un instructor de ejemplo al `IdentidadSeeder` (cambio en Identidad, fuera de este CU), o esperar a CU09. Mientras tanto, la validación de punta a punta de la UT 07 queda pendiente de esa cuenta; las demás UT se validan con `flutter test`.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU12-MOB-01](./UT-CU12-MOB-01.md) | Modelos de horarios y franjas | — |
| [UT-CU12-MOB-02](./UT-CU12-MOB-02.md) | `HorariosService` | 01 |
| [UT-CU12-MOB-03](./UT-CU12-MOB-03.md) | Validadores del formulario de horario | — |
| [UT-CU12-MOB-04](./UT-CU12-MOB-04.md) | Providers de horarios y franjas | 01, 02 |
| [UT-CU12-MOB-05](./UT-CU12-MOB-05.md) | Pantallas "Horarios" y "Nuevo horario" | 03, 04 |
| [UT-CU12-MOB-06](./UT-CU12-MOB-06.md) | Pantalla "Franjas" | 04 |
| [UT-CU12-MOB-07](./UT-CU12-MOB-07.md) | Sección "Horarios" en el shell y rutas | 05, 06 |

**Cadena:** `01 → 02 → 04 → 05, 06 → 07`; la 03 es independiente y la necesita la 05. Para probar de punta a punta hace falta el backend del CU12 (todas las UT de `UT-BACKEND`) y la cuenta de instructor.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal | Ver mis horarios (con o sin inactivos) | 01, 02, 04, 05, 07 |
| F1, A1 a A4 | Crear un horario y sus validaciones de formulario | 01, 02, 03, 04, 05 |
| F2, A12 | Ver las franjas de una fecha (también vacía) | 01, 02, 04, 06 |
| F3, F4, A9 a A11 | Bloquear y desbloquear | 02, 04, 06 |
| F5 | Desactivar un horario | 02, 04, 05 |
| A5 a A11 (mensajes) | Mostrar el error de negocio del backend | 02, 05, 06 |
| A13 | Solo Instructor | 07 |
| A15 | Sin conexión: informar y permitir reintentar | 04, 05, 06 |

S1 (generación diaria) y las reglas de negocio son del backend.

## Follow-ups (no se implementan en estas UT)

- Editar un horario y reactivar uno desactivado (cuando existan en el backend).
- Scroll infinito en las listas.
- Vista de calendario semanal.
- Mostrar el vehículo de la franja (CU10).
- Notificar al instructor cuando se reserva una de sus franjas (CU36).

## Qué NO incluyen estas UT

- Cambios en el login, la sesión, el router base ni `ApiClient`: se reutilizan tal como están.
- Reservar, cancelar o reprogramar una franja (estudiante: CU13 a CU15) ni consultar el horario de clases (CU16).
- `AdminShell`: la app móvil no es para administradores.

## Commits sugeridos

Un commit `[MOBILE]` para todo CU12-MOB.
