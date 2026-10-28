# Unidades de trabajo — CU35-MOB

**Caso de uso:** Consultar tutoriales: la pestaña «Tutoriales» de «Estudio» (estudiante) y la «Biblioteca» (instructor), con búsqueda, tipo y apertura dentro de la app.
**Microservicio(s) / micro-frontend(s):** `mobile` (Flutter) — consume el gateway ([UT-BACKEND](../UT-BACKEND/README.md))
**Fuente CU:** [`CU35 - Consultar tutoriales.md`](../../../CasosDeUso/CU35%20-%20Consultar%20tutoriales.md)
**Recurso HTTP / contrato:** `/tutoriales/publicados`

## Antes de implementar cualquier UT

Leer [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md) y [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md).

## Decisiones de diseño

- **Una sola vista para los dos roles** (`BibliotecaTutorialesView`): el backend decide la audiencia por el rol del token, así que la app pide lo mismo; el estudiante la ve como pestaña de «Estudio» y el instructor como pestaña «Biblioteca» de su pantalla de tutoriales.
- **Se abre dentro de la app** con el visor de PDF y el reproductor de [CU33-MOB-01](../../CU33%20-%20Gestionar%20tutoriales%20del%20instructor/UT-MOBILE/UT-CU33-MOB-01.md); la tarjeta (`TarjetaTutorial`) es la misma de «Mis tutoriales».
- **La búsqueda se envía al pulsar «buscar»**, no en cada letra.
- «Estudio» ya tiene cuatro pestañas: pasa a una barra desplazable.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU35-MOB-01](./UT-CU35-MOB-01.md) | Biblioteca de tutoriales | CU35-BE-01 y CU33-MOB-01 |

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| Flujo principal, CA01, CA02 | Lista, búsqueda y tipo | 01 |
| A1 a A4, CA06 a CA08 | Estado vacío, reintento y abrir PDF o video | 01 |

## Follow-ups (no se implementan en estas UT)

- «Cargar más» (hoy se trae la página 1 de 50).

## Qué NO incluyen estas UT

- La gestión de los propios tutoriales (CU33).
