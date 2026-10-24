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

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Ya existe un auto con esa placa. La placa es única en toda la plataforma,
  también frente a autos de otra escuela o dados de baja (CU07). También se responde si dos
  registros simultáneos con la misma placa chocan: el segundo pierde.
- **Dónde se lanza:** `RegistrarVehiculoUseCase.ejecutar()` y `VehiculoRepositoryImpl.guardar()`

## `ERR_ESC_08`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** El auto pedido no existe en la escuela de quien lo pide. Un auto de otra
  escuela se responde igual, para no revelar qué autos existen (CU07).
- **Dónde se lanza:** `ObtenerVehiculoUseCase.ejecutar()`, `EditarVehiculoUseCase.ejecutar()` y
  `CambiarEstadoVehiculoUseCase.ejecutar()`

## `ERR_ESC_09`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El cambio no es posible desde el estado actual del auto (CU07): editar o
  cambiar de estado un auto dado de baja, enviar a mantenimiento uno que no está disponible,
  habilitar uno que no está en mantenimiento, o dar de baja uno que ya lo está.
- **Dónde se lanza:** `EditarVehiculoUseCase.ejecutar()` y `CambiarEstadoVehiculoUseCase.ejecutar()`

## `ERR_ESC_10`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El token de acceso no trae la escuela de la cuenta (claim `escuela`), así
  que no hay escuela sobre la cual operar (CU07). Un Administrador de escuela siempre la trae.
- **Dónde se lanza:** `VehiculoController.escuelaDe()`
