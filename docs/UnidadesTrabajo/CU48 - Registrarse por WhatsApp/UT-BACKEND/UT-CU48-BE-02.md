# UT-CU48-BE-02 — Validar un dato del registro

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU48-BE-02` |
| **Nombre** | Validar un dato del registro |
| **Caso de uso** | CU48-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [CU01](../../../CasosDeUso/CU01%20-%20Registrar%20estudiante.md) y [CU08a](../../../CasosDeUso/CU08a%20-%20Perfil%20de%20la%20persona.md) (las reglas), y `AltaCuenta` (donde ya viven las reglas compartidas).

## Solicitud de negocio

El bot debe saber si un dato sirve apenas la persona lo escribe, con las mismas reglas que el registro de la app.

## Objetivo

`POST /internal/v1/whatsapp/registros/validaciones` valida un campo del registro.

## Alcance

- **Sin copiar reglas.** El formato lo valida `ValidadorDatoRegistro` (adaptador web) con `Validator.validateValue` sobre las MISMAS anotaciones de `RegistrarEstudianteRequest` (nombres, apellidos, fecha, correo, CI, sexo, latitud y longitud), así que no pueden desalinearse. Lo que depende de los datos guardados o de la fecha (edad mínima, correo y CI sin usar) lo resuelve el caso de uso `ValidarDatoRegistroUseCase`, que llama a métodos públicos nuevos de `AltaCuenta` (`verificarEdadMinima`, `verificarCorreoLibre`, `verificarCiLibre`), los mismos que usa `AltaCuenta.crear`, con el mismo orden de validaciones que antes: el registro normal no cambia.
- Campos aceptados: `nombres`, `apellidos`, `fechaNacimiento` (`yyyy-MM-dd`), `sexo`, `ci`, `correo` y `ubicacion` («lat,lon»). Cualquier otro campo → 400 `ERR_SYS_01`. Correo y CI repetidos → 409 (`ERR_IDE_01`, `ERR_IDE_19`); menor de edad → 400 `ERR_IDE_02`.
- Controller `RegistroWhatsappInternoController`: `POST /internal/v1/whatsapp/registros/validaciones` con `{ campo, valor }`; `200` con `data: null` si es válido.
- Tests: cada campo con valores válidos, inválidos y de borde, correo y CI duplicados, campo desconocido; `@WebMvcTest` del sobre y servicio unitario.

Archivos esperados (crear/tocar): `AltaCuenta.java`, `ValidarDatoRegistroUseCase`/`DatoRegistro`/`ValidarDatoRegistroService`, `ValidadorDatoRegistro.java`, `ValidarDatoRegistroRequest.java`, el controller y los tests.

## Fuera de alcance (no tocar)

- Guardar la solicitud (UT-03) y cambiar cualquier regla o mensaje del registro normal.

## Resultado esperado

El bot puede preguntar «¿sirve este dato?» y obtener la misma respuesta que daría el formulario de la app.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `projects/backend/identidad`; los tests existentes de registro siguen pasando.
2. Con la credencial: una fecha de hace 10 años → 400 `ERR_IDE_02`; un CI ya registrado → 409 `ERR_IDE_19`; un nombre válido → 200.
