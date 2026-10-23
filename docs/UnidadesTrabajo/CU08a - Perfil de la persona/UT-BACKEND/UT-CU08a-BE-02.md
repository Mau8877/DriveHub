# UT-CU08a-BE-02 — Dominio y persistencia de `Perfil` dentro de `Usuario`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU08a-BE-02` |
| **Nombre** | Dominio y persistencia de `Perfil` dentro de `Usuario` |
| **Caso de uso** | CU08a-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU08a-BE-01 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (dominio puro, dos modelos por concepto con mapper propio, transacciones en `application/service/`), [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Todo el código que hoy lee o escribe nombres, teléfono, foto o fecha de nacimiento tiene que seguir funcionando igual, ahora desde el perfil.

## Objetivo

`Usuario` contiene un `Perfil`; la persistencia los guarda y lee juntos; todo lo que ya existía se comporta exactamente igual que antes.

## Alcance

- **Refactor sin cambio de comportamiento visible.** Esta UT no agrega endpoints ni campos al contrato: los campos nuevos (`ci`, `sexo`, ubicación, categoría) existen en el dominio pero ningún caso de uso los pide todavía (las UT 04 a 06 lo hacen).
- `domain/model/Perfil` (`@Getter @Builder(toBuilder = true)`, sin JPA): `nombres`, `apellidos`, `telefono`, `fotoUrl`, `fechaNacimiento`, `ci`, `sexo` (`SexoPersona`), `direccion`, `latitud`, `longitud` (`BigDecimal`), `categoriaSigla`. Sin id interno; la identidad es la del `Usuario`.
- `Usuario` pierde `nombres`, `apellidos`, `telefono`, `fotoUrl` y `fechaNacimiento` y gana `Perfil perfil`. Para no tocar decenas de llamadas de golpe, puede conservar métodos de conveniencia que delegan (`getNombres()`, `getFechaNacimiento()`...) **solo durante esta UT**; se eliminan antes de cerrarla y los llamadores usan `getPerfil()`.
- Persistencia: el mapper (`UsuarioPersistenceMapper` o el equivalente actual) traduce `UsuarioModel` + `PerfilModel` ↔ `Usuario` + `Perfil`. El adaptador que implementa `UsuarioRepositoryPort` guarda y actualiza **ambos en la misma transacción** (`guardar`, `actualizar`) y los lee juntos (`buscarPorEmail`, `buscarPorCodigo`, `buscarAdministradorDeEscuela`): una consulta con `JOIN FETCH` del perfil, sin N+1. `eliminarDefinitivamente` (CU05) borra también el perfil. Los métodos que hoy piden solo cuenta siguen igual.
- Compatibilidad con lo existente: donde un caso de uso crea una cuenta sin los datos nuevos (registro, alta del Administrador, seeders), esta UT los llena con **valores provisionales** (`ci` derivado de un UUID corto y único, `sexo = MASCULINO`, `direccion = "X"`); las UT 04, 06 y 07 los reemplazan por los reales. Cada sitio con un valor provisional lleva un comentario `TODO: UT-CU08a-BE-0N` indicando qué UT lo quita.
- Adaptar a `getPerfil()`: `PerfilArmador`, `AuthController`, `PerfilController`, `UsuarioController`, los servicios de foto (`ActualizarFotoPerfilService`, `QuitarFotoPerfilService`), de correo (`SolicitarCambioCorreoService`, `ConfirmarCambioCorreoService`, `SolicitarVerificacionCorreoService`), `SolicitarRecuperacionService`, `EditarPerfilService`, `CrearAdministradorEscuelaService`, `RegistrarEstudianteService`, `IdentidadSeeder`, el emisor del JWT si usa algún dato de nombre, y los correos HTML que muestran el nombre.
- Pruebas: **toda la suite existente de Identidad sigue en verde** (se ajustan los builders de los tests para crear `Perfil`). Nuevas: el mapper en los dos sentidos (con y sin ubicación, con y sin categoría); el adaptador guarda `usuario` y `perfil` atómicamente (si falla el perfil, no queda la cuenta); `buscarPorEmail` trae el perfil con una sola consulta; `actualizar` cambia datos del perfil sin tocar los de acceso; `eliminarDefinitivamente` no deja filas huérfanas.

Archivos esperados (crear/tocar):

- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/Perfil.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/domain/model/Usuario.java`
- `projects/backend/identidad/src/main/java/com/drivehub/identidad/infrastructure/adapter/out/persistence/` (mapper, `UsuarioJpaRepository`, el adaptador de persistencia)
- Los servicios, controllers y el seeder listados arriba, solo en lo necesario para compilar y conservar su comportamiento
- Los tests existentes que construyen `Usuario`, ajustados

## Fuera de alcance (no tocar)

- Cambiar contratos HTTP, mensajes o reglas de negocio.
- Pedir, validar o exponer `ci`, `sexo`, ubicación o categoría (UT 04 a 06).
- Renombrar `PerfilUsuario` ni `PerfilArmador`: son la vista para el cliente.
- Las migraciones (UT 01) y el servicio Escuela.

## Resultado esperado

Identidad compila y pasa toda su suite con el perfil separado; nadie nota el cambio.

## Validación

1. `./mvnw test` y `./mvnw checkstyle:check` en `identidad` sin errores y sin tests desactivados.
2. Con el stack levantado: iniciar sesión con `adminexample@gmail.com`, ver el perfil (`GET /api/v1/usuarios/me`) y subir una foto siguen funcionando como antes.
