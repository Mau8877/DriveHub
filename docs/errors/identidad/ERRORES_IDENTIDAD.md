# Errores — Identidad (`IDE`)

Códigos del microservicio Identidad (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `IdentidadErrorCodes` (`common/exception/`).

## `ERR_IDE_01`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El correo ya pertenece a una cuenta de la plataforma.
  El correo es único en toda la plataforma, no solo dentro de una escuela
  (CU01, registro del estudiante). También se devuelve si dos registros con
  el mismo correo llegan a la vez y la base rechaza el segundo.
- **Dónde se lanza:** `RegistrarEstudianteService.registrar()` y
  `UsuarioPersistenceAdapter.guardar()` (carrera entre dos registros)

## `ERR_IDE_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La persona no alcanza la edad mínima para registrarse
  (18 años cumplidos a la fecha del registro, configurable con
  `app.registro.edad-minima`).
- **Dónde se lanza:** `RegistrarEstudianteService.registrar()`

## `ERR_IDE_03`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La escuela indicada en el registro no existe o no está
  activa. Se responde igual en ambos casos para no revelar qué escuelas
  existen.
- **Dónde se lanza:** `RegistrarEstudianteService.registrar()`
