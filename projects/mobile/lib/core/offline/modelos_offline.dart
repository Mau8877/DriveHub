/// Una acción que la persona hizo sin internet y que se envía sola cuando vuelve la conexión.
///
/// * Guarda la petición tal cual iba a salir (método, ruta, cuerpo) y un texto corto para mostrarla.
/// * El orden de la cola es el orden en que se hicieron: se envían de a una, en ese orden.
class AccionPendiente {
  final String id;
  final String metodo;
  final String ruta;
  final Object? cuerpo;
  final String descripcion;
  final DateTime creadaEn;

  /// Qué clase de acción es, para las que el servidor responde 200 aunque haya rechazado una parte (reservar varias
  /// franjas): el núcleo revisa su respuesta con el verificador de este tipo. `null` = basta con el status HTTP.
  final String? tipo;

  /// Qué hace la acción («cancelar», «reservar»...) y sobre qué códigos (la reserva, la franja...): con esto las
  /// pantallas muestran lo hecho sin conexión como «pendiente» (actualización optimista) en vez de dejar la lista
  /// como si nada hubiera pasado. Si el servidor la rechaza, sale de la cola y la lista vuelve sola a lo real.
  final String? efecto;
  final List<String> objetivos;

  /// Lo que hace falta para dibujar la acción como pendiente (por ejemplo, las franjas de una reserva). JSON.
  final Map<String, dynamic>? datos;

  const AccionPendiente({
    required this.id,
    required this.metodo,
    required this.ruta,
    required this.descripcion,
    required this.creadaEn,
    this.cuerpo,
    this.tipo,
    this.efecto,
    this.objetivos = const [],
    this.datos,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'metodo': metodo,
    'ruta': ruta,
    'cuerpo': cuerpo,
    'descripcion': descripcion,
    'creadaEn': creadaEn.toIso8601String(),
    'tipo': tipo,
    'efecto': efecto,
    'objetivos': objetivos,
    'datos': datos,
  };

  factory AccionPendiente.fromJson(Map<String, dynamic> json) => AccionPendiente(
    id: json['id'] as String,
    metodo: json['metodo'] as String,
    ruta: json['ruta'] as String,
    cuerpo: json['cuerpo'],
    descripcion: json['descripcion'] as String,
    creadaEn: DateTime.parse(json['creadaEn'] as String),
    tipo: json['tipo'] as String?,
    efecto: json['efecto'] as String?,
    objetivos: [for (final o in (json['objetivos'] as List<dynamic>? ?? const [])) o as String],
    datos: json['datos'] == null ? null : Map<String, dynamic>.from(json['datos'] as Map),
  );
}

/// Una acción pendiente que el servidor no aceptó al volver internet (la franja ya se ocupó, la clase cambió...).
/// Se descartó de la cola; queda registrada para avisarle a la persona hasta que la cierre.
class AccionRechazada {
  final String id;
  final String descripcion;
  final String motivo;
  final String? codigo;
  final DateTime rechazadaEn;

  const AccionRechazada({
    required this.id,
    required this.descripcion,
    required this.motivo,
    required this.rechazadaEn,
    this.codigo,
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'descripcion': descripcion,
    'motivo': motivo,
    'codigo': codigo,
    'rechazadaEn': rechazadaEn.toIso8601String(),
  };

  factory AccionRechazada.fromJson(Map<String, dynamic> json) => AccionRechazada(
    id: json['id'] as String,
    descripcion: json['descripcion'] as String,
    motivo: json['motivo'] as String,
    codigo: json['codigo'] as String?,
    rechazadaEn: DateTime.parse(json['rechazadaEn'] as String),
  );
}

/// Una acción pendiente que el servidor sí aceptó al volver internet: se le avisa a la persona que se cumplió.
class AccionCompletada {
  final String id;
  final String descripcion;
  final DateTime completadaEn;

  const AccionCompletada({required this.id, required this.descripcion, required this.completadaEn});

  Map<String, dynamic> toJson() => {
    'id': id,
    'descripcion': descripcion,
    'completadaEn': completadaEn.toIso8601String(),
  };

  factory AccionCompletada.fromJson(Map<String, dynamic> json) => AccionCompletada(
    id: json['id'] as String,
    descripcion: json['descripcion'] as String,
    completadaEn: DateTime.parse(json['completadaEn'] as String),
  );
}

/// Una parte de una acción que el servidor aceptó con un 200 pero no cumplió (una franja de varias reservadas).
class ObservacionRechazo {
  final String detalle;
  final String? codigo;

  const ObservacionRechazo(this.detalle, {this.codigo});
}

/// Revisa la respuesta (ya desempaquetada) de una acción enviada desde la cola y devuelve lo que el servidor no
/// cumplió. Una lista vacía = todo se cumplió.
typedef VerificadorDeRespuesta = List<ObservacionRechazo> Function(Object? respuesta);

/// Lo que la interfaz necesita saber del modo offline.
class EstadoOffline {
  /// La última petición no pudo salir (no hay red o el servidor no responde): se muestran datos guardados.
  final bool sinConexion;
  final List<AccionPendiente> pendientes;
  final List<AccionRechazada> rechazadas;
  final List<AccionCompletada> completadas;
  final bool sincronizando;

  const EstadoOffline({
    this.sinConexion = false,
    this.pendientes = const [],
    this.rechazadas = const [],
    this.completadas = const [],
    this.sincronizando = false,
  });

  EstadoOffline copiar({
    bool? sinConexion,
    List<AccionPendiente>? pendientes,
    List<AccionRechazada>? rechazadas,
    List<AccionCompletada>? completadas,
    bool? sincronizando,
  }) => EstadoOffline(
    sinConexion: sinConexion ?? this.sinConexion,
    pendientes: pendientes ?? this.pendientes,
    rechazadas: rechazadas ?? this.rechazadas,
    completadas: completadas ?? this.completadas,
    sincronizando: sincronizando ?? this.sincronizando,
  );

  /// La acción pendiente más reciente que toca a [codigo] (una reserva, una franja, una clase), o `null`.
  AccionPendiente? pendienteSobre(String codigo) {
    for (final a in pendientes.reversed) {
      if (a.objetivos.contains(codigo)) return a;
    }
    return null;
  }

  /// Las acciones pendientes de un [efecto], en el orden en que se hicieron.
  List<AccionPendiente> pendientesConEfecto(String efecto) => [
    for (final a in pendientes)
      if (a.efecto == efecto) a,
  ];

  /// Hay un resultado que mostrarle a la persona: lo que se cumplió y lo que no al volver internet.
  bool get hayResultados => rechazadas.isNotEmpty || completadas.isNotEmpty;

  /// Hay algo que decirle a la persona (sin red, pendientes o resultados).
  bool get hayAviso => sinConexion || pendientes.isNotEmpty || hayResultados;
}

/// Se lanza desde un `service` cuando una acción no pudo enviarse por falta de conexión y quedó guardada para
/// enviarse sola al volver internet. No es un error de la persona ni del servidor: la pantalla lo muestra como
/// «guardado, se enviará cuando haya conexión».
class AccionEncolada implements Exception {
  final String descripcion;

  const AccionEncolada(this.descripcion);

  @override
  String toString() => 'AccionEncolada($descripcion)';
}
