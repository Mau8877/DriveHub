import 'fechas.dart';

/// Estado de una franja. `reservada` solo lo cambian la reserva y su cancelación (CU14, CU15).
enum EstadoFranja {
  libre('LIBRE'),
  reservada('RESERVADA'),
  bloqueada('BLOQUEADA');

  /// El valor tal como viaja en la API.
  final String valorApi;

  const EstadoFranja(this.valorApi);

  /// Un valor desconocido lanza [FormatException]: mejor fallar que mostrar un estado inventado.
  factory EstadoFranja.fromJson(String valor) {
    for (final estado in values) {
      if (estado.valorApi == valor) return estado;
    }
    throw FormatException('Estado de franja desconocido: $valor');
  }
}

/// Lo que el instructor puede hacer con una franja.
enum AccionFranja { bloquear, desbloquear }

/// Cuántas franjas hay de cada estado: el resumen que se muestra arriba de la semana.
class ResumenFranjas {
  final int libres;
  final int reservadas;
  final int bloqueadas;

  const ResumenFranjas({required this.libres, required this.reservadas, required this.bloqueadas});

  factory ResumenFranjas.de(Iterable<Franja> franjas) {
    var libres = 0;
    var reservadas = 0;
    var bloqueadas = 0;
    for (final franja in franjas) {
      switch (franja.estado) {
        case EstadoFranja.libre:
          libres++;
        case EstadoFranja.reservada:
          reservadas++;
        case EstadoFranja.bloqueada:
          bloqueadas++;
      }
    }
    return ResumenFranjas(libres: libres, reservadas: reservadas, bloqueadas: bloqueadas);
  }
}

/// Un bloque de clase reservable de un instructor (CU12).
class Franja {
  final String codigo;
  final DateTime fecha;

  /// `HH:mm`.
  final String horaInicio;
  final String horaFin;
  final EstadoFranja estado;

  const Franja({
    required this.codigo,
    required this.fecha,
    required this.horaInicio,
    required this.horaFin,
    required this.estado,
  });

  factory Franja.fromJson(Map<String, dynamic> json) {
    return Franja(
      codigo: json['codigo'] as String,
      fecha: fechaDesdeApi(json['fecha'] as String),
      horaInicio: json['horaInicio'] as String,
      horaFin: json['horaFin'] as String,
      estado: EstadoFranja.fromJson(json['estado'] as String),
    );
  }

  /// La franja ya empezó o ya pasó (el inicio es igual o anterior a [ahora]).
  ///
  /// * Es solo para la interfaz: el backend es quien rechaza con `ERR_ACA_10`.
  bool yaEmpezo(DateTime ahora) {
    final partes = horaInicio.split(':');
    final inicio = DateTime(fecha.year, fecha.month, fecha.day, int.parse(partes[0]), int.parse(partes[1]));
    return !inicio.isAfter(ahora);
  }

  /// La acción que se ofrece: bloquear una libre, desbloquear una bloqueada; ninguna si está
  /// reservada o si ya empezó.
  AccionFranja? accionDisponible(DateTime ahora) {
    if (yaEmpezo(ahora)) return null;
    switch (estado) {
      case EstadoFranja.libre:
        return AccionFranja.bloquear;
      case EstadoFranja.bloqueada:
        return AccionFranja.desbloquear;
      case EstadoFranja.reservada:
        return null;
    }
  }
}
