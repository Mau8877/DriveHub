# UT-CU08a-FE-05 — Esquema de escuelas con el perfil del Administrador

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-FE-05` |
| **Nombre** | Esquema de escuelas con el perfil del Administrador |
| **Caso de uso** | CU08a-FE |
| **Microservicio** | `escuela` (web) |
| **Depende de** | UT-CU08a-FE-02 |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) (el esquema es datos, reglas iguales a backend, tests `*.esquema.spec.ts`), [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md). Como base, `features/escuelas/schemas/escuelas.esquema.ts` (CU05).

## Solicitud de negocio

Al crear una escuela, el Super admin tiene que dar también el CI, el sexo y la ubicación del Administrador.

## Objetivo

El formulario "Nueva escuela" pide los datos nuevos del Administrador y muestra los errores del CI bajo su campo.

## Alcance

- `escuelas.esquema.ts`, sección "Administrador" del `crear`: campos nuevos `administrador.ci` (requerido, 5 a 15, patrón `[A-Za-z0-9-]+`, `mitad` junto al sexo), `administrador.sexo` (`tipo: 'seleccion'`, opciones `MASCULINO` "Masculino" y `FEMENINO` "Femenino", requerido, `mitad`) y `administrador.ubicacion` (`tipo: 'ubicacion'`, requerido, que escribe `administrador.latitud` y `administrador.longitud`), con mensajes en español iguales a los del registro. `erroresDeCampo` suma `ERR_IDE_19` → `administrador.ci` ("Este CI ya está registrado"). El orden del formulario: nombres, apellidos, fecha de nacimiento, sexo, CI, teléfono, correo, contraseña y ubicación.
- Los límites del CI se documentan en el comentario del archivo junto a los demás, como reglas que tienen que seguir iguales.
- Tests (`escuelas.esquema.spec.ts`): existen los campos nuevos con sus reglas, el patrón y las opciones del sexo; el tipo `ubicacion` es requerido; `erroresDeCampo` mapea `ERR_IDE_19`; con `validar` de `@drivehub/gestion`, un CI de 4 caracteres o con espacios falla. Los tests existentes del esquema se ajustan.

Archivos esperados (crear/tocar):

- `projects/frontend/escuela/src/app/features/escuelas/schemas/escuelas.esquema.ts`
- `projects/frontend/escuela/src/app/features/escuelas/tests/escuelas.esquema.spec.ts`

## Fuera de alcance (no tocar)

- Los tipos de campo (UT 02) y el mapa (UT 01).
- La edición de escuelas (solo cambia el nombre) y las acciones.
- La ruta y la pantalla anfitriona.
- Backend (UT 09).

## Resultado esperado

Crear una escuela en la web exige el perfil completo del Administrador.

## Validación

1. `pnpm --dir projects/frontend/escuela test` y `lint` sin errores.
2. Con todo levantado: iniciar sesión como `superadmin@gmail.com`, crear una escuela con un punto en el mapa y verificar que el Administrador puede iniciar sesión; repetir el CI muestra el error bajo ese campo.
