import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../models/franja_disponible.dart';
import '../models/franja_rechazada.dart';
import '../models/reservas_resultado.dart';
import '../providers/clases_pestana_provider.dart';
import '../providers/franjas_seleccionadas_provider.dart';
import 'formato_clases.dart';
import 'reserva_card.dart';

/// Abre el resultado de confirmar (CU14): qué quedó reservado y qué no, con el motivo de cada una.
///
/// * [elegidas] son las franjas que se mandaron: sirven para dejar elegidas de nuevo las que fallaron por un corte de
/// * Pagos y ofrecer «Reintentar».
Future<void> mostrarResultadoReserva(
  BuildContext context,
  ReservasResultado resultado,
  Map<String, FranjaDisponible> elegidas,
) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    builder: (_) => ResultadoReservaSheet(resultado: resultado, elegidas: elegidas),
  );
}

/// El resultado de confirmar varias clases a la vez.
///
/// * Las reservadas van arriba; las que no se pudieron, abajo, con su fecha, hora, instructor y el motivo en español
/// * (el que manda el backend). Si ninguna se reservó, se dice que no se gastó ninguna hora.
/// * Las rechazadas por `ERR_ACA_17` (Pagos no respondió) se pueden reintentar: se vuelven a elegir.
class ResultadoReservaSheet extends ConsumerWidget {
  final ReservasResultado resultado;
  final Map<String, FranjaDisponible> elegidas;

  const ResultadoReservaSheet({super.key, required this.resultado, required this.elegidas});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final tema = Theme.of(context);
    final reintentables = [
      for (final r in resultado.rechazadas)
        if (r.sePuedeReintentar && elegidas.containsKey(r.franjaCodigo)) elegidas[r.franjaCodigo]!,
    ];

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Flexible(
              child: ListView(
                shrinkWrap: true,
                children: [
                  if (resultado.reservadas.isNotEmpty) ...[
                    Text(
                      'Clases reservadas (${resultado.reservadas.length})',
                      key: const Key('tituloReservadas'),
                      style: tema.textTheme.titleLarge,
                    ),
                    const SizedBox(height: 12),
                    for (final reserva in resultado.reservadas) ...[
                      ReservaCard(key: Key('reservada-${reserva.codigo}'), reserva: reserva),
                      const SizedBox(height: 10),
                    ],
                  ],
                  if (resultado.rechazadas.isNotEmpty) ...[
                    const SizedBox(height: 6),
                    Text(
                      'No se pudieron reservar (${resultado.rechazadas.length})',
                      key: const Key('tituloRechazadas'),
                      style: tema.textTheme.titleLarge,
                    ),
                    const SizedBox(height: 12),
                    if (resultado.ningunaReservada) ...[
                      const AvisoCockpit(
                        key: Key('ningunaHoraGastada'),
                        tono: TonoAviso.info,
                        texto: 'No se descontó ninguna hora.',
                      ),
                      const SizedBox(height: 10),
                    ],
                    for (final rechazada in resultado.rechazadas) ...[
                      _Rechazada(rechazada),
                      const SizedBox(height: 10),
                    ],
                  ],
                  if (resultado.horasDisponibles != null) ...[
                    const SizedBox(height: 4),
                    Text(
                      'Te quedan ${resultado.horasDisponibles} h',
                      key: const Key('horasRestantes'),
                      style: tema.textTheme.titleSmall,
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 16),
            if (reintentables.isNotEmpty) ...[
              OutlinedButton(
                key: const Key('reintentar'),
                onPressed: () {
                  final seleccion = ref.read(franjasSeleccionadasUiProvider.notifier);
                  final horas = resultado.horasDisponibles ?? reintentables.length;
                  for (final franja in reintentables) {
                    seleccion.alternar(franja, horasDisponibles: horas);
                  }
                  Navigator.of(context).pop();
                },
                child: const Text('Reintentar las que fallaron'),
              ),
              const SizedBox(height: 10),
            ],
            SizedBox(
              width: double.infinity,
              child: resultado.reservadas.isNotEmpty
                  ? FilledButton(
                      key: const Key('verMisClases'),
                      onPressed: () {
                        ref.read(clasesPestanaUiProvider.notifier).elegir(PestanaClases.misClases);
                        Navigator.of(context).pop();
                      },
                      child: const Text('Ver mis clases'),
                    )
                  : FilledButton(
                      key: const Key('entendido'),
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text('Entendido'),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Rechazada extends StatelessWidget {
  final FranjaRechazada rechazada;

  const _Rechazada(this.rechazada);

  @override
  Widget build(BuildContext context) {
    final cuando = _cuando();
    final motivo = rechazada.laTomoOtro ? 'Otro estudiante ya la reservó.' : rechazada.mensaje;

    return AvisoCockpit(
      key: Key('rechazada-${rechazada.franjaCodigo}'),
      tono: TonoAviso.advertencia,
      texto: cuando == null ? motivo : '$cuando\n$motivo',
    );
  }

  /// «Mañana · 11:00 – 12:00 · Ivan Alfa», o nada si la franja ya no existe para el estudiante.
  String? _cuando() {
    final fecha = rechazada.fecha;
    final inicio = rechazada.horaInicio;
    final fin = rechazada.horaFin;
    if (fecha == null || inicio == null || fin == null) return null;
    final partes = [
      tituloDeDia(fecha, DateTime.now()),
      rangoDeHoras(inicio, fin),
      if (rechazada.instructorNombre != null) rechazada.instructorNombre!,
    ];
    return partes.join(' · ');
  }
}
