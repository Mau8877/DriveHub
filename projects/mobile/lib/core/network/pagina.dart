/// Una página de un listado paginado de la API.
///
/// * Vive en `core/network/` porque la usan varias features (horarios, pagos).
class Pagina<T> {
  final List<T> items;
  final int page;
  final int pageSize;
  final int totalItems;
  final int totalPages;

  const Pagina({
    required this.items,
    required this.page,
    required this.pageSize,
    required this.totalItems,
    required this.totalPages,
  });

  /// Arma la página desde `{ 'data': [...], 'meta': {...} }`, lo que entrega `ApiClient` en los listados.
  factory Pagina.fromJson(Map<String, dynamic> json, T Function(Map<String, dynamic>) desdeJson) {
    final meta = json['meta'] as Map<String, dynamic>;
    return Pagina(
      items: (json['data'] as List<dynamic>).map((e) => desdeJson(e as Map<String, dynamic>)).toList(),
      page: meta['page'] as int,
      pageSize: meta['pageSize'] as int,
      totalItems: meta['totalItems'] as int,
      totalPages: meta['totalPages'] as int,
    );
  }
}
