import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../pagos/widgets/formato_fecha.dart';
import '../../pagos/widgets/insignia_de_estado_pago.dart';
import '../models/horas_alumno_detalle.dart';
import '../providers/alumno_detalle_provider.dart';

/// El detalle de un alumno para su instructor (CU29, `/students/:codigo`): el resumen de horas, el tiempo
/// conjunto clase a clase y las compras del alumno.
///
/// * Solo lectura: ninguna acción sobre el alumno ni sobre sus pagos. El comprobante llega con CU30.
/// * Un alumno ajeno o inexistente (`ERR_PAG_12`, 404) vuelve a la lista con un aviso.
class AlumnoDetalleScreen extends ConsumerWidget {
  final String codigo;

  const AlumnoDetalleScreen({super.key, required this.codigo});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.listen(alumnoDetalleProvider(codigo), (_, nuevo) {
      final error = nuevo.error;
      if (error is ApiException && error.code == 'ERR_PAG_12') {
        avisar(context, 'Ese alumno no está entre tus alumnos.');
        if (context.canPop()) {
          context.pop();
        } else {
          context.go('/inicio');
        }
      }
    });
    final detalle = ref.watch(alumnoDetalleProvider(codigo));

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Alumno')),
      body: SafeArea(
        child: detalle.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (error, _) => ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
            children: [
              AvisoCockpit(texto: _mensajeDeError(error), tono: TonoAviso.error),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarAlumno'),
                onPressed: () => ref.invalidate(alumnoDetalleProvider(codigo)),
                child: const Text('Reintentar'),
              ),
            ],
          ),
          data: (datos) => _Contenido(detalle: datos),
        ),
      ),
    );
  }

  static String _mensajeDeError(Object error) {
    if (error is ApiException && error.code == 'ERR_PAG_15') {
      return 'No pudimos cargar los datos, inténtalo más tarde.';
    }
    if (error is DioException) {
      return 'No hay conexión. Revisa tu internet e inténtalo de nuevo.';
    }
    return 'No pudimos cargar los datos, inténtalo más tarde.';
  }
}

class _Contenido extends StatelessWidget {
  final HorasAlumnoDetalle detalle;

  const _Contenido({required this.detalle});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final resumen = detalle.resumen;

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(resumen.nombreCompleto, style: tema.textTheme.titleLarge),
                const SizedBox(height: 12),
                Row(
                  children: [
                    _Cifra('Comprado', resumen.horasCompradas),
                    _Cifra('Conmigo', resumen.horasGastadasConmigo),
                    _Cifra('Restantes', resumen.horasRestantes),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 20),
        Text('Tiempo conmigo', style: tema.textTheme.titleMedium),
        const SizedBox(height: 8),
        if (detalle.consumos.isEmpty)
          const AvisoCockpit(
            key: Key('sinClasesConmigo'),
            texto: 'Todavía no tienes clases con este alumno.',
          )
        else
          Card(
            child: Column(
              children: [
                for (final clase in detalle.consumos)
                  ListTile(
                    key: Key('clase-${clase.claseCodigo}'),
                    title: Text(formatearFechaCorta(clase.fecha)),
                    trailing: Text(
                      '${clase.horas} h',
                      style: tema.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        const SizedBox(height: 20),
        Text('Pagos', style: tema.textTheme.titleMedium),
        const SizedBox(height: 8),
        if (detalle.pagos.isEmpty)
          const AvisoCockpit(
            key: Key('sinPagosDelAlumno'),
            texto: 'Este alumno todavía no tiene compras.',
          )
        else
          Card(
            child: Column(
              children: [
                for (final pago in detalle.pagos)
                  ListTile(
                    key: Key('pago-${pago.codigo}'),
                    title: Text(pago.planNombre),
                    subtitle: Text(
                      '${pago.horas == 1 ? '1 hora' : '${pago.horas} horas'} · ${formatearFechaCorta(pago.creadoEn)}',
                    ),
                    trailing: InsigniaDeEstadoPago(pago.estado),
                  ),
              ],
            ),
          ),
      ],
    );
  }
}

class _Cifra extends StatelessWidget {
  final String etiqueta;
  final int horas;

  const _Cifra(this.etiqueta, this.horas);

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '$horas h',
            style: tema.textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          Text(
            etiqueta,
            style: tema.textTheme.bodySmall?.copyWith(
              color: tema.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}
