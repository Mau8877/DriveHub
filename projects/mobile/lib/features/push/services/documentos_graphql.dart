// * Los documentos de GraphQL para registrar y quitar el dispositivo push (CU36, CU45).
// ! Tienen que ser válidos contra el esquema del servicio `notificaciones`
// ! (`projects/backend/notificaciones/src/features/dispositivos/graphql/`).

/// Registra el token de Firebase de este teléfono; si ya estaba, el servicio lo reactiva.
const String mutacionRegistrarDispositivo = r'''
mutation RegistrarDispositivo($entrada: RegistrarDispositivoInput!) {
  registrarDispositivo(entrada: $entrada) { codigoDispositivoPush plataforma activo }
}''';

/// Quita un dispositivo de la persona.
const String mutacionQuitarDispositivo = r'''
mutation QuitarDispositivo($codigo: ID!) {
  quitarDispositivo(codigo: $codigo)
}''';
