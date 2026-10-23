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
- **Dónde se lanza:** `RegistrarEstudianteService.registrar()`,
  `SolicitarCambioCorreoService.solicitar()` (CU06, el correo nuevo ya lo usa otra cuenta) y
  `UsuarioPersistenceAdapter.guardar()` (carrera entre dos registros)

## `ERR_IDE_02`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La persona no alcanza la edad mínima para registrarse
  (18 años cumplidos a la fecha del registro, configurable con
  `app.registro.edad-minima`).
- **Dónde se lanza:** `RegistrarEstudianteService.registrar()` y, al cambiar la fecha de
  nacimiento del perfil (CU06), `EditarPerfilService.editar()`

## `ERR_IDE_03`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La escuela indicada en el registro no existe o no está
  activa. Se responde igual en ambos casos para no revelar qué escuelas
  existen.
- **Dónde se lanza:** `RegistrarEstudianteService.registrar()`

## `ERR_IDE_04`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** Las credenciales de inicio de sesión son incorrectas: el correo
  no existe o la contraseña no coincide. Se responde igual en ambos casos para no
  revelar qué correos existen (CU02).
- **Dónde se lanza:** `IniciarSesionService.iniciar()`

## `ERR_IDE_05`

- **HTTP:** 423
- **Excepción:** `LockedException`
- **Significado:** La cuenta está bloqueada temporalmente por 5 intentos fallidos
  consecutivos (15 minutos, se levanta sola). También se devuelve en el propio 5.º
  intento fallido, y mientras dure el bloqueo aunque la contraseña sea correcta, o
  al renovar la sesión de una cuenta bloqueada (CU02).
- **Dónde se lanza:** `IniciarSesionService.iniciar()`, `RenovarSesionService.renovar()`

## `ERR_IDE_06`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** La cuenta está inactiva. Solo se informa con la contraseña
  correcta, o al renovar la sesión (CU02).
- **Dónde se lanza:** `IniciarSesionService.iniciar()`, `RenovarSesionService.renovar()`

## `ERR_IDE_07`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** La escuela de la cuenta está inactiva (la desactivó el Super admin, CU05)
  o ya no existe, al iniciar o renovar la sesión. Afecta a todas las cuentas de esa escuela:
  Administrador, Instructores y Estudiantes. El mensaje es "Tu escuela está inactiva. Contacta al administrador de la plataforma". El Super admin no
  pertenece a ninguna escuela y no se ve afectado (CU02).
- **Dónde se lanza:** `SesionEmisora.verificarEscuelaActiva()` (usada por
  `IniciarSesionService.iniciar()` y `RenovarSesionService.renovar()`)

## `ERR_IDE_08`

- **HTTP:** 403
- **Excepción:** `ForbiddenException`
- **Significado:** El rol de la cuenta no puede usar la plataforma desde la que
  entra: en la web solo Super admin y Administrador de escuela; en la app móvil
  solo Estudiante e Instructor (CU02).
- **Dónde se lanza:** `SesionEmisora.verificarPlataforma()` (usada por
  `IniciarSesionService.iniciar()` y `RenovarSesionService.renovar()`)

## `ERR_IDE_09`

- **HTTP:** 401
- **Excepción:** `UnauthorizedException`
- **Significado:** La sesión renovable no sirve: falta, no existe, venció, ya fue
  revocada o ya se usó (la renovación rota el token), o es de otro dispositivo.
  El cliente debe llevar a la persona al inicio de sesión (CU02). También se devuelve en los
  endpoints de perfil (CU06) si la cuenta de la sesión ya no existe.
- **Dónde se lanza:** `RenovarSesionService.renovar()`, y en CU06 los servicios de perfil, foto,
  contraseña y cambio de correo

## `ERR_IDE_10`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La persona pidió el correo de verificación pero el correo de su
  cuenta ya estaba verificado (CU04b). El cliente debe actualizar la sesión y ocultar
  el botón.
- **Dónde se lanza:** `SolicitarVerificacionCorreoService.solicitar()`

## `ERR_IDE_11`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El enlace recibido por correo no sirve: no existe, ya se usó, venció,
  fue reemplazado por uno más nuevo o es de otro tipo (un enlace de verificación no
  sirve para restablecer la contraseña ni al revés; tampoco uno de cambio de correo sirve para los otros). Se responde igual en todos los
  casos para no revelar cuál fue (CU04b, CU04).
- **Dónde se lanza:** `ConfirmarCorreoService.confirmar()` (CU04 lo reutiliza en
  `RestablecerContrasenaService` y CU06 en `ConfirmarCambioCorreoService`)

## `ERR_IDE_12`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** La escuela ya tiene un Administrador: se intentó crear una segunda
  cuenta `ADMIN_ESCUELA` para el mismo `escuela_codigo` (CU05). Una escuela tiene un solo
  Administrador.
- **Dónde se lanza:** `CrearAdministradorEscuelaService.crear()`

## `ERR_IDE_13`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** Se intentó revertir al Administrador de una escuela, pero esa cuenta ya
  inició sesión alguna vez y se considera en uso: no se borra (CU05). Solo se puede
  revertir una cuenta que nunca se usó.
- **Dónde se lanza:** `RevertirAdministradorEscuelaService.revertir()`

## `ERR_IDE_14`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La contraseña actual que mandó la persona no es la de su cuenta, al cambiar
  la contraseña o al pedir el cambio de correo (CU06). Es 400 y no 401 a propósito: un 401
  haría que el cliente intente renovar la sesión. No suma intentos fallidos ni bloquea la cuenta.
- **Dónde se lanza:** `CambiarContrasenaService.cambiar()`, `SolicitarCambioCorreoService.solicitar()`

## `ERR_IDE_15`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La contraseña nueva es igual a la actual (CU06). Se comprueba contra el hash
  guardado.
- **Dónde se lanza:** `CambiarContrasenaService.cambiar()`

## `ERR_IDE_16`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La foto de perfil no pasó la revisión básica (CU06): el archivo está vacío, su
  contenido no es una imagen JPG, PNG o WebP (se mira el contenido, no la extensión), no se puede
  leer completa, tiene datos extra después del final de la imagen o supera 4000×4000 píxeles. No
  se guarda nada.
- **Dónde se lanza:** `ActualizarFotoPerfilService.actualizar()`

## `ERR_IDE_17`

- **HTTP:** 400
- **Excepción:** `ValidationException` (en el `GlobalExceptionHandler` cuando Spring corta la
  petición antes del controller)
- **Significado:** La foto de perfil pesa más de 5 MB (CU06). Lo corta Spring al recibir el
  multipart (`spring.servlet.multipart.max-file-size`) y el servicio lo vuelve a comprobar.
- **Dónde se lanza:** `ActualizarFotoPerfilService.actualizar()`,
  `GlobalExceptionHandler.handleArchivoDemasiadoGrande()`

## `ERR_IDE_18`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El correo nuevo que la persona pidió es igual al correo actual de su cuenta
  (CU06). Si lo usa otra cuenta, la respuesta es `ERR_IDE_01`.
- **Dónde se lanza:** `SolicitarCambioCorreoService.solicitar()`

## `ERR_IDE_19`

- **HTTP:** 409
- **Excepción:** `ConflictException`
- **Significado:** El CI ya pertenece a otra cuenta (CU08a). El CI es único en toda la plataforma y
  se compara sin espacios y en mayúsculas.
- **Dónde se lanza:** `RegistrarEstudianteService.registrar()`,
  `CrearAdministradorEscuelaService.crear()`, `EditarPerfilService.editar()` y
  `UsuarioPersistenceAdapter` (si dos operaciones simultáneas chocan con el mismo CI)
