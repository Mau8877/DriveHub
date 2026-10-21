# Unidades de trabajo — CU04b-FE

**Caso de uso:** Verificar el correo en la web: botón en la barra del shell y página pública que confirma el enlace.
**Micro-frontend(s):** `shell` (UT 01, 02, 04) e `identidad` (UT 03) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU04b - Verificar correo`](../../../CasosDeUso/CU04b%20-%20Verificar%20correo.md)
**Recurso HTTP / contrato:** botón "Verificar correo" en el layout; página pública `/account/verify-email?token=…`; consume `POST /api/v1/auth/verificar-correo` y `/confirmar` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ANGULAR_FRONTEND.md](../../../../.claude/rules/frontend/ANGULAR_FRONTEND.md), [ARQUITECTURA_FRONTEND.md](../../../../.claude/rules/ARQUITECTURA_FRONTEND.md), [TAILWIND_STYLES_FRONTEND.md](../../../../.claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **El botón vive en el shell;** el remoto `identidad` solo aporta la página pública. Los remotos no reciben la verificación en `@drivehub/mf-contract`.
2. **Las páginas públicas de cuenta van en `/account`,** un `exposes` nuevo (`./routes-cuenta`) separado de `./routes-publicas` (registro). CU04 sumará `recuperar` y `restablecer` ahí.
3. **El botón desaparece al refrescar la sesión:** `SesionService` renueva cuando la pestaña vuelve a estar visible y el correo sigue sin verificar. No se agrega un endpoint de "mi perfil".
4. **Sin sesión no hay botón,** y la página `/account/verify-email` funciona con o sin sesión.

## Orden de ejecución

| Código | Nombre | Micro-frontend | Depende de |
|--------|--------|----------------|------------|
| [UT-CU04b-FE-01](./UT-CU04b-FE-01.md) | Sesión con `correoVerificado` y pedir la verificación | `shell` | — |
| [UT-CU04b-FE-02](./UT-CU04b-FE-02.md) | Botón "Verificar correo" en la barra superior | `shell` | 01 |
| [UT-CU04b-FE-03](./UT-CU04b-FE-03.md) | Página `verificar-correo` en `identidad` | `identidad` | — |
| [UT-CU04b-FE-04](./UT-CU04b-FE-04.md) | Montar `/account` público en el shell | `shell` | 03 |

**Cadena:** `01 → 02`; `03 → 04`. Necesita el backend de CU04b solo para la validación manual.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | Botón visible solo sin verificar | 01, 02 |
| CA02 | Pedir el correo e informarlo | 01, 02 |
| CA03, CA04 | Página que confirma o explica el enlace inválido | 03, 04 |
| CA07 | El botón se oculta al volver tras verificar | 01 |

## Follow-ups (no se implementan en estas UT)

- Menú de usuario con el correo y el estado de verificación.
- Sincronizar la verificación entre pestañas sin esperar a que vuelvan a estar visibles.

## Qué NO incluyen estas UT

- Recuperar y restablecer la contraseña (CU04).
- Cambios en los remotos `escuela`, `pagos` y los React.

## Commits sugeridos

Un commit `[FRONT]` por micro-frontend: `shell` (UT 01, 02, 04) e `identidad` (UT 03).
