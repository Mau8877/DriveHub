# UT-CU09-BE-01 — Alta de cuenta parametrizada por rol y por código

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU09-BE-01` |
| **Nombre** | Alta de cuenta parametrizada por rol y por código |
| **Caso de uso** | CU09-BE |
| **Microservicio** | `identidad` |
| **Depende de** | — (CU08 completo) |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md). Como base, `AltaEstudiante` tal como la dejó CU08.

## Solicitud de negocio

El Administrador va a dar de alta instructores con las mismas reglas de datos que un estudiante (edad mínima, correo y CI únicos, contraseña, dirección calculada): no se quiere copiar esa lógica. Además, Escuela necesita poder elegir el código de la persona para poder deshacer el alta aunque no reciba la respuesta.

## Objetivo

La regla compartida de alta de una cuenta de escuela sirve para cualquier rol, no solo para `ESTUDIANTE`, y puede crear la cuenta con un código dado, sin cambiar el comportamiento del registro ni del alta de estudiantes.

## Alcance

- Renombrar `application/service/AltaEstudiante` a **`AltaCuenta`** y sumar el rol como parámetro: `Usuario crear(Datos datos, Supplier<UUID> escuela, String rol, String categoriaSigla)`. El rol se busca con `RolRepositoryPort.buscarPorNombre(rol)`; si no está sembrado, `IllegalStateException` como hoy. `Datos` suma un `UUID codigo` **opcional** (nulo = lo genera el servicio, como hoy). Si llega y ya existe una cuenta con ese código, `ValidationException` `ERR_SYS_01` ("El código ya existe") antes de guardar nada. Los mensajes de log pasan de "Registro rechazado" a "Alta rechazada" (el módulo sigue siendo `usuarios`).
- `UsuarioPersistenceAdapter.guardar` usa el código del `Usuario` si trae uno y deja que `BaseModel` lo genere si no. Para eso `BaseModel` suma un método `asignarCodigo(UUID)` que solo funciona mientras el Model todavía no tiene código (después, `IllegalStateException`): el código público nunca cambia una vez creado.
- `RegistrarEstudianteService` y `CrearEstudianteService` pasan `RolNombres.ESTUDIANTE`; todo lo demás queda **igual**, incluido el **orden de las validaciones** (edad → escuela → correo → CI).
- Tests: `AltaEstudianteTest` pasa a `AltaCuentaTest` y suma casos con `RolNombres.INSTRUCTOR` (la cuenta sale con ese rol y `categoriaSigla` nula), con un rol no sembrado (`IllegalStateException`), con un código dado (la cuenta queda con ese código) y con un código que ya existe (`ERR_SYS_01`, no se guarda). Una prueba de persistencia comprueba que `guardar` respeta el código dado y que `asignarCodigo` no sobrescribe uno existente. Los tests de `RegistrarEstudianteService`, `CrearEstudianteService` y los de integración existentes siguen en verde, solo con el constructor ajustado.
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`: donde decía `AltaEstudiante.crear()` pasa a decir `AltaCuenta.crear()`.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/AltaCuenta.java` (antes `AltaEstudiante.java`)
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/model/BaseModel.java` y `.../repository/UsuarioPersistenceAdapter.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/RegistrarEstudianteService.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/application/service/CrearEstudianteService.java`
- `projects/backend/identidad/src/test/java/com/drivehub/identidad/application/service/AltaCuentaTest.java` (antes `AltaEstudianteTest.java`), los tests de los dos servicios y una prueba de persistencia del código dado
- `docs/errors/identidad/ERRORES_IDENTIDAD.md`

## Fuera de alcance (no tocar)

- Crear instructores: es la UT 03.
- Cambiar una regla de datos (edad, contraseña, CI, correo) o un código de error.
- El puerto de usuarios y los endpoints.

## Resultado esperado

Existe `AltaCuenta` y el registro y el alta de estudiantes se comportan exactamente como antes.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores.
2. Con el stack levantado, el registro público de un estudiante y el alta de uno por el Administrador siguen funcionando.
