import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/aviso_snack.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../models/notificacion.dart';
import '../models/preferencias_notificacion.dart';
import '../providers/bandeja_providers.dart';
import '../widgets/detalle_notificacion_sheet.dart';
import '../widgets/fecha_aviso.dart';

/// Cuánto falta para el final de la lista cuando se pide la página siguiente.
const double _margenParaCargarMas = 240;

/// La bandeja de avisos (CU38): lo que la plataforma le mandó a la persona, del más reciente al más antiguo.
///
/// * Abrir un aviso muestra su mensaje completo y lo marca como leído. Ver la lista no marca nada.
/// * Al llegar al final se carga la página siguiente; deslizar hacia abajo la vuelve a pedir.
/// * Mientras carga, o si la carga falla, no se muestra una lista inventada.
class NotificacionesScreen extends ConsumerStatefulWidget {
  const NotificacionesScreen({super.key});

  @override
  ConsumerState<NotificacionesScreen> createState() =>
      _NotificacionesScreenState();
}

class _NotificacionesScreenState extends ConsumerState<NotificacionesScreen> {
  final _scroll = ScrollController();
  bool _marcandoTodas = false;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_alDesplazar);
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  void _alDesplazar() {
    if (_scroll.position.extentAfter < _margenParaCargarMas) {
      ref.read(bandejaProvider.notifier).cargarMas();
    }
  }

  Future<void> _abrir(Notificacion aviso) async {
    final marcado = ref
        .read(bandejaProvider.notifier)
        .marcarLeida(aviso.codigoNotificacion)
        // * El mensaje se ve aunque no se pueda marcar; solo se avisa del problema
        .catchError((Object _) {
          if (mounted) {
            avisar(context, 'No pudimos marcar el aviso como leído');
          }
        });
    await mostrarDetalleNotificacion(context, aviso);
    await marcado;
  }

  Future<void> _marcarTodas() async {
    setState(() => _marcandoTodas = true);
    try {
      await ref.read(bandejaProvider.notifier).marcarTodas();
      if (mounted) avisar(context, 'Todos tus avisos quedaron leídos');
    } catch (_) {
      if (mounted) avisar(context, 'No pudimos marcar los avisos como leídos');
    } finally {
      if (mounted) setState(() => _marcandoTodas = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final bandeja = ref.watch(bandejaProvider);
    final conteo = ref.watch(conteoNoLeidasProvider).valueOrNull ?? 0;

    return CockpitScaffold(
      appBar: AppBar(
        title: const Text('Notificaciones'),
        actions: [
          TextButton(
            key: const Key('botonMarcarTodas'),
            onPressed: conteo > 0 && !_marcandoTodas ? _marcarTodas : null,
            child: const Text('Marcar todas como leídas'),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            const _Filtros(),
            Expanded(
              child: RefreshIndicator(
                onRefresh: () async {
                  ref.invalidate(conteoNoLeidasProvider);
                  await ref.read(bandejaProvider.notifier).recargar();
                },
                child: bandeja.when(
                  loading: () => const Center(child: CircularProgressIndicator()),
                  error: (_, _) => _ErrorDeCarga(
                    alReintentar: () => ref.invalidate(bandejaProvider),
                  ),
                  data: _lista,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _lista(Bandeja datos) {
    final filtro = ref.watch(bandejaFiltroUiProvider);
    if (datos.items.isEmpty) {
      return ListView(
        // * Siempre desplazable: así «deslizar para actualizar» funciona también con la lista vacía
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          EstadoVacio(
            icono: Icons.notifications_none,
            titulo: filtro.esTodas ? 'No tienes avisos' : 'No hay avisos con este filtro',
            texto: filtro.esTodas
                ? 'Cuando haya novedades de tus clases, pagos o exámenes, las verás aquí.'
                : 'Prueba con otro filtro.',
          ),
        ],
      );
    }

    final ahora = DateTime.now();
    return ListView(
      controller: _scroll,
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 24),
      children: [
        for (final aviso in datos.items) ...[
          _TarjetaAviso(
            aviso: aviso,
            ahora: ahora,
            alAbrir: () => _abrir(aviso),
          ),
          const SizedBox(height: 8),
        ],
        if (datos.errorAlCargarMas != null)
          const AvisoCockpit(
            texto: 'No pudimos cargar más avisos. Desliza para volver a intentarlo.',
            tono: TonoAviso.error,
          ),
        if (datos.cargandoMas)
          const Padding(
            padding: EdgeInsets.all(16),
            child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
          ),
      ],
    );
  }
}

/// Las fichas de filtro: «Todas», «No leídas» y los cuatro tipos.
class _Filtros extends ConsumerWidget {
  const _Filtros();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final filtro = ref.watch(bandejaFiltroUiProvider);
    final notifier = ref.read(bandejaFiltroUiProvider.notifier);

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
      child: Row(
        children: [
          ChoiceChip(
            label: const Text('Todas'),
            selected: filtro.esTodas,
            onSelected: (_) => notifier.todas(),
          ),
          const SizedBox(width: 8),
          ChoiceChip(
            label: const Text('No leídas'),
            selected: filtro.soloNoLeidas,
            onSelected: (_) => notifier.alternarNoLeidas(),
          ),
          for (final tipo in TipoNotificacion.values) ...[
            const SizedBox(width: 8),
            ChoiceChip(
              label: Text(tipo.titulo),
              selected: filtro.tipo == tipo,
              onSelected: (_) => notifier.elegirTipo(tipo),
            ),
          ],
        ],
      ),
    );
  }
}

class _TarjetaAviso extends StatelessWidget {
  final Notificacion aviso;
  final DateTime ahora;
  final VoidCallback alAbrir;

  const _TarjetaAviso({
    required this.aviso,
    required this.ahora,
    required this.alAbrir,
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final acento = context.cockpit.acento;

    return Card(
      child: InkWell(
        key: Key('aviso-${aviso.codigoNotificacion}'),
        borderRadius: BorderRadius.circular(16),
        onTap: alAbrir,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: acento.withValues(alpha: 0.14),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(_iconoDe(aviso.tipo), color: acento, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            aviso.titulo,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: tema.textTheme.titleSmall?.copyWith(
                              fontWeight: aviso.leida ? FontWeight.w500 : FontWeight.w800,
                            ),
                          ),
                        ),
                        if (!aviso.leida)
                          Container(
                            key: Key('noLeida-${aviso.codigoNotificacion}'),
                            width: 8,
                            height: 8,
                            margin: const EdgeInsets.only(left: 8),
                            decoration: BoxDecoration(color: acento, shape: BoxShape.circle),
                          ),
                      ],
                    ),
                    const SizedBox(height: 2),
                    Text(
                      aviso.mensaje,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: tema.textTheme.bodyMedium,
                    ),
                    const SizedBox(height: 6),
                    Text(
                      '${aviso.etiquetaTipo} · ${fechaDeAviso(aviso.creadaEn, ahora: ahora)}',
                      style: tema.textTheme.bodySmall?.copyWith(
                        color: tema.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  IconData _iconoDe(TipoNotificacion? tipo) => switch (tipo) {
    TipoNotificacion.reserva => Icons.event_available_outlined,
    TipoNotificacion.pago => Icons.payments_outlined,
    TipoNotificacion.examen => Icons.quiz_outlined,
    TipoNotificacion.recordatorio => Icons.alarm_outlined,
    null => Icons.notifications_none,
  };
}

class _ErrorDeCarga extends StatelessWidget {
  final VoidCallback alReintentar;

  const _ErrorDeCarga({required this.alReintentar});

  @override
  Widget build(BuildContext context) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(24),
      children: [
        const AvisoCockpit(
          texto: 'No pudimos cargar tus avisos. Revisa tu conexión e inténtalo de nuevo.',
          tono: TonoAviso.error,
        ),
        const SizedBox(height: 12),
        OutlinedButton(
          key: const Key('botonReintentarAvisos'),
          onPressed: alReintentar,
          child: const Text('Reintentar'),
        ),
      ],
    );
  }
}
