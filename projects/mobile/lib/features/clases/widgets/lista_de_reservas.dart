import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../models/reserva.dart';
import '../models/vista_reservas.dart';
import '../providers/franjas_disponibles_provider.dart';
import 'formato_clases.dart';
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
        if (lista.isEmpty) {
          return Card(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [vacio, if (accionVacia != null) Padding(padding: const EdgeInsets.only(bottom: 20), child: accionVacia)],
            ),
          );
        }
        return _esHistorial ? _Historial(lista) : _ProximasPorDia(lista);
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

  const _ProximasPorDia(this.reservas);

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final ahora = DateTime.now();
    final hijos = <Widget>[];
    DateTime? diaActual;
    for (final reserva in reservas) {
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
      hijos
        ..add(ReservaCard(key: Key('reserva-${reserva.codigo}'), reserva: reserva))
        ..add(const SizedBox(height: 10));
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: hijos);
  }
}
