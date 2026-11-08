import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/transporte.dart';

/// Con qué protocolo habla la app con Notificaciones (CU38, CU37): GraphQL o REST.
///
/// * Sale de `NOTIFICACIONES_TRANSPORTE` en el `.env`; sin valor, GraphQL. Para volver a REST sin recompilar:
///   `NOTIFICACIONES_TRANSPORTE=rest`.
/// * Es un provider aparte para fijarlo en los tests.
final transporteNotificacionesProvider = Provider<Transporte>(
  (ref) => transporteDesde(dotenv.isInitialized ? dotenv.env['NOTIFICACIONES_TRANSPORTE'] : null),
);
