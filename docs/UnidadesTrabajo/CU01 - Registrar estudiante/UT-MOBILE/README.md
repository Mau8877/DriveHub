# Unidades de trabajo — CU01-MOB

**Caso de uso:** Pantalla móvil de registro del estudiante, con el código de su escuela, sin sesión.
**Aplicación:** `projects/mobile` (Flutter, estudiante e instructor) — ver [MICROSERVICIOS.md](../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU01 - Registrar estudiante`](../../CasosDeUso/CU01%20-%20Registrar%20estudiante.md)
**Recurso HTTP / contrato:** `POST /api/v1/usuarios` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [WIDGETS_UI_MOBILE.md](../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) y [TESTING_MOBILE.md](../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La escuela se escribe como "código de escuela"** (el `slug`, ej. `escuelaexample`), un campo del formulario: la app no tiene enlaces por escuela. No se consulta antes el nombre de la escuela: si el código es inválido, el backend responde `ERR_IDE_03` y se muestra bajo ese campo.
2. **Convención de DTOs (pendiente en las rules):** se define en UT 01 antes del primer `service` real. Propuesta: los DTOs Dart viven en `features/{feature}/models/`, son clases inmutables con `toJson`/`fromJson` escritos a mano (sin generación de código), y el `service` es el único que los construye.
3. **Navegación mínima:** `core/routing/` está vacío. UT 02 crea el `GoRouter` único con solo `/login` (provisional) y `/registro`; el redirect por sesión llega con CU02. `main.dart` pasa a `MaterialApp.router`.
4. **Sin auto-login** (CA02): al registrarse bien se vuelve a `/login` con un aviso de cuenta creada.
5. **Reglas de validación idénticas a backend y web** (ver [UT-CU01-FE-01](../UT-FRONTEND/UT-CU01-FE-01.md)): nombres y apellidos obligatorios (máx. 80); fecha de nacimiento obligatoria, pasada y con al menos 18 años cumplidos; correo obligatorio, con formato válido (máx. 120); contraseña de 9 a 64 caracteres; teléfono opcional (máx. 20); código de escuela obligatorio. Mensajes en español.
6. **Errores del backend en español:** `ERR_IDE_01` bajo el correo, `ERR_IDE_02` bajo la fecha de nacimiento, `ERR_IDE_03` bajo el código de escuela, otros errores en un aviso general.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU01-MOB-01](./UT-CU01-MOB-01.md) | Convención de DTOs Dart | — |
| [UT-CU01-MOB-02](./UT-CU01-MOB-02.md) | `GoRouter` mínimo y `MaterialApp.router` | — |
| [UT-CU01-MOB-03](./UT-CU01-MOB-03.md) | Reglas de validación del registro | — |
| [UT-CU01-MOB-04](./UT-CU01-MOB-04.md) | Modelo y `RegistroService` | 01 |
| [UT-CU01-MOB-05](./UT-CU01-MOB-05.md) | Provider del registro | 04 |
| [UT-CU01-MOB-06](./UT-CU01-MOB-06.md) | Pantalla de registro y aviso en el login | 02, 03, 05 |

**Cadena:** `01 → 04 → 05 → 06`; `02` y `03` son independientes y entran a `06`.

## Criterios de aceptación cubiertos

| CA (CU01) | Descripción | UTs |
|-----------|-------------|-----|
| CA02 | Tras registrarse, va al login sin sesión (la web muestra "ingresa desde la app", ver UT-CU01-FE-03) | 02, 06 |
| CA03 | Campos obligatorios y correo inválido, error por campo | 03, 06 |
| CA04 | Contraseña de menos de 9 caracteres | 03, 06 |
| CA05, CA06 | Menor de 18 años rechazado; exactamente 18 aceptado | 03, 06 |
| CA07 | Correo en uso (`ERR_IDE_01`) | 05, 06 |
| CA08 | Escuela inexistente o inactiva (`ERR_IDE_03`) | 05, 06 |
| CA01, CA09 | La petición nunca envía rol | 04 |

CA10 y CA11 son de backend/gateway.

## Follow-ups (no se implementan en estas UT)

- Login real, almacenamiento seguro del token y redirect por sesión (CU02).
- Enlaces profundos (deep links) por escuela.
- Los shells `AdminShell` y `UsuarioShell` conectados al router.

## Qué NO incluyen estas UT

- Login, sesión, `flutter_secure_storage` o el redirect global por sesión.
- La feature `health` (se mantiene tal cual).
- Pantallas de otros CU, ni de roles distintos del estudiante.
- Cambios en la paleta de colores.

## Commits sugeridos

Los cambios en `projects/mobile` van en commits `[MOBILE]`; la edición de reglas en `.claude/rules/` (UT 01) va en un commit `[REPO]` aparte.
