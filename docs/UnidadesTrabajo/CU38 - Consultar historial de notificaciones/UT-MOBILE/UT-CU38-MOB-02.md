# UT-CU38-MOB-02 — Pantalla «Notificaciones» y detalle del aviso

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU38-MOB-02` |
| **Nombre** | Pantalla «Notificaciones» y detalle del aviso |
| **Caso de uso** | CU38-MOB |
| **Microservicio** | `mobile` |
| **Depende de** | UT-CU38-MOB-01 |

## Antes de implementar

Leer [WIDGETS_UI_MOBILE.md](../../../../.claude/rules/mobile/WIDGETS_UI_MOBILE.md) (`CockpitScaffold`, `Insignia`, `EstadoVacio`, `AvisoCockpit`, `IndicadorBoton`, `avisar`) y [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md). Ver `preferencias_notificacion_screen.dart` y la pantalla de «Mis alumnos» (listado con carga al llegar al final).

## Solicitud de negocio

Estudiantes e instructores leen los avisos que la plataforma les mandó y dejan de ver los ya leídos como pendientes.

## Objetivo

Una pantalla con la bandeja de la persona, sus filtros, el detalle de cada aviso y «Marcar todas como leídas».

## Alcance

- `screens/notificaciones_screen.dart`, con las piezas del Cockpit y el texto del tema (sin colores propios):
  - Fichas de filtro: «Todas», «No leídas», «Clases y reservas», «Pagos», «Exámenes» y «Recordatorios».
  - Lista de tarjetas: tipo (con su icono), título, adelanto del mensaje de una o dos líneas, fecha relativa (`hace 5 min`, `ayer`, `04 may`) y, en los no leídos, un punto de acento y el título en negrita.
  - Carga de la página siguiente al llegar al final (con un indicador al pie), y deslizar hacia abajo para refrescar.
  - Acción «Marcar todas como leídas» en la barra superior, habilitada solo si hay no leídas (`conteoNoLeidasProvider`); al terminar, `avisar(context, 'Todos tus avisos quedaron leídos')`.
  - Estados: indicador en la primera carga; `EstadoVacio` «No tienes avisos» (o «No hay avisos con este filtro»); error de carga con «Reintentar» y, si ya había avisos, un `AvisoCockpit` de error sin quitarlos.
- `widgets/detalle_notificacion_sheet.dart`: hoja inferior con el tipo, el título, el mensaje completo y la fecha y hora. Al abrirla marca el aviso como leído; si el servicio falla, `avisar` con «No pudimos marcar el aviso como leído» y el mensaje se sigue viendo.
- `lib/core/routing/app_router.dart`: la ruta `/notificaciones`.
- Tests (`test/features/notificaciones/`, servicio falso): la lista distingue no leídos de leídos; los filtros piden lo correcto; al llegar al final carga la página siguiente; abrir un aviso muestra el mensaje completo y lo deja leído; falla de red al marcar muestra el aviso y conserva el mensaje; 404 quita el aviso; «Marcar todas» queda deshabilitado sin pendientes; bandeja vacía y con filtro vacío; error de carga con «Reintentar» y error al cargar más sin perder lo mostrado.

Archivos esperados (crear/tocar): `notificaciones_screen.dart`, `detalle_notificacion_sheet.dart`, `app_router.dart` y los tests. Los textos de fecha viven en `widgets/fecha_aviso.dart`.

## Fuera de alcance (no tocar)

- La campana y el contador en la barra superior (UT-CU38-MOB-03).
- Navegar a la clase, el pago o el examen del aviso.
- Borrar avisos o marcarlos como no leídos.
- Cambios al tema o a piezas compartidas: si falta un widget, se resuelve con el tema del Cockpit.

## Resultado esperado

En `/notificaciones` la persona ve sus avisos del más reciente al más antiguo, abre uno, lee el mensaje y al volver ya figura leído.

## Validación

1. `flutter test` y `flutter analyze` en `projects/mobile` en verde.
2. En un emulador contra el stack: enviar tres avisos a la cuenta con `POST /internal/v1/notificaciones`, abrir `/notificaciones` (tres no leídos), abrir uno (queda leído), filtrar «No leídas» (quedan dos) y «Marcar todas como leídas» (no quedan pendientes).
