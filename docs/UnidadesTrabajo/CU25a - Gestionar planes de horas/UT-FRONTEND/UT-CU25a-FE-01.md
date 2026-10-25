# UT-CU25a-FE-01 — Campo decimal y columna moneda en `<dh-gestion>`

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU25a-FE-01` |
| **Nombre** | Campo decimal y columna moneda en `<dh-gestion>` |
| **Caso de uso** | CU25a-FE |
| **Microservicio** | `packages/gestion` |
| **Depende de** | — |

## Antes de implementar

[GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md). Leer en `packages/gestion/src/` cómo `dh-gestion.js`, `reglas.js`, `estilos.js` e `index.d.ts` tratan hoy `tipo: 'numero'` (solo enteros) y `formatearFecha` (formato propio, sin depender del idioma del navegador).

## Solicitud de negocio

El formulario de un plan necesita un costo con centavos, y el listado tiene que mostrar el dinero como `Bs 1.250,00` y `US$ 179,60`, igual en cualquier navegador.

## Objetivo

`<dh-gestion>` sabe pedir un número con decimales y dibujar una cantidad de dinero en una columna o en un dato del detalle.

## Alcance

- **Campo `tipo: 'decimal'`** (`dh-gestion.js`, `reglas.js`): un `<input type="text" inputmode="decimal">` (no el `type="number"` nativo, que muestra flechas y acepta `e`). Propiedad `decimales` (por defecto 2). La persona puede escribir punto o coma como separador (se normaliza, sin espacios ni separador de miles). `validar` acepta solo dígitos con hasta `decimales` decimales y rechaza vacío (si es requerido), letras, más de un separador y más decimales de los permitidos; `min` y `max` son el rango del **valor** (como en `numero`); los mensajes salen del esquema (`mensajes.requerido`, `patron`, `min`, `max`) o por defecto ("Debe ser un número con hasta 2 decimales", "Debe ser al menos 0,01", "No puede superar 99999999,99"). Al enviar, el valor viaja como **número** (`Number`), no como texto, y un campo opcional vacío no se envía. Al editar, el valor del registro se muestra con exactamente `decimales` decimales (`450` → `450.00`).
- **Columna y dato de detalle `tipo: 'moneda'`** (con `moneda: 'BOB' | 'USD'`): el valor numérico se dibuja con prefijo `Bs ` o `US$ `, miles con punto, decimal con coma y dos decimales (`1250` → `Bs 1.250,00`; `179.6` → `US$ 179,60`). Formateo escrito a mano (no `Intl`), por la misma razón que `formatearFecha`. Un valor nulo o que no es número se muestra vacío en la tabla y "—" en el detalle. Ancho por defecto de la columna: `9rem`.
- `index.d.ts`: `Campo.tipo` suma `'decimal'`, `Campo.decimales`, `Columna.tipo` y `CampoDetalle.tipo` suman `'moneda'`, y ambos suman `moneda`.
- Documentar ambos en [GESTION_FRONTEND.md](../../../../.claude/rules/frontend/GESTION_FRONTEND.md) (una viñeta junto a las demás).
- Pruebas (`vitest`, jsdom):
  - `reglas.test.js`: decimal válido con punto y con coma, `0,01` y `99999999.99` en el límite, más decimales de los permitidos, texto, vacío con y sin `requerido`, `min` y `max` con sus mensajes.
  - `dh-gestion.test.js`: el campo dibuja el input de texto con `inputmode="decimal"`; escribir `1250,5` envía `1250.5` como número; un campo opcional vacío no se envía; la edición muestra `450.00`; la columna moneda en Bs y en USD, con miles y con valor nulo; el dato de detalle moneda con valor y vacío.
  - Los campos y columnas existentes siguen funcionando (las pruebas actuales pasan sin cambios).

Archivos esperados (crear/tocar):

- `packages/gestion/src/dh-gestion.js`, `reglas.js`, `index.d.ts` y, si hace falta, `estilos.js`
- `packages/gestion/src/dh-gestion.test.js` y `reglas.test.js`
- `.claude/rules/frontend/GESTION_FRONTEND.md`

## Fuera de alcance (no tocar)

- Otras monedas, o separadores y símbolos según el idioma del navegador.
- Convertir entre monedas en el cliente (la conversión la hace el backend).
- El esquema de planes (UT 02).
- El comportamiento del tipo `numero` (sigue siendo solo enteros).

## Resultado esperado

Un esquema puede declarar un campo con centavos y una columna de dinero, y el cuerpo enviado respeta su forma.

## Validación

1. `pnpm --dir packages/gestion test` sin errores, con las pruebas existentes incluidas.
2. Revisar `pnpm --dir projects/frontend/escuela test`, `identidad`, `examenes` y `contenido`: ninguna pantalla existente cambia.
