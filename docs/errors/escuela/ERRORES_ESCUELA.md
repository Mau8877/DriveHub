# Errores — Escuela (`ESC`)

Códigos del microservicio Escuela (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `EscuelaErrorCodes` (`common/exception/`).

## `ERR_ESC_01`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La escuela pedida por su `slug` no existe o está
  inactiva. Se responde igual en ambos casos para no revelar qué escuelas
  existen (CU01, registro del estudiante).
- **Dónde se lanza:** `ObtenerEscuelaPorSlugUseCase.ejecutar()`

## `ERR_ESC_02`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** No existe ninguna escuela con el `codigo` pedido. Lo consulta
  Identidad por la red interna al iniciar y renovar sesión (CU02); una escuela
  suspendida **sí** existe y se devuelve con `activa = false`.
- **Dónde se lanza:** `ObtenerEscuelaPorCodigoUseCase.ejecutar()`

## `ERR_ESC_03`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe una escuela con ese `slug`, activa o desactivada: el slug
  sigue reservado aunque la escuela esté eliminada lógicamente (CU05). También se
  responde si dos creaciones simultáneas chocan con el mismo slug.
- **Dónde se lanza:** `CrearEscuelaUseCase.ejecutar()` y `EscuelaRepositoryImpl.guardar()`

## `ERR_ESC_04`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se intentó desactivar una escuela que ya estaba inactiva (CU05). También
  se responde si dos desactivaciones simultáneas chocan: la segunda no cambia nada.
- **Dónde se lanza:** `DesactivarEscuelaUseCase.ejecutar()`

## `ERR_ESC_05`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se intentó reactivar una escuela que ya estaba activa (CU05). También se
  responde si dos reactivaciones simultáneas chocan: la segunda no cambia nada.
- **Dónde se lanza:** `ReactivarEscuelaUseCase.ejecutar()`

## `ERR_ESC_06`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se intentó editar el nombre de una escuela inactiva: primero hay que
  reactivarla (CU05).
- **Dónde se lanza:** `EditarEscuelaUseCase.ejecutar()`

## `ERR_ESC_07`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La categoría de licencia pedida por sigla no existe en el catálogo (CU08).
  Identidad usa este 404 para saber que la categoría que eligió el Administrador no es válida.
- **Dónde se lanza:** `ObtenerCategoriaLicenciaUseCase.ejecutar()`

## `ERR_ESC_08`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe un auto con esa placa. La placa es única en toda la plataforma,
  también frente a autos de otra escuela o dados de baja (CU07). También se responde si dos
  registros simultáneos con la misma placa chocan: el segundo pierde.
- **Dónde se lanza:** `RegistrarVehiculoUseCase.ejecutar()` y `VehiculoRepositoryImpl.guardar()`

## `ERR_ESC_09`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El auto pedido no existe en la escuela de quien lo pide. Un auto de otra
  escuela se responde igual, para no revelar qué autos existen (CU07).
- **Dónde se lanza:** `ObtenerVehiculoUseCase.ejecutar()`, `EditarVehiculoUseCase.ejecutar()` y
  `CambiarEstadoVehiculoUseCase.ejecutar()`

## `ERR_ESC_10`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El cambio no es posible desde el estado actual del auto (CU07): editar o
  ir a mantenimiento un auto dado de baja, enviar a mantenimiento uno que no está disponible,
  habilitar uno que no está en mantenimiento ni de baja, o dar de baja uno que ya lo está.
- **Dónde se lanza:** `EditarVehiculoUseCase.ejecutar()` y `CambiarEstadoVehiculoUseCase.ejecutar()`

## `ERR_ESC_11`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El token de acceso no trae la escuela de la cuenta (claim `escuela`), así
  que no hay escuela sobre la cual operar (CU07). Un Administrador de escuela siempre la trae.
- **Dónde se lanza:** `VehiculoController.escuelaDe()`

## `ERR_ESC_12`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El número de licencia ya pertenece a otro instructor (CU09). Es único en toda la
  plataforma y se compara sin espacios y en mayúsculas. También se devuelve si dos altas con el
  mismo número llegan a la vez y la base rechaza la segunda.
- **Dónde se lanza:** `CrearInstructorUseCase.ejecutar()`, `EditarInstructorUseCase.ejecutar()` e
  `InstructorRepositoryImpl.guardar()` / `actualizar()` (carrera entre dos operaciones)

## `ERR_ESC_13`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El instructor pedido no existe en la escuela de quien consulta (CU09). Un
  instructor de otra escuela y un código inexistente responden igual: no se distinguen.
- **Dónde se lanza:** `ObtenerInstructorUseCase.ejecutar()` y `EditarInstructorUseCase.ejecutar()`

## `ERR_ESC_14`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** Una de las categorías elegidas para el instructor no existe en el catálogo
  (CU09). La opción "sin categoría" (`SIN`) tampoco vale: un instructor enseña categorías reales.
- **Dónde se lanza:** `CrearInstructorUseCase.ejecutar()` y `EditarInstructorUseCase.ejecutar()`

## `ERR_ESC_15`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El estudiante no es alumno vigente del instructor que pregunta (CU29). Un alumno de otro
  instructor, uno de otra escuela, uno cuya asignación ya terminó y un código inexistente responden igual: no
  se distingue "no existe" de "no es tuyo".
- **Dónde se lanza:** `ConfirmarMiAlumnoUseCase.ejecutar()`

## `ERR_ESC_16`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El auto ya tiene un instructor vigente (CU10). También se responde al cambiar el auto al
  mismo instructor que ya lo usa, y si dos asignaciones simultáneas del mismo auto chocan con el índice único:
  la segunda pierde.
- **Dónde se lanza:** `AsignarVehiculoUseCase.ejecutar()`, `CambiarInstructorVehiculoUseCase.ejecutar()` y
  `AsignacionVehiculoRepositoryImpl.abrir()`

## `ERR_ESC_17`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El auto no está `DISPONIBLE` (está en mantenimiento o de baja), así que no se asigna ni se
  cambia de instructor (CU10).
- **Dónde se lanza:** `AsignarVehiculoUseCase.ejecutar()` y `CambiarInstructorVehiculoUseCase.ejecutar()`
  (vía `ReglasAsignacionVehiculo.vehiculoDisponible()`)

## `ERR_ESC_18`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El instructor no está `ACTIVO` (está de licencia o inactivo), así que no se le asigna un
  auto (CU10).
- **Dónde se lanza:** `AsignarVehiculoUseCase.ejecutar()` y `CambiarInstructorVehiculoUseCase.ejecutar()`
  (vía `ReglasAsignacionVehiculo.instructorActivo()`)

## `ERR_ESC_19`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El auto no tiene un instructor vigente, así que no hay nada que cambiar ni liberar (CU10).
- **Dónde se lanza:** `CambiarInstructorVehiculoUseCase.ejecutar()` y `LiberarVehiculoUseCase.ejecutar()`
  (vía `ReglasAsignacionVehiculo.exigirVigente()`)
