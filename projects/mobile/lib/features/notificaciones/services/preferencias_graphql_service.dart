import '../../../core/network/graphql_client.dart';
import '../models/preferencias_notificacion.dart';
import 'documentos_graphql.dart';
import 'preferencias_service.dart';

/// Las preferencias de aviso (CU37) pedidas por GraphQL (CU45): hacen lo mismo que [PreferenciasService] con las
/// mismas reglas, pero hablando con `POST /graphql`.
///
/// * Es un [PreferenciasService] más: la pantalla y el provider no saben cuál de los dos usan.
/// * Guardar reemplaza la configuración completa, como el `PUT` de REST: lo que se omite se vacía.
class PreferenciasGraphqlService extends PreferenciasService {
  final GraphqlClient _graphql;

  PreferenciasGraphqlService({GraphqlClient? graphql}) : _graphql = graphql ?? GraphqlClient();

  @override
  Future<PreferenciasNotificacion> obtener() async {
    final datos = await _graphql.consultar(consultaPreferencias);
    return PreferenciasNotificacion.fromJson(datos['preferencias'] as Map<String, dynamic>);
  }

  @override
  Future<PreferenciasNotificacion> guardar(
    PreferenciasNotificacion preferencias, {
    required String email,
  }) async {
    final datos = await _graphql.mutar(
      mutacionGuardarPreferencias,
      // * El mismo cuerpo que el PUT: el `PreferenciasInput` del servicio tiene la forma de `toJson`
      variables: {'entrada': preferencias.toJson(email: email)},
    );
    return PreferenciasNotificacion.fromJson(datos['guardarPreferencias'] as Map<String, dynamic>);
  }
}
