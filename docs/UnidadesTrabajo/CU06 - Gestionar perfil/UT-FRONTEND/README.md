# Unidades de trabajo — CU06-FE

**Caso de uso:** Gestionar el perfil en la web: opción "Editar perfil" en el menú del avatar del shell, pantalla con los datos personales, la foto, la contraseña y el correo, y la página pública que confirma el cambio de correo.
**Micro-frontend(s):** `shell` (UT 01 a 03) e `identidad` (UT 04 a 09) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU06 - Gestionar perfil`](../../../CasosDeUso/CU06%20-%20Gestionar%20perfil.md)
**Recurso HTTP / contrato:** ruta `/perfil` (con sesión) y página pública `/account/change-email?token=…`; consume los endpoints de [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http)

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El menú del avatar y la ruta `/perfil` son del shell; las pantallas son del remoto `identidad`** (el CU06 es de Identidad). El remoto expone `./routes-perfil` aparte de `./routes`, igual que `escuela` expone `./routes-escuelas` para CU05. `/perfil` es de primer nivel (dueño: el shell), dentro del layout y con `sesionGuard`; sirve a todos los roles, así que no lleva `rolGuard`.
2. **No es una pantalla `<dh-gestion>`:** no es un CRUD de listado sino un perfil con secciones (datos, foto, contraseña, correo) y flujos propios; va como pantalla nativa de Angular ([GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md), "Cuándo se usa y cuándo no").
3. **El avatar se actualiza por eventos, no por importación:** al guardar nombres o foto, el remoto emite `perfil-actualizado` con `{ nombres, apellidos, fotoUrl }` (`emitirEvento` de `@drivehub/mf-contract`) y el shell lo escucha y actualiza su sesión. Los remotos no tocan la sesión del shell.
4. **Sesión con `fotoUrl`:** `UsuarioSesion` gana `fotoUrl` (llega en login y renovación, ver la [UT-CU06-BE-11](../UT-BACKEND/UT-CU06-BE-11.md)); la URL prefirmada se renueva con cada renovación de sesión.
5. **Sin avatar, iniciales:** si no hay foto, el avatar muestra las iniciales de nombres y apellidos sobre un fondo de tokens (nada de colores propios).
6. **Un componente por sección,** dentro de `features/perfil/components/`, todos en la pantalla `perfil`. Las reglas de validación de nombres, apellidos, teléfono, fecha de nacimiento y contraseña se **reutilizan de `features/registro`** por su `index.ts` (mismas reglas que el registro, como ya hace `cuenta`).
7. **El cliente revisa la foto antes de enviarla** (tipo JPG/PNG/WebP, ≤ 5 MB, ≤ 4000×4000 px) solo para dar feedback inmediato; el backend decide.
8. **La foto sube como `multipart/form-data`** con `FormData` y la parte `archivo`; el `apiInterceptor` no debe fijar `Content-Type` en ese caso (el navegador agrega el `boundary`).
9. **Páginas públicas de cuenta en inglés** (regla de rutas): `/account/change-email`, junto a `verify-email`.

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU06-FE-01](./UT-CU06-FE-01.md) | Sesión con `fotoUrl` y evento `perfil-actualizado` | `shell` | — |
| [UT-CU06-FE-02](./UT-CU06-FE-02.md) | Menú del avatar con "Editar perfil" | `shell` | 01 |
| [UT-CU06-FE-03](./UT-CU06-FE-03.md) | Ruta `/perfil` en el shell | `shell` | 04 |
| [UT-CU06-FE-04](./UT-CU06-FE-04.md) | Feature `perfil`: tipos, servicio de API y `./routes-perfil` | `identidad` | — |
| [UT-CU06-FE-05](./UT-CU06-FE-05.md) | Pantalla de perfil y edición de datos personales | `identidad` | 04 |
| [UT-CU06-FE-06](./UT-CU06-FE-06.md) | Sección de la foto | `identidad` | 05 |
| [UT-CU06-FE-07](./UT-CU06-FE-07.md) | Sección de cambiar la contraseña | `identidad` | 05 |
| [UT-CU06-FE-08](./UT-CU06-FE-08.md) | Sección de cambiar el correo | `identidad` | 05 |
| [UT-CU06-FE-09](./UT-CU06-FE-09.md) | Página pública `change-email` | `identidad` | — |

**Cadena:** `01 → 02`; `04 → 03, 05`; `05 → 06, 07, 08`; la 09 es independiente. Necesita el backend de CU06 solo para la validación manual.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | "Editar perfil" en el menú del avatar abre el perfil | 02, 03, 05 |
| CA02, CA03 | Editar datos personales y errores de campo | 04, 05 |
| CA04 a CA07, CA09 | Subir, rechazar y quitar la foto | 06 |
| CA10, CA11, CA12 | Cambiar la contraseña y sus errores | 07 |
| CA13, CA14, CA17 | Pedir el cambio de correo y sus errores | 08 |
| CA15, CA16, CA18 | Confirmar el enlace y ver el correo nuevo | 08, 09 |
| Avatar en la barra | Foto o iniciales, actualizados sin recargar | 01, 02, 05, 06 |

## Follow-ups (no se implementan en estas UT)

- Mover "Cerrar sesión" al menú del avatar (hoy sigue como botón de la cabecera).
- Mostrar el avatar de otras personas (instructor, compañeros) cuando exista un caso de uso que lo necesite.
- Recortar la foto en el navegador antes de subirla.

## Qué NO incluyen estas UT

- La app móvil ([UT-MOBILE](../UT-MOBILE/README.md)).
- Recuperar contraseña y verificar el correo (CU04, CU04b).
- Los remotos `escuela`, `pagos` y los React.

## Commits sugeridos

Un commit `[FRONT]` por micro-frontend: `shell` (UT 01 a 03) e `identidad` (UT 04 a 09).
