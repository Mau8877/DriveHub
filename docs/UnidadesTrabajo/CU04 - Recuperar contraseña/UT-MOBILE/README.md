# Unidades de trabajo — CU04-MOB

**Caso de uso:** Recuperar la contraseña desde la app móvil: pantalla para pedir el enlace y acceso desde el login.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU04 - Recuperar contraseña`](../../../CasosDeUso/CU04%20-%20Recuperar%20contrase%C3%B1a.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/recuperar` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **La app solo pide el enlace;** elegir la contraseña nueva ocurre en la página web a la que lleva el enlace del correo (se abre en el navegador del teléfono). No hay pantalla de nueva contraseña ni deep links.
2. **Mismo mensaje fijo que la web,** con la advertencia permanente de que hace falta el correo verificado.
3. **Ruta pública:** `/recuperar` se suma a las rutas que el redirect global deja pasar sin sesión.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU04-MOB-01](./UT-CU04-MOB-01.md) | Pantalla "Recuperar contraseña" y su servicio | — |
| [UT-CU04-MOB-02](./UT-CU04-MOB-02.md) | Ruta pública `/recuperar` y enlace en el login | 01 |

**Cadena:** `01 → 02`. Necesita el backend de CU04 solo para la validación manual.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal 1 | "¿Olvidaste tu contraseña?" en el login | 02 |
| Pasos 2 a 4, A1 | Pedir el enlace con validación y mensaje fijo | 01 |

## Follow-ups (no se implementan en estas UT)

- Abrir el enlace directamente en la app (deep links / App Links).

## Qué NO incluyen estas UT

- Verificar el correo ([CU04b](../../CU04b%20-%20Verificar%20correo/UT-MOBILE/README.md)).
- Pantalla de nueva contraseña dentro de la app.
- Cambios en el login más allá del enlace.

## Commits sugeridos

Un commit `[MOBILE]` para todo CU04-MOB.
