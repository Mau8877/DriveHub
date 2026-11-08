/// Con qué protocolo habla la app con un servicio que ofrece las dos formas (REST y GraphQL, CU45).
enum Transporte { rest, graphql }

/// El transporte que pide una variable del `.env` (por ejemplo `NOTIFICACIONES_TRANSPORTE`).
///
/// * Sin valor o con un valor que no se reconoce se usa GraphQL. `rest` vuelve a las rutas de siempre sin recompilar,
///   por si el backend no tiene GraphQL encendido (`GRAPHQL_ENABLED=false`) o algo falla.
Transporte transporteDesde(String? valor) =>
    valor?.trim().toLowerCase() == 'rest' ? Transporte.rest : Transporte.graphql;
