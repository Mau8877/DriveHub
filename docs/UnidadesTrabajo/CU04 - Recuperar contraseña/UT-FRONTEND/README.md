# Unidades de trabajo — CU04-FE

**Caso de uso:** Recuperar la contraseña en la web: enlace desde el login, página para pedir el correo y página para elegir la contraseña nueva.
**Micro-frontend(s):** `identidad` (UT 01, 02) y `shell` (UT 03) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU04 - Recuperar contraseña`](../../../CasosDeUso/CU04%20-%20Recuperar%20contrase%C3%B1a.md)
**Recurso HTTP / contrato:** páginas públicas `/cuenta/recuperar` y `/cuenta/restablecer?token=…`; consume `POST /api/v1/auth/recuperar` y `/restablecer` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

**Requiere [CU04b-FE](../../CU04b%20-%20Verificar%20correo/UT-FRONTEND/README.md) implementado** (`routes-cuenta`, `CuentaService` y el montaje de `/cuenta` en el shell). Leer en [`.claude/rules/`](../../../../.claude/rules/): [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Las dos páginas son públicas y viven en el remoto `identidad`,** bajo `/cuenta`, igual que la de verificar el correo. Funcionan con o sin sesión y también desde el navegador de un teléfono, que es como llegan quienes solo usan la app.
2. **Mensaje fijo al pedir el enlace,** sin depender de lo que responda el backend, y con la advertencia permanente de que hace falta el correo verificado.
3. **Misma regla de contraseña que el registro** (mínimo 9) y confirmación obligatoria; el backend sigue siendo la fuente de verdad.
4. **Tras restablecer no se inicia sesión:** la página lleva a `/login`.

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU04-FE-01](./UT-CU04-FE-01.md) | Página "Recuperar contraseña" | `identidad` | — |
| [UT-CU04-FE-02](./UT-CU04-FE-02.md) | Página "Restablecer contraseña" | `identidad` | 01 |
| [UT-CU04-FE-03](./UT-CU04-FE-03.md) | Enlace en el login del shell | `shell` | — |

**Cadena:** `01 → 02`; la 03 es independiente. Necesita el backend de CU04 solo para la validación manual.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 | "¿Olvidaste tu contraseña?" en el login | 03 |
| Pasos 2 a 4, A1 | Pedir el enlace con validación y mensaje fijo | 01 |
| Pasos 6 a 10, A4, A5 | Restablecer, enlace inválido y contraseña inválida | 02 |
| CA10, A6 | La confirmación debe coincidir | 02 |

## Follow-ups (no se implementan en estas UT)

- Medidor de fortaleza de la contraseña.
- Reenvío del enlace desde la página de enlace vencido.

## Qué NO incluyen estas UT

- Verificar el correo (CU04b).
- Cambios en los remotos `escuela`, `pagos` y los React.

## Commits sugeridos

Un commit `[FRONT]` por micro-frontend: `identidad` (UT 01, 02) y `shell` (UT 03).
