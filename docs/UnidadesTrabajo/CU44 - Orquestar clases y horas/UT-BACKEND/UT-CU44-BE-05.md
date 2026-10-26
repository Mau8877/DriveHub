# UT-CU44-BE-05 — Infra: variable de la credencial de servicio

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-05` |
| **Nombre** | Infra: variable de la credencial de servicio |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `infra` (`docker-compose.yml` y `.env.example`, scope `INFRA`) |
| **Depende de** | — |

## Antes de implementar

La decisión 3 del [README](./README.md#decisiones-de-diseño). [CONVENCIONES_GIT.md](../../../../.claude/rules/CONVENCIONES_GIT.md) (esta UT va en su propio commit `[INFRA]`). Ver cómo `PAGOS_URL_RETORNO` ya se declara obligatoria en `docker-compose.yml` y en `.env.example`.

## Solicitud de negocio

Pagos y Académico deben recibir la **misma** credencial de servicio por entorno, sin escribirla en el código ni en el repositorio.

## Objetivo

`docker-compose.yml` entrega `PAGOS_CREDENCIAL_ACADEMICO` a los contenedores `pagos` y `academico`, y `.env.example` documenta cómo generar el valor.

## Alcance

- `docker-compose.yml`:
  - Servicio `pagos`: `PAGOS_CREDENCIAL_ACADEMICO=${PAGOS_CREDENCIAL_ACADEMICO:?Define PAGOS_CREDENCIAL_ACADEMICO en .env (ver .env.example)}`.
  - Servicio `academico`: la **misma** variable, con el mismo valor, hacia `PAGOS_CREDENCIAL_ACADEMICO`.
  - Obligatoria (con `:?`), igual que `PAGOS_URL_RETORNO`: sin ella el stack no arranca, en vez de levantar con la puerta interna cerrada o abierta por descuido.
- `.env.example`: una entrada `PAGOS_CREDENCIAL_ACADEMICO=` con un comentario que diga para qué sirve, que debe ser aleatoria y larga (por ejemplo `openssl rand -hex 32`), distinta por entorno, y que **nunca** se commitea con un valor real.
- No se agrega ningún valor real en ningún archivo versionado.

Archivos esperados (crear/tocar):

- `docker-compose.yml`
- `.env.example`

## Fuera de alcance (no tocar)

- El filtro, las propiedades y el código de Pagos y de Académico (UT 01 y 06).
- Secretos de otros servicios o de otros pares de servicios.
- El `.env` real de cada persona: no se versiona.
- Dockerfiles, nginx y perfiles de compose.

## Resultado esperado

El stack solo levanta con la credencial definida, y Pagos y Académico reciben el mismo valor.

## Validación

1. `docker compose config` con la variable definida muestra el valor en los dos servicios; sin definirla, falla con el mensaje de ayuda.
2. `git diff` no contiene ningún secreto real.
3. Tras `docker compose up -d pagos academico`, el entorno del contenedor trae la variable (`docker compose exec pagos printenv PAGOS_CREDENCIAL_ACADEMICO` no vacío; **no** pegar el valor en ningún chat).
