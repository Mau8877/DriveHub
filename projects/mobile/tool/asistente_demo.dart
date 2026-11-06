// * Demostración del asistente de voz de clases (CU50) SIN backend: abre la sección «Clases» real, con el asistente y los
// * modelos reales del teléfono, pero con clases, horarios e instructores de ejemplo guardados en memoria.
// *
// * No es parte de la app: sirve para probar el asistente hablándole mientras no hay backend disponible.
// *   flutter build apk --debug -t tool/asistente_demo.dart
// * Necesita los modelos en `<documentos>/ia/` (ver las UT del CU50). Cancelar, reprogramar y reservar cambian los
// * datos de ejemplo (y el saldo de horas), así se ve el resultado completo; todo se reinicia al cerrar la app.
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/core/network/pagina.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/clases/models/estado_reserva.dart';
import 'package:mobile/features/clases/models/franja_disponible.dart';
import 'package:mobile/features/clases/models/hora_de_cancelacion.dart';
import 'package:mobile/features/clases/models/instructor_disponible.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';
import 'package:mobile/features/clases/models/reserva.dart';
import 'package:mobile/features/clases/models/reservas_resultado.dart';
import 'package:mobile/features/clases/models/resultado_cancelacion.dart';
import 'package:mobile/features/clases/models/vista_reservas.dart';
import 'package:mobile/features/clases/providers/clases_service_provider.dart';
import 'package:mobile/features/clases/screens/clases_screen.dart';
import 'package:mobile/features/clases/services/clases_service.dart';
import 'package:mobile/features/horarios/models/fechas.dart';
import 'package:mobile/features/pagos/providers/pagos_service_provider.dart';
import 'package:mobile/features/pagos/services/pagos_service.dart';

import '../test/helpers/sesion_falsa.dart';

const _instructores = [
  InstructorDisponible(codigo: 'i1', nombre: 'Lucía Rojas'),
  InstructorDisponible(codigo: 'i2', nombre: 'Carlos Quispe'),
  InstructorDisponible(codigo: 'i3', nombre: 'José Pérez'),
];

/// Los datos de ejemplo, compartidos por los dos servicios falsos.
class DatosDeEjemplo {
  int saldo = 5;
  final List<Reserva> proximas = [];
  final List<Reserva> historial = [];

  DatosDeEjemplo() {
    final hoy = soloFecha(DateTime.now());
    proximas.addAll([
      _reserva(sumarDias(hoy, 1), 10, 'i1'),
      _reserva(sumarDias(hoy, 3), 15, 'i2'),
      _reserva(sumarDias(hoy, 3), 9, 'i1'),
    ]);
    historial.addAll([
      _reserva(sumarDias(hoy, -7), 10, 'i1', estado: EstadoReserva.completada),
      _reserva(sumarDias(hoy, -14), 16, 'i3', estado: EstadoReserva.completada),
      _reserva(sumarDias(hoy, -20), 11, 'i2', estado: EstadoReserva.cancelada),
    ]);
  }

  static String _dos(int n) => n.toString().padLeft(2, '0');

  static Reserva _reserva(DateTime fecha, int hora, String instructor, {EstadoReserva estado = EstadoReserva.confirmada}) {
    final inicio = DateTime(fecha.year, fecha.month, fecha.day, hora);
    final i = _instructores.firstWhere((x) => x.codigo == instructor);
    return Reserva(
      codigo: 'r-${fechaParaApi(fecha)}-$hora-$instructor',
      fecha: fecha,
      horaInicio: '${_dos(hora)}:00',
      horaFin: '${_dos(hora + 1)}:00',
      instructorCodigo: instructor,
      instructorNombre: i.nombre,
      estado: estado,
      terminada: estado != EstadoReserva.confirmada,
      // * El plazo de CU15: con 12 horas o más de anticipación se devuelve la hora y se puede reprogramar
      cancelacionATiempo: inicio.difference(DateTime.now()) >= const Duration(hours: 12),
    );
  }

  /// Todas las franjas posibles de un día: de 08:00 a 17:00 con cada instructor, menos las ya reservadas.
  List<FranjaDisponible> franjasDe(DateTime fecha) {
    final dia = soloFecha(fecha);
    return [
      for (final i in _instructores)
        for (var h = 8; h <= 17; h++)
          if (!proximas.any((r) => soloFecha(r.fecha) == dia && r.horaInicio == '${_dos(h)}:00' && r.instructorCodigo == i.codigo))
            FranjaDisponible(
              codigo: 'f|${fechaParaApi(dia)}|$h|${i.codigo}',
              fecha: dia,
              horaInicio: '${_dos(h)}:00',
              horaFin: '${_dos(h + 1)}:00',
              instructorCodigo: i.codigo,
              instructorNombre: i.nombre,
            ),
    ];
  }

  /// La reserva que corresponde al código de una franja de [franjasDe].
  Reserva reservaDeFranja(String codigoFranja) {
    final partes = codigoFranja.split('|');
    return _reserva(fechaDesdeApi(partes[1]), int.parse(partes[2]), partes[3]);
  }
}

class ClasesDemo extends ClasesService {
  final DatosDeEjemplo datos;

  ClasesDemo(this.datos) : super(dio: Dio());

  static Pagina<T> _pagina<T>(List<T> items) =>
      Pagina(items: items, page: 1, pageSize: 100, totalItems: items.length, totalPages: 1);

  @override
  Future<Pagina<FranjaDisponible>> franjasDisponibles({
    DateTime? fecha,
    MomentoDia momento = MomentoDia.todos,
    String? instructorCodigo,
    int page = 1,
    int pageSize = 100,
  }) async {
    final desde = momento.horaDesde, hasta = momento.horaHasta;
    final franjas = datos.franjasDe(fecha ?? DateTime.now()).where((f) {
      if (instructorCodigo != null && f.instructorCodigo != instructorCodigo) return false;
      if (desde != null && f.horaInicio.compareTo(desde) < 0) return false;
      if (hasta != null && f.horaInicio.compareTo(hasta) >= 0) return false;
      return true;
    }).toList();
    return _pagina(franjas);
  }

  @override
  Future<List<InstructorDisponible>> instructoresDisponibles({DateTime? fecha}) async => _instructores;

  @override
  Future<Pagina<Reserva>> reservas({required VistaReservas vista, int page = 1, int pageSize = 50}) async {
    final lista = vista == VistaReservas.proximas ? datos.proximas : datos.historial;
    return _pagina(List.of(lista));
  }

  @override
  Future<ReservasResultado> reservar(List<String> franjaCodigos) async {
    final nuevas = [for (final c in franjaCodigos) datos.reservaDeFranja(c)];
    datos.proximas
      ..addAll(nuevas)
      ..sort((a, b) {
        final f = a.fecha.compareTo(b.fecha);
        return f != 0 ? f : a.horaInicio.compareTo(b.horaInicio);
      });
    datos.saldo -= nuevas.length;
    return ReservasResultado(reservadas: nuevas, rechazadas: const [], horasDisponibles: datos.saldo);
  }

  @override
  Future<ResultadoCancelacion> cancelar(String codigoReserva) async {
    final r = datos.proximas.firstWhere((x) => x.codigo == codigoReserva);
    datos.proximas.remove(r);
    final devuelta = r.cancelacionATiempo;
    if (devuelta) datos.saldo += 1;
    return ResultadoCancelacion(
      reserva: r,
      horaDevuelta: devuelta ? HoraDeCancelacion.devuelta : HoraDeCancelacion.perdida,
      horasDisponibles: datos.saldo,
    );
  }

  @override
  Future<Reserva> reprogramar(String codigoReserva, String franjaCodigo) async {
    final vieja = datos.proximas.firstWhere((x) => x.codigo == codigoReserva);
    final nueva = datos.reservaDeFranja(franjaCodigo);
    datos.proximas
      ..remove(vieja)
      ..add(nueva)
      ..sort((a, b) {
        final f = a.fecha.compareTo(b.fecha);
        return f != 0 ? f : a.horaInicio.compareTo(b.horaInicio);
      });
    return nueva;
  }
}

class PagosDemo extends PagosService {
  final DatosDeEjemplo datos;

  PagosDemo(this.datos) : super(dio: Dio());

  @override
  Future<int> saldoDeHoras() async => datos.saldo;
}

void main() {
  final datos = DatosDeEjemplo();
  runApp(
    ProviderScope(
      overrides: [
        clasesServiceProvider.overrideWithValue(ClasesDemo(datos)),
        pagosServiceProvider.overrideWithValue(PagosDemo(datos)),
        sesionFalsa(
          SesionFalsa(
            EstadoSesion.conSesion(
              const UsuarioSesion(codigo: 'u1', nombres: 'Ana', apellidos: 'Pérez', roles: ['ESTUDIANTE']),
            ),
          ),
        ),
      ],
      child: MaterialApp(
        title: 'Demo del asistente',
        theme: AppTheme.light,
        darkTheme: AppTheme.dark,
        home: Scaffold(
          appBar: AppBar(title: const Text('Demo del asistente (datos de ejemplo)')),
          body: const ClasesScreen(),
        ),
      ),
    ),
  );
}
