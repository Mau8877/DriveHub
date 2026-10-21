# UT-CU01-BE-08 — Dominio y Model: fecha de nacimiento, escuela y `RolNombres`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU01-BE-08` |
| **Nombre** | Dominio y Model: fecha de nacimiento, escuela y `RolNombres` |
| **Caso de uso** | CU01-BE |
| **Microservicio** | `identidad` |
| **Depende de** | UT-CU01-BE-07 |

## Antes de implementar

[ARQUITECTURA_HEXAGONAL_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_HEXAGONAL_BACKEND.md) (dos modelos por concepto) y [CONVENCIONES_JAVA_BACKEND.md](../../../../.claude/rules/backend/CONVENCIONES_JAVA_BACKEND.md) (Lombok, `@Setter` por campo).

## Solicitud de negocio

El dominio y el Model de usuario deben reflejar las columnas nuevas, y el código necesita nombres de rol sin escribir texto suelto.

## Objetivo

`Usuario` y `UsuarioModel` tienen `fechaNacimiento` y `escuelaCodigo`, y existe `RolNombres` con los 4 roles.

## Alcance

- `domain/model/Usuario`: agregar `LocalDate fechaNacimiento` y `UUID escuelaCodigo` (nullable).
- `UsuarioModel`: agregar `fecha_nacimiento` (`LocalDate`, `nullable = false`) y `escuela_codigo` (`UUID`, nullable), con `@Setter` solo donde haga falta (el Model se crea y rara vez se modifica).
- `domain/model/RolNombres`: clase final con constantes de texto `SUPER_ADMIN`, `ADMIN_ESCUELA`, `INSTRUCTOR`, `ESTUDIANTE` (los roles son datos, no un enum cerrado, ver SEGURIDAD_AUTH_BACKEND.md).
- Test: el contexto arranca con `ddl-auto=validate` contra la migración de UT 07 (cubierto por el test de arranque existente) y un test unitario simple del builder de `Usuario` no es necesario.

Archivos esperados (crear/tocar):

- `.../domain/model/Usuario.java`, `.../domain/model/RolNombres.java`
- `.../infrastructure/adapter/out/persistence/model/UsuarioModel.java`

## Fuera de alcance (no tocar)

- Puertos, servicios, mappers y adaptadores (UT 09 a 12).
- Reglas de edad o de rol (viven en el servicio, UT 10).
- Los Models de rol, permiso y asociaciones, que no cambian.
- Un enum de roles.

## Resultado esperado

El servicio arranca con Hibernate validando `UsuarioModel` contra la tabla migrada; el dominio expone fecha de nacimiento y escuela.

## Validación

1. `./mvnw test` en `identidad` pasa (`IdentidadApplicationTests` arranca con `validate`).
2. `./mvnw checkstyle:check` sin errores.
3. `Usuario` y `domain/model/` siguen sin importar nada de Spring/JPA.
