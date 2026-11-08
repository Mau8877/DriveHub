import '../../../core/network/graphql_client.dart';
import '../../../core/network/pagina.dart';
import '../models/notificacion.dart';
import '../models/preferencias_notificacion.dart';
import 'documentos_graphql.dart';
import 'notificaciones_service.dart';

/// La bandeja de avisos (CU38) pedida por GraphQL (CU45): hace lo mismo que [NotificacionesService] con las mismas
/// reglas, pero hablando con `POST /graphql`.
///
/// * Es un [NotificacionesService] más: las pantallas y los providers no saben cuál de los dos usan.
/// * Un error de negocio llega como `ApiException` con su código `ERR_...`, igual que por REST.
/// * Las lecturas (`listar`, `contarNoLeidas`) quedan en la caché del modo offline; las mutaciones nunca.
class NotificacionesGraphqlService extends NotificacionesService {
  final GraphqlClient _graphql;

  NotificacionesGraphqlService({GraphqlClient? graphql}) : _graphql = graphql ?? GraphqlClient();

  @override
  Future<Pagina<Notificacion>> listar({
    int page = 1,
    int pageSize = 20,
    bool soloNoLeidas = false,
    TipoNotificacion? tipo,
  }) async {
    final datos = await _graphql.consultar(
      consultaBandeja,
      variables: {
        'page': page,
        'pageSize': pageSize,
        // * Sin filtro viaja `null`: el servicio no filtra por leída ni por tipo
        'leida': soloNoLeidas ? false : null,
        'tipo': tipo?.valorApi,
      },
    );
    final pagina = datos['notificaciones'] as Map<String, dynamic>;
    // * `Pagina.fromJson` lee `{ data, meta }`, la forma que entrega REST: se le arma igual
    return Pagina.fromJson({'data': pagina['items'], 'meta': pagina['meta']}, Notificacion.fromJson);
  }

  @override
  Future<int> contarNoLeidas() async {
    final datos = await _graphql.consultar(consultaConteoNoLeidas);
    return datos['conteoNoLeidas'] as int;
  }

  @override
  Future<Notificacion> leer(String codigoNotificacion) async {
    final datos = await _graphql.mutar(mutacionMarcarLeida, variables: {'codigo': codigoNotificacion});
    return Notificacion.fromJson(datos['marcarLeida'] as Map<String, dynamic>);
  }

  @override
  Future<int> leerTodas() async {
    final datos = await _graphql.mutar(mutacionMarcarTodasLeidas);
    return datos['marcarTodasLeidas'] as int;
  }
}
