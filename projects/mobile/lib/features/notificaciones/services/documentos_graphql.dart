// * Los documentos de GraphQL de Notificaciones (CU45): lo que la app le pide al servicio. Cada uno pide solo los
// * campos que los modelos (`Notificacion`, `PreferenciasNotificacion`) leen.
// ! Tienen que ser válidos contra el esquema del servicio `notificaciones` (resolvers en
// ! `projects/backend/notificaciones/src/features/*/graphql/`): si se cambia un campo allá, se cambia acá.

/// La bandeja de avisos, paginada; `leida` y `tipo` en `null` no filtran.
const String consultaBandeja = r'''
query Bandeja($page: Int!, $pageSize: Int!, $leida: Boolean, $tipo: TipoNotificacion) {
  notificaciones(page: $page, pageSize: $pageSize, leida: $leida, tipo: $tipo) {
    items { codigoNotificacion tipo titulo mensaje leida creadaEn }
    meta { page pageSize totalItems totalPages }
  }
}''';

/// Cuántos avisos sin leer tiene la persona.
const String consultaConteoNoLeidas = r'''
query ConteoNoLeidas {
  conteoNoLeidas
}''';

/// Marca un aviso como leído y lo devuelve.
const String mutacionMarcarLeida = r'''
mutation MarcarLeida($codigo: ID!) {
  marcarLeida(codigo: $codigo) { codigoNotificacion tipo titulo mensaje leida creadaEn }
}''';

/// Marca todos los avisos como leídos y devuelve cuántos eran.
const String mutacionMarcarTodasLeidas = r'''
mutation MarcarTodasLeidas {
  marcarTodasLeidas
}''';

/// Las preferencias guardadas, o las de por defecto si la persona nunca configuró.
const String consultaPreferencias = r'''
query Preferencias {
  preferencias {
    canales { email push inApp }
    tipos { RESERVA PAGO EXAMEN RECORDATORIO }
    silencioDesde
    silencioHasta
    email
    actualizadoEn
  }
}''';

/// Guarda la configuración completa y devuelve lo guardado.
const String mutacionGuardarPreferencias = r'''
mutation GuardarPreferencias($entrada: PreferenciasInput!) {
  guardarPreferencias(entrada: $entrada) {
    canales { email push inApp }
    tipos { RESERVA PAGO EXAMEN RECORDATORIO }
    silencioDesde
    silencioHasta
    email
    actualizadoEn
  }
}''';
