import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../../../shared/widgets/indicador_boton.dart';
import '../models/preferencias_notificacion.dart';
import '../providers/notificaciones_providers.dart';
import '../validators/franja_silencio_validator.dart';

/// Horas que se proponen al encender el silencio: las de dormir, que cruzan la medianoche.
const _silencioPropuestoDesde = '22:00';
const _silencioPropuestoHasta = '06:00';

/// «Perfil > Notificaciones» (CU37): canales, tipos de aviso y horas de silencio.
///
/// * Lo que la persona toca vive en un borrador (`preferenciasFormUiProvider`) y solo se guarda al pulsar
///   «Guardar»; mientras no difiera de lo guardado el botón está deshabilitado.
/// * Mientras carga, o si la carga falla, no se muestran interruptores (sin valores inventados).
/// * Si el guardado falla, el borrador se conserva y se puede volver a intentar.
class PreferenciasNotificacionScreen extends ConsumerStatefulWidget {
  const PreferenciasNotificacionScreen({super.key});

  @override
  ConsumerState<PreferenciasNotificacionScreen> createState() =>
      _PreferenciasNotificacionScreenState();
}

class _PreferenciasNotificacionScreenState
    extends ConsumerState<PreferenciasNotificacionScreen> {
  bool _guardando = false;
  String? _error;

  Future<void> _guardar(PreferenciasNotificacion borrador) async {
    setState(() {
      _guardando = true;
      _error = null;
    });
    try {
      await ref
          .read(preferenciasNotificacionProvider.notifier)
          .guardar(borrador);
      ref.read(preferenciasFormUiProvider.notifier).descartar();
      if (mounted) avisar(context, 'Preferencias guardadas');
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted) {
        setState(
          () => _error =
              'No pudimos guardar tus preferencias. Revisa tu conexión e intenta de nuevo.',
        );
      }
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final preferencias = ref.watch(preferenciasNotificacionProvider);

    return CockpitScaffold(
      appBar: AppBar(title: const Text('Notificaciones')),
      body: SafeArea(
        child: preferencias.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => _ErrorDeCarga(
            alReintentar: () =>
                ref.invalidate(preferenciasNotificacionProvider),
          ),
          data: _formulario,
        ),
      ),
    );
  }

  Widget _formulario(PreferenciasNotificacion guardadas) {
    final borrador = ref.watch(preferenciasFormUiProvider);
    final actuales = borrador ?? guardadas;
    final errorFranja = validarFranjaSilencio(
      actuales.silencioDesde,
      actuales.silencioHasta,
    );
    final puedeGuardar =
        hayCambiosEnPreferencias(guardadas, borrador) &&
        errorFranja == null &&
        !_guardando;
    final tema = Theme.of(context);

    void cambiar(
      PreferenciasNotificacion Function(PreferenciasNotificacion) cambio,
    ) {
      ref.read(preferenciasFormUiProvider.notifier).editar(guardadas, cambio);
    }

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
      children: [
        Text(
          'Elige cómo y de qué quieres que te avisemos.',
          style: tema.textTheme.bodyMedium,
        ),
        const SizedBox(height: 16),
        Text('Canales', style: tema.textTheme.titleMedium),
        const SizedBox(height: 8),
        Card(
          child: Column(
            children: [
              _Interruptor(
                icono: Icons.mail_outline,
                titulo: 'Correo',
                detalle: 'Te escribimos a tu correo',
                valor: actuales.correo,
                alCambiar: (v) => cambiar((p) => p.conCanal(correo: v)),
              ),
              const Divider(height: 1),
              _Interruptor(
                icono: Icons.notifications_active_outlined,
                titulo: 'Notificaciones push',
                detalle: 'Avisos en la pantalla del teléfono',
                valor: actuales.push,
                alCambiar: (v) => cambiar((p) => p.conCanal(push: v)),
              ),
              const Divider(height: 1),
              _Interruptor(
                icono: Icons.inbox_outlined,
                titulo: 'Dentro de la app',
                detalle: 'Los guardamos en tu bandeja',
                valor: actuales.enApp,
                alCambiar: (v) => cambiar((p) => p.conCanal(enApp: v)),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),
        Text('Tipos de aviso', style: tema.textTheme.titleMedium),
        const SizedBox(height: 8),
        Card(
          child: Column(
            children: [
              for (final (i, tipo) in TipoNotificacion.values.indexed) ...[
                if (i > 0) const Divider(height: 1),
                _Interruptor(
                  titulo: tipo.titulo,
                  detalle: tipo.detalle,
                  valor: actuales.tipos[tipo] ?? true,
                  alCambiar: (v) => cambiar((p) => p.conTipo(tipo, v)),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 20),
        Text('Horas de silencio', style: tema.textTheme.titleMedium),
        const SizedBox(height: 8),
        Card(
          child: Column(
            children: [
              _Interruptor(
                icono: Icons.bedtime_outlined,
                titulo: 'Horas de silencio',
                detalle: 'Sin notificaciones push en esas horas',
                valor: actuales.tieneSilencio,
                alCambiar: (v) => cambiar(
                  (p) => v
                      ? p.conSilencio(
                          desde: _silencioPropuestoDesde,
                          hasta: _silencioPropuestoHasta,
                        )
                      : p.conSilencio(),
                ),
              ),
              if (actuales.tieneSilencio) ...[
                const Divider(height: 1),
                _FilaHora(
                  etiqueta: 'Desde',
                  hora: actuales.silencioDesde,
                  alElegir: (h) => cambiar(
                    (p) => p.conSilencio(desde: h, hasta: p.silencioHasta),
                  ),
                ),
                const Divider(height: 1),
                _FilaHora(
                  etiqueta: 'Hasta',
                  hora: actuales.silencioHasta,
                  alElegir: (h) => cambiar(
                    (p) => p.conSilencio(desde: p.silencioDesde, hasta: h),
                  ),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'El silencio solo detiene las notificaciones push. El correo y tu '
          'bandeja siguen funcionando. Hora de Bolivia.',
          style: tema.textTheme.bodySmall?.copyWith(
            color: tema.colorScheme.onSurfaceVariant,
          ),
        ),
        if (errorFranja != null) ...[
          const SizedBox(height: 12),
          AvisoCockpit(texto: errorFranja, tono: TonoAviso.advertencia),
        ],
        if (_error != null) ...[
          const SizedBox(height: 12),
          AvisoCockpit(texto: _error!, tono: TonoAviso.error),
        ],
        const SizedBox(height: 24),
        FilledButton(
          onPressed: puedeGuardar ? () => _guardar(actuales) : null,
          child: _guardando ? const IndicadorBoton() : const Text('Guardar'),
        ),
      ],
    );
  }
}

/// Una fila con interruptor: título, detalle y, si se da, un icono a la izquierda.
class _Interruptor extends StatelessWidget {
  final IconData? icono;
  final String titulo;
  final String detalle;
  final bool valor;
  final ValueChanged<bool> alCambiar;

  const _Interruptor({
    this.icono,
    required this.titulo,
    required this.detalle,
    required this.valor,
    required this.alCambiar,
  });

  @override
  Widget build(BuildContext context) {
    return SwitchListTile(
      secondary: icono == null ? null : Icon(icono),
      title: Text(titulo),
      subtitle: Text(detalle),
      value: valor,
      onChanged: alCambiar,
    );
  }
}

/// «Desde» / «Hasta»: muestra la hora y abre el selector de hora de Flutter en formato de 24 horas.
class _FilaHora extends StatelessWidget {
  final String etiqueta;
  final String? hora;
  final ValueChanged<String> alElegir;

  const _FilaHora({
    required this.etiqueta,
    required this.hora,
    required this.alElegir,
  });

  Future<void> _elegir(BuildContext context) async {
    final elegida = await showTimePicker(
      context: context,
      initialTime: _comoHora(hora) ?? const TimeOfDay(hour: 22, minute: 0),
      builder: (context, hijo) => MediaQuery(
        data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
        child: hijo!,
      ),
    );
    if (elegida != null) alElegir(_comoTexto(elegida));
  }

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: const Icon(Icons.schedule),
      title: Text(etiqueta),
      trailing: Text(
        hora ?? '--:--',
        style: Theme.of(context).textTheme.titleMedium,
      ),
      onTap: () => _elegir(context),
    );
  }

  static TimeOfDay? _comoHora(String? texto) {
    if (texto == null) return null;
    final partes = texto.split(':');
    if (partes.length != 2) return null;
    final h = int.tryParse(partes[0]);
    final m = int.tryParse(partes[1]);
    return h == null || m == null ? null : TimeOfDay(hour: h, minute: m);
  }

  static String _comoTexto(TimeOfDay hora) =>
      '${hora.hour.toString().padLeft(2, '0')}:${hora.minute.toString().padLeft(2, '0')}';
}

/// La carga falló: no se muestran interruptores, solo el motivo y «Reintentar».
class _ErrorDeCarga extends StatelessWidget {
  final VoidCallback alReintentar;

  const _ErrorDeCarga({required this.alReintentar});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const EstadoVacio(
              icono: Icons.cloud_off_outlined,
              titulo: 'No pudimos cargar tus preferencias',
              texto: 'Revisa tu conexión e intenta de nuevo.',
            ),
            OutlinedButton(
              onPressed: alReintentar,
              child: const Text('Reintentar'),
            ),
          ],
        ),
      ),
    );
  }
}
