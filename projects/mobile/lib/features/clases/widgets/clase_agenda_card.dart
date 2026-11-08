import 'package:flutter/material.dart';

import '../../../shared/widgets/insignia.dart';
import '../models/clase_agenda.dart';
import '../models/estado_reserva.dart';
import 'formato_clases.dart';

/// Una clase de la agenda del instructor (CU16): la hora, el estudiante y el estado.
///
/// * Los colores salen del tema; el estado va siempre en texto y el color solo lo refuerza.
/// * Una clase cancelada se ve atenuada con el color secundario del tema, no con opacidad: sigue legible.
/// * Cada acción se ofrece solo si el servidor lo dice y la pantalla le pasó qué hacer; no usa Riverpod, así se prueba
///   sin montar nada más. «Cancelar» si la clase es [ClaseAgenda.cancelable] ([alCancelar]), «Cerrar clase» si es
///   [ClaseAgenda.cerrable] ([alCerrar], CU17) y «Editar» si ya se cerró y trae su [ClaseAgenda.claseCodigo]
///   ([alEditar], CU17). Una clase tiene a lo sumo una de las tres. Una clase cerrada con asistencia puede ofrecer además
///   «Evaluar» si es [ClaseAgenda.evaluable] ([alEvaluar], CU18) y «Ver prueba» si [ClaseAgenda.conPrueba]
///   ([alVerPrueba], CU18); las acciones de una clase cerrada se acomodan en varias líneas si no caben.
class ClaseAgendaCard extends StatelessWidget {
  final ClaseAgenda clase;
  final VoidCallback? alCancelar;
  final VoidCallback? alCerrar;
  final VoidCallback? alEditar;
  final VoidCallback? alEvaluar;
  final VoidCallback? alVerPrueba;

  /// Si la clase tiene una operación hecha sin conexión sin enviar («Cierre pendiente»), su texto: se muestra y se
  /// apagan las acciones que chocarían con ella (no se cancela dos veces, no se evalúa una clase por cerrar).
  final String? etiquetaPendiente;

  const ClaseAgendaCard({
    super.key,
    required this.clase,
    this.alCancelar,
    this.alCerrar,
    this.alEditar,
    this.alEvaluar,
    this.alVerPrueba,
    this.etiquetaPendiente,
  });

  bool get _cancelada => clase.estado == EstadoReserva.cancelada;

  bool get _libre => etiquetaPendiente == null;
  bool get _puedeEditar => _libre && clase.claseCodigo != null && alEditar != null;
  bool get _puedeEvaluar => _libre && clase.evaluable && alEvaluar != null;
  bool get _puedeVerPrueba => clase.conPrueba && alVerPrueba != null;

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colorSecundario = tema.colorScheme.onSurfaceVariant;

    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    rangoDeHoras(clase.horaInicio, clase.horaFin),
                    style: tema.textTheme.titleMedium?.copyWith(color: _cancelada ? colorSecundario : null),
                  ),
                ),
                const SizedBox(width: 8),
                Insignia(etiquetaDeEstadoAgenda(clase), tono: _tono(clase)),
              ],
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                Icon(Icons.person_outline, size: 18, color: colorSecundario),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    clase.estudianteNombre,
                    style: tema.textTheme.bodyMedium?.copyWith(color: _cancelada ? colorSecundario : null),
                  ),
                ),
              ],
            ),
            if (etiquetaPendiente != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Insignia(etiquetaPendiente!, tono: TonoInsignia.advertencia),
              ),
            if (_libre && clase.cancelable && alCancelar != null)
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  key: Key('cancelar-${clase.codigo}'),
                  onPressed: alCancelar,
                  style: TextButton.styleFrom(foregroundColor: tema.colorScheme.error),
                  child: const Text('Cancelar'),
                ),
              ),
            if (_libre && clase.cerrable && alCerrar != null)
              Align(
                alignment: Alignment.centerRight,
                child: FilledButton.tonal(
                  key: Key('cerrar-${clase.codigo}'),
                  onPressed: alCerrar,
                  style: FilledButton.styleFrom(minimumSize: const Size(0, 40)),
                  child: const Text('Cerrar clase'),
                ),
              ),
            if (_puedeEditar || _puedeVerPrueba || _puedeEvaluar)
              Align(
                alignment: Alignment.centerRight,
                child: Wrap(
                  alignment: WrapAlignment.end,
                  spacing: 4,
                  children: [
                    if (_puedeEditar)
                      TextButton(key: Key('editar-${clase.codigo}'), onPressed: alEditar, child: const Text('Editar')),
                    if (_puedeVerPrueba)
                      TextButton(
                        key: Key('ver-prueba-${clase.codigo}'),
                        onPressed: alVerPrueba,
                        child: const Text('Ver prueba'),
                      ),
                    if (_puedeEvaluar)
                      FilledButton.tonal(
                        key: Key('evaluar-${clase.codigo}'),
                        onPressed: alEvaluar,
                        style: FilledButton.styleFrom(minimumSize: const Size(0, 40)),
                        child: const Text('Evaluar'),
                      ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  static TonoInsignia _tono(ClaseAgenda clase) {
    switch (clase.estado) {
      case EstadoReserva.completada:
        return TonoInsignia.exito;
      case EstadoReserva.noAsistio:
        return TonoInsignia.advertencia;
      case EstadoReserva.cancelada:
        return TonoInsignia.neutro;
      case EstadoReserva.confirmada:
        return clase.terminada ? TonoInsignia.advertencia : TonoInsignia.info;
    }
  }
}
