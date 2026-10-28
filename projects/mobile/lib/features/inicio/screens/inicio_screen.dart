import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/fila_accion.dart';
import '../../auth/providers/sesion_provider.dart';
import '../../clases/models/reserva.dart';
import '../../clases/models/vista_reservas.dart';
import '../../clases/providers/franjas_disponibles_provider.dart';
import '../../clases/widgets/formato_clases.dart';
import '../../clases/widgets/reserva_card.dart';

/// Cuántas clases se muestran en Inicio: la próxima en la tarjeta y las siguientes debajo.
const _maxClasesInicio = 3;

/// Inicio de la app (pestaña "Inicio" del `UsuarioShell`): saludo, estado de la cuenta, la tarjeta
/// de la próxima clase y los accesos rápidos.
///
/// * La próxima clase y las siguientes del estudiante son las mismas que ve en «Mis clases» (CU14).
/// * La agenda del instructor, la reserva, los simulacros, los manuales y el asistente son de otros
/// * casos de uso: hasta que existan, los accesos avisan que llegan con su caso de uso (igual que el mockup).
/// ? ¿Sumar el progreso del curso y la próxima cuota cuando existan Académico y Pagos en mobile?
class InicioScreen extends ConsumerWidget {
  const InicioScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final usuario = ref.watch(sesionProvider).usuario;
    final esInstructor = usuario?.roles.contains('INSTRUCTOR') ?? false;
    final tema = Theme.of(context);
    final colores = context.cockpit;
    // * El instructor no reserva clases: su agenda es de CU16
    final proximas = esInstructor
        ? null
        : ref.watch(reservasListProvider(VistaReservas.proximas));
    final clases =
        proximas?.asData?.value
            .where((r) => !r.terminada)
            .take(_maxClasesInicio)
            .toList() ??
        const <Reserva>[];

    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
      children: [
        Text(
          usuario == null ? 'Hola' : 'Hola, ${usuario.nombres}',
          style: tema.textTheme.headlineSmall,
        ),
        const SizedBox(height: 12),
        SizedBox(
          height: 34,
          child: ListView(
            scrollDirection: Axis.horizontal,
            children: [
              _Chip(
                icono: Icons.verified_user_outlined,
                texto: esInstructor ? 'Instructor' : 'Estudiante',
              ),
              const SizedBox(width: 8),
              _Chip(
                icono: usuario?.correoVerificado == true
                    ? Icons.check_circle_outline
                    : Icons.mail_outline,
                texto: usuario?.correoVerificado == true
                    ? 'Correo verificado'
                    : 'Correo sin verificar',
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        _TarjetaProximaClase(
          esInstructor: esInstructor,
          proxima: clases.isEmpty ? null : clases.first,
          cargando: proximas?.isLoading ?? false,
          error: proximas?.hasError ?? false,
        ),
        if (clases.length > 1) ...[
          const SizedBox(height: 22),
          Text(
            'SIGUIENTES CLASES',
            style: tema.textTheme.labelSmall?.copyWith(
              fontWeight: FontWeight.w800,
              letterSpacing: 1.4,
              color: tema.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 10),
          for (final reserva in clases.skip(1)) ...[
            ReservaCard(
              key: Key('inicio-reserva-${reserva.codigo}'),
              reserva: reserva,
            ),
            const SizedBox(height: 10),
          ],
        ],
        if (esInstructor) ...[
          const SizedBox(height: 22),
          Card(
            child: FilaAccion(
              icono: Icons.video_library_outlined,
              titulo: 'Tutoriales',
              detalle: 'Crea los tuyos y mira los de otros instructores',
              onTap: () => context.push('/tutorials'),
            ),
          ),
        ],
        if (!esInstructor) ...[
          const SizedBox(height: 22),
          Text(
            'ACCESOS RÁPIDOS',
            style: tema.textTheme.labelSmall?.copyWith(
              fontWeight: FontWeight.w800,
              letterSpacing: 1.4,
              color: tema.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              for (final (i, acceso) in _accesos.indexed) ...[
                if (i > 0) const SizedBox(width: 10),
                Expanded(
                  child: _Acceso(
                    icono: acceso.$1,
                    texto: acceso.$2,
                    color: acceso.$3 ?? colores.acento,
                  ),
                ),
              ],
            ],
          ),
        ],
      ],
    );
  }
}

const _accesos = <(IconData, String, Color?)>[
  (Icons.calendar_today_outlined, 'Reservar', null),
  (Icons.assignment_turned_in_outlined, 'Simulacro', Color(0xFF0284C7)),
  (Icons.menu_book_outlined, 'Manuales', Color(0xFF6366F1)),
  (Icons.smart_toy_outlined, 'Asistente', Color(0xFF16A34A)),
];

class _Chip extends StatelessWidget {
  final IconData icono;
  final String texto;

  const _Chip({required this.icono, required this.texto});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = context.cockpit;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: tema.colorScheme.surface,
        borderRadius: BorderRadius.circular(99),
        border: Border.all(color: colores.borde),
      ),
      child: Row(
        children: [
          Icon(icono, size: 15, color: colores.acento),
          const SizedBox(width: 6),
          Text(
            texto,
            style: tema.textTheme.labelMedium?.copyWith(
              fontWeight: FontWeight.w700,
              color: tema.colorScheme.onSurface,
            ),
          ),
        ],
      ),
    );
  }
}

/// Tarjeta principal: la próxima clase (o, para el instructor, el día). Superficie opaca con borde
/// ámbar y la ruta dibujada al fondo a la derecha, lejos del texto, para que se lea bien.
class _TarjetaProximaClase extends StatelessWidget {
  final bool esInstructor;

  /// La clase por venir más cercana del estudiante, o `null` si no tiene.
  final Reserva? proxima;
  final bool cargando;
  final bool error;

  const _TarjetaProximaClase({
    required this.esInstructor,
    this.proxima,
    this.cargando = false,
    this.error = false,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final colores = context.cockpit;
    final reserva = proxima;
    final titulo = reserva != null
        ? '${tituloDeDia(reserva.fecha, DateTime.now())} · ${rangoDeHoras(reserva.horaInicio, reserva.horaFin)}'
        : esInstructor
        ? 'Sin clases por ahora'
        : cargando
        ? 'Buscando tus clases…'
        : 'Aún no tienes clases reservadas';
    final texto = reserva != null
        ? 'Con ${reserva.instructorNombre}'
        : esInstructor
        ? 'Tu agenda aparecerá acá cuando tengas clases asignadas.'
        : error
        ? 'No pudimos cargar tus clases. Revisa tu conexión.'
        : 'Cuando reserves una clase, verás acá la hora y tu instructor.';

    return Container(
      decoration: BoxDecoration(
        color: tema.colorScheme.surface,
        borderRadius: BorderRadius.circular(AppRadios.tarjeta),
        border: Border.all(color: AppColors.ambar.withValues(alpha: 0.55)),
        boxShadow: [
          BoxShadow(
            color: AppColors.ambar.withValues(alpha: 0.18),
            blurRadius: 30,
            offset: const Offset(0, 14),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          Positioned(
            right: -6,
            bottom: -4,
            width: 120,
            height: 76,
            child: IgnorePointer(
              child: CustomPaint(
                painter: _RutaPainter(
                  color: colores.acento.withValues(alpha: 0.28),
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  esInstructor ? 'HOY' : 'PRÓXIMA CLASE',
                  style: tema.textTheme.labelSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.4,
                    color: colores.acento,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  titulo,
                  style: tema.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 6),
                Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppColors.ambar,
                    borderRadius: BorderRadius.circular(9),
                  ),
                ),
                const SizedBox(height: 10),
                // * Deja libre la esquina derecha, donde va dibujada la ruta, para que no pase por el texto
                Padding(
                  padding: const EdgeInsets.only(right: 70),
                  child: Text(
                    texto,
                    style: tema.textTheme.bodyMedium?.copyWith(
                      color: tema.colorScheme.onSurfaceVariant,
                    ),
                  ),
                ),
                if (!esInstructor && reserva == null) ...[
                  const SizedBox(height: 16),
                  SizedBox(
                    width: 190,
                    child: FilledButton.icon(
                      onPressed: () => avisar(
                        context,
                        'Reservar: disponible en el siguiente caso de uso',
                      ),
                      icon: const Icon(Icons.add, size: 18),
                      label: const Text('Reservar clase'),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Dos líneas que se juntan hacia el horizonte con la raya central discontinua de una ruta.
class _RutaPainter extends CustomPainter {
  final Color color;

  const _RutaPainter({required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final borde = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    final centro = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeWidth = 3;
    final x = size.width / 2;

    canvas.drawLine(
      Offset(x, 0),
      Offset(size.width * 0.15, size.height),
      borde,
    );
    canvas.drawLine(
      Offset(x, 0),
      Offset(size.width * 0.85, size.height),
      borde,
    );
    for (final tramo in [(0.05, 0.2), (0.34, 0.52), (0.68, 0.92)]) {
      canvas.drawLine(
        Offset(x, size.height * tramo.$1),
        Offset(x, size.height * tramo.$2),
        centro,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _RutaPainter oldDelegate) =>
      oldDelegate.color != color;
}

class _Acceso extends StatelessWidget {
  final IconData icono;
  final String texto;
  final Color color;

  const _Acceso({
    required this.icono,
    required this.texto,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Material(
      color: tema.colorScheme.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(AppRadios.control),
        side: BorderSide(color: context.cockpit.borde),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(AppRadios.control),
        onTap: () =>
            avisar(context, '$texto: disponible en el siguiente caso de uso'),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 14),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.14),
                  shape: BoxShape.circle,
                  border: Border.all(color: color.withValues(alpha: 0.5)),
                ),
                child: Icon(icono, size: 21, color: color),
              ),
              const SizedBox(height: 8),
              Text(
                texto.toUpperCase(),
                style: tema.textTheme.labelSmall?.copyWith(
                  fontWeight: FontWeight.w800,
                  letterSpacing: 0.6,
                  fontSize: 10.5,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
