import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/offline/modelos_offline.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../models/reserva.dart';
import '../models/vista_reservas.dart';
import '../providers/franjas_disponibles_provider.dart';
import 'acciones_reserva.dart';
import 'dialogo_cancelar_reserva.dart';
import '../../offline/providers/offline_provider.dart';
import 'formato_clases.dart';
import 'pendientes_offline.dart';
import 'reprogramar_sheet.dart';
import 'reserva_card.dart';

/// Las clases del estudiante de una vista (CU14): «Mis clases» (agrupadas por día, de la más próxima a la más
/// lejana) o el «Historial» (de la más reciente a la más antigua, con el estado de cada una).
///
/// * Muestra su propio estado de carga, de error con «Reintentar» y de lista vacía.
/// * El orden lo pone el backend: la app no reordena.
class ListaDeReservas extends ConsumerWidget {
  final VistaReservas vista;

  /// Lo que se muestra cuando no hay clases; en «Mis clases» lleva además la acción para ir a reservar.
  final EstadoVacio vacio;
  final Widget? accionVacia;

  const ListaDeReservas({super.key, required this.vista, required this.vacio, this.accionVacia});

  bool get _esHistorial => vista == VistaReservas.historial;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final reservas = ref.watch(reservasListProvider(vista));
    final offline = ref.watch(offlineProvider);
    // * Las reservas hechas sin conexión se ven ya en «Mis clases», marcadas como pendientes
    final provisionales = _esHistorial ? const <Reserva>[] : reservasPendientes(offline);

    return reservas.when(
      loading: () => const Padding(
        padding: EdgeInsets.all(32),
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (error, _) => AvisoCockpit(
        key: Key('errorClases-${vista.valorApi}'),
        tono: TonoAviso.error,
        texto: mensajeDeError(error),
        accion: TextButton(
          onPressed: () => ref.invalidate(reservasListProvider(vista)),
          child: const Text('Reintentar'),
        ),
      ),
      data: (lista) {
        if (lista.isEmpty && provisionales.isEmpty) {
          return Card(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [vacio, if (accionVacia != null) Padding(padding: const EdgeInsets.only(bottom: 20), child: accionVacia)],
            ),
          );
        }
        return _esHistorial ? _Historial(lista) : _ProximasPorDia(lista, provisionales: provisionales, offline: offline);
      },
    );
  }
}

class _Historial extends StatelessWidget {
  final List<Reserva> reservas;

  const _Historial(this.reservas);

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (final reserva in reservas) ...[
          ReservaCard(key: Key('reserva-${reserva.codigo}'), reserva: reserva, conEstado: true),
          const SizedBox(height: 10),
        ],
      ],
    );
  }
}

class _ProximasPorDia extends StatelessWidget {
  final List<Reserva> reservas;
  final List<Reserva> provisionales;
  final EstadoOffline offline;

  const _ProximasPorDia(this.reservas, {required this.provisionales, required this.offline});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final ahora = DateTime.now();
    final hijos = <Widget>[];
    DateTime? diaActual;
    final codigosProvisionales = {for (final r in provisionales) r.codigo};
    final todas = [...reservas, ...provisionales];
    // * Solo se reordena si hay provisionales: el orden de las reales lo pone el backend
    if (provisionales.isNotEmpty) {
      todas.sort((a, b) {
        final porFecha = a.fecha.compareTo(b.fecha);
        return porFecha != 0 ? porFecha : a.horaInicio.compareTo(b.horaInicio);
      });
    }
    for (final reserva in todas) {
      final esProvisional = codigosProvisionales.contains(reserva.codigo);
      final pendiente = esProvisional ? null : offline.pendienteSobre(reserva.codigo);
      final etiqueta = esProvisional ? 'Reserva pendiente' : (pendiente == null ? null : etiquetaDePendiente(pendiente));
      if (diaActual == null || reserva.fecha != diaActual) {
        diaActual = reserva.fecha;
        hijos.add(
          Padding(
            padding: const EdgeInsets.only(top: 6, bottom: 8),
            child: Text(
              tituloDeDia(reserva.fecha, ahora),
              style: tema.textTheme.titleSmall?.copyWith(color: tema.colorScheme.onSurfaceVariant),
            ),
          ),
        );
      }
      hijos.add(ReservaCard(key: Key('reserva-${reserva.codigo}'), reserva: reserva, etiquetaPendiente: etiqueta));
      // * Con una operación pendiente encima (o sin reserva real todavía) no se ofrece cancelar ni reprogramar
      if (etiqueta == null) {
        hijos.add(
          AccionesReserva(
            reserva: reserva,
            alCancelar: () => mostrarDialogoCancelar(context, reserva),
            alReprogramar: () => mostrarReprogramar(context, reserva),
          ),
        );
      }
      hijos.add(const SizedBox(height: 10));
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: hijos);
  }
}
