# Caso de uso

| Id   | Nombre               | Paquete                 | Prioridad | Estado    |
| ---- | -------------------- | ----------------------- | --------- | --------- |
| CU01 | Registrar estudiante | P1. Identidad y acceso  | Alta      | Pendiente |

**Actor principal:** Estudiante (persona aún sin cuenta)
**Actores secundarios:** —
**Objetivo:** Crear su propia cuenta en la plataforma, asociada a una escuela, para poder iniciar sesión después.
**Plataforma:** Móvil y Web

> **Modificado por [CU08a: Perfil de la persona](CU08a%20-%20Perfil%20de%20la%20persona.md).** El registro pasa a pedir además CI, sexo y ubicación en un mapa, a crear la cuenta junto con su perfil y a rechazar un CI ya registrado (`ERR_IDE_19`). Donde este documento contradiga a CU08a, manda CU08a.

**Descripción:**
Una persona que quiere estudiar en una autoescuela abre el enlace de registro de esa escuela (web o app) y completa un formulario con sus datos personales y una contraseña. El sistema comprueba que la escuela existe y está activa, que la persona cumple la edad mínima y que el correo no está en uso. Si todo es válido, crea la cuenta con el rol `ESTUDIANTE`, asociada a esa escuela, y lleva a la persona a la pantalla de inicio de sesión. **No** inicia sesión automáticamente. Si algún dato es inválido, el sistema lo informa campo por campo sin crear la cuenta.

Este CU cubre **solo** el auto-registro del estudiante. Los demás roles no se registran solos: el Super admin se crea con un seeder, el Administrador de escuela lo crea el Super admin al crear la escuela ([CU05](CU05%20-%20Gestionar%20escuelas.md)) y el Instructor lo crea el Administrador de escuela (CU01_2, a definir).

---

## Precondiciones

- La escuela indicada existe y está activa (el estudiante llega a ella por el enlace o código de su escuela).
- El correo no pertenece a ninguna cuenta existente en la plataforma.

## Flujo principal

1. La persona abre el enlace de registro de su escuela.
2. El sistema muestra el formulario: nombres, apellidos, fecha de nacimiento, correo, contraseña y teléfono (opcional).
3. La persona completa el formulario y lo envía.
4. El sistema valida los datos: campos obligatorios, formato de correo, contraseña de al menos 9 caracteres y edad mínima.
5. El sistema verifica que la escuela existe y está activa, y que el correo no está registrado.
6. El sistema crea la cuenta con estado `ACTIVO`, rol `ESTUDIANTE` y la escuela indicada.
7. El sistema informa que el registro fue exitoso. En la app, lleva a la persona a la pantalla de inicio de sesión; en la web, le muestra una pantalla que le indica que su cuenta está lista y que debe ingresar desde la app móvil (los estudiantes no usan la web).

## Flujos alternativos y excepciones

- **A1 (paso 4):** Falta un campo obligatorio o su formato es inválido → el sistema rechaza el registro e indica el error de cada campo.
- **A2 (paso 4):** La contraseña tiene menos de 9 caracteres → se rechaza indicando la longitud mínima.
- **A3 (paso 4):** La persona no alcanza la edad mínima (18 años cumplidos a la fecha del registro) → se rechaza con un mensaje que explica el requisito de edad.
- **A4 (paso 5):** La escuela no existe o no está activa → se rechaza sin revelar más detalle.
- **A5 (paso 5):** El correo ya está registrado → se rechaza con un mensaje de correo en uso.

## Reglas de negocio

- Solo el rol `ESTUDIANTE` puede crearse por auto-registro; la persona no puede elegir ni enviar otro rol.
- Una cuenta pertenece a **una sola escuela**.
- El correo es **único en toda la plataforma**, no solo dentro de una escuela.
- Edad mínima: **18 años**, configurable (no se escribe a mano en el código). Se calcula con la fecha de nacimiento a la fecha del registro.
- Contraseña: mínimo **9 caracteres**, sin otras restricciones de complejidad. Nunca se guarda en texto plano ni se registra en logs ([SEGURIDAD_AUTH_BACKEND.md](../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md), [LOGGING_BACKEND.md](../../.claude/rules/backend/LOGGING_BACKEND.md)).
- El registro es un endpoint público, protegido por el rate limit del gateway.
- Las reglas de validación de cada campo son las mismas en web, mobile y backend.
- No hay verificación de correo en este CU: se agrega junto con el SMTP en [CU04](CU04%20-%20Recuperar%20contrase%C3%B1a.md).
- El perfil de estudiante del servicio Escuela **no** se crea aquí: lo gestiona CU08.

## Postcondiciones

- Existe una cuenta `ACTIVO` con rol `ESTUDIANTE` y su escuela, lista para iniciar sesión desde la app móvil (CU02).
- No hay sesión iniciada.

## Criterios de aceptación

- **CA01:** Dado que la escuela existe y está activa y los datos son válidos, el sistema debe crear la cuenta `ACTIVO` con rol `ESTUDIANTE` y la escuela indicada, y responder éxito sin devolver sesión ni contraseña.
- **CA02:** Dado que el registro fue exitoso, la app debe llevar a la persona a la pantalla de inicio de sesión y la web debe mostrarle una pantalla que le indique que ingrese desde la app móvil, sin sesión iniciada en ninguna de las dos.
- **CA03:** Dado que falta un campo obligatorio o el correo tiene formato inválido, el sistema debe responder 400 con el error del campo y no crear la cuenta.
- **CA04:** Dado que la contraseña tiene menos de 9 caracteres, el sistema debe responder 400 y no crear la cuenta.
- **CA05:** Dado que la persona tiene menos de 18 años a la fecha del registro, el sistema debe responder 400 con un código de error propio y no crear la cuenta.
- **CA06:** Dado que la persona cumple exactamente 18 años el día del registro, el sistema debe aceptar el registro.
- **CA07:** Dado que el correo ya está registrado (en cualquier escuela), el sistema debe responder 409 y no crear la cuenta.
- **CA08:** Dado que la escuela no existe o no está activa, el sistema debe responder 404 y no crear la cuenta.
- **CA09:** Dado que la petición incluye un rol, el sistema debe ignorarlo y asignar siempre `ESTUDIANTE`.
- **CA10:** Dado cualquier registro, la contraseña no debe guardarse en texto plano ni aparecer en los logs.
- **CA11:** Dado que se superó el límite de peticiones, el sistema debe responder 429 (`ERR_SYS_02`).

## Desarrollador

Mauro
