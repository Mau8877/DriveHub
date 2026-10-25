import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/cockpit_scaffold.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../models/estado_pago.dart';
import '../models/pago_iniciado.dart';
import '../providers/abridor_provider.dart';
import '../providers/compra_provider.dart';
import '../providers/estado_de_pago_provider.dart';

/// El resultado de la compra (CU26, `/payments/result`): «Pago en proceso» mientras se espera la confirmación
/// de la pasarela, y después «¡Listo!», «No se pudo completar» o «Tu pago sigue en proceso».
///
/// * El estado no sale del enlace de retorno (puede no llegar nunca): se consulta al backend cada pocos
/// * segundos y de inmediato al volver a la app desde la página de pago (`resumed`).
/// * Quien confirma es el backend, con el aviso firmado de la pasarela: esta pantalla solo lo muestra.
/// * Cuando el pago llega a un estado final, la compra en curso se termina: la próxima usa una clave nueva.
/// * Cada estado se distingue por su icono y su texto, no solo por el color.
class PagoResultadoScreen extends ConsumerStatefulWidget {
  const PagoResultadoScreen({super.key});

  @override
  ConsumerState<PagoResultadoScreen> createState() =>
      _PagoResultadoScreenState();
}

class _PagoResultadoScreenState extends ConsumerState<PagoResultadoScreen>
    with WidgetsBindingObserver {
  /// El pago que se está siguiendo. Se toma al abrir la pantalla: terminar la compra lo borra de la compra en
  /// curso, y la pantalla tiene que seguir mostrándolo.
  PagoIniciado? _pago;
  bool _terminada = false;

  ConsultaDeEstado? get _consulta =>
      _pago == null ? null : ConsultaDeEstado(_pago!.codigo, _pago!.expiraEn);

  @override
  void initState() {
    super.initState();
    _pago = ref.read(compraUiProvider).pago;
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState estado) {
    final consulta = _consulta;
    if (estado == AppLifecycleState.resumed && consulta != null) {
      unawaited(ref.read(estadoDePagoProvider(consulta).notifier).refrescar());
    }
  }

  void _terminarLaCompraSiYaNoCambia(EstadoPago estado) {
    if (_terminada || !estado.esFinal) return;
    _terminada = true;
    // * Se hace después del cuadro actual: no se puede cambiar un provider mientras se construye el árbol
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) ref.read(compraUiProvider.notifier).terminar();
    });
  }

  Future<void> _volverAAbrir(String url) async {
    final mensajes = ScaffoldMessenger.of(context);
    final abrio = await ref.read(abridorDePaginaProvider)(url);
    if (!abrio) {
      mensajes
        ..hideCurrentSnackBar()
        ..showSnackBar(
          const SnackBar(
            content: Text(
              'No pudimos abrir la página de pago. Inténtalo de nuevo.',
            ),
          ),
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    final pago = _pago;
    final consulta = _consulta;
    if (pago == null || consulta == null) {
      return CockpitScaffold(
        appBar: AppBar(
          title: const Text('Tu pago'),
          automaticallyImplyLeading: false,
        ),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const EstadoVacio(
                icono: Icons.receipt_long_outlined,
                titulo: 'No hay una compra en curso',
                texto:
                    'Cuando compres un plan, aquí verás el resultado de tu pago.',
              ),
              FilledButton(
                onPressed: () => context.go('/inicio'),
                child: const Text('Ir al inicio'),
              ),
            ],
          ),
        ),
      );
    }

    ref.listen(estadoDePagoProvider(consulta), (_, siguiente) {
      final estado = siguiente.valueOrNull?.estado.estado;
      if (estado != null) _terminarLaCompraSiYaNoCambia(estado);
    });
    final seguimiento = ref.watch(estadoDePagoProvider(consulta));

    return CockpitScaffold(
      appBar: AppBar(
        title: const Text('Tu pago'),
        automaticallyImplyLeading: false,
      ),
      body: SafeArea(
        child: seguimiento.when(
          loading: () => const _Mensaje(
            icono: Icons.hourglass_top,
            titulo: 'Consultando tu pago…',
            texto: 'Un momento, por favor.',
            cargando: true,
          ),
          error: (_, _) => ListView(
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 24),
            children: [
              const AvisoCockpit(
                texto:
                    'No pudimos consultar el estado de tu pago. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarEstado'),
                onPressed: () => ref.invalidate(estadoDePagoProvider(consulta)),
                child: const Text('Reintentar'),
              ),
              TextButton(
                onPressed: () => context.go('/inicio'),
                child: const Text('Volver al inicio'),
              ),
            ],
          ),
          data: (datos) => _contenido(context, pago, datos),
        ),
      ),
    );
  }

  Widget _contenido(
    BuildContext context,
    PagoIniciado pago,
    SeguimientoDePago datos,
  ) {
    switch (datos.estado.estado) {
      case EstadoPago.confirmado:
        return _Mensaje(
          icono: Icons.check_circle_outline,
          titulo: '¡Listo! Tus horas ya están disponibles',
          texto: pago.horas == 1
              ? 'Se sumó 1 hora a tu cuenta.'
              : 'Se sumaron ${pago.horas} horas a tu cuenta.',
          tono: TonoAviso.exito,
          acciones: [
            FilledButton(
              key: const Key('botonIrAlInicio'),
              onPressed: () => context.go('/inicio'),
              child: const Text('Ir al inicio'),
            ),
          ],
        );
      case EstadoPago.fallido:
      case EstadoPago.revertido:
        return _Mensaje(
          icono: Icons.error_outline,
          titulo: 'No se pudo completar la compra',
          texto:
              'No se acreditaron horas. Si te cobraron, te devolveremos el dinero.',
          tono: TonoAviso.error,
          acciones: [
            FilledButton(
              key: const Key('botonVolverALosPlanes'),
              onPressed: () => context.go('/payments/plans'),
              child: const Text('Volver a los planes'),
            ),
          ],
        );
      case EstadoPago.pendiente:
      case EstadoPago.desconocido:
        if (datos.sinNovedades) {
          return _Mensaje(
            icono: Icons.schedule,
            titulo: 'Tu pago sigue en proceso',
            texto:
                'Todavía no recibimos la confirmación. Cuando llegue, tus horas aparecerán en tu cuenta. '
                'Puedes volver más tarde.',
            acciones: [
              FilledButton(
                key: const Key('botonVolverAlInicio'),
                onPressed: () => context.go('/inicio'),
                child: const Text('Volver al inicio'),
              ),
            ],
          );
        }
        final url = pago.urlPago;
        final vigente = pago.expiraEn.isAfter(DateTime.now());
        return _Mensaje(
          icono: Icons.hourglass_top,
          titulo: 'Pago en proceso',
          texto:
              'Estamos esperando la confirmación de tu pago. Puede tardar unos segundos. '
              'Si ya pagaste, tus horas se acreditarán igual.',
          cargando: true,
          acciones: [
            if (url != null && vigente)
              OutlinedButton(
                key: const Key('botonVolverAAbrirElPago'),
                onPressed: () => _volverAAbrir(url),
                child: const Text('Volver a abrir la página de pago'),
              ),
            TextButton(
              key: const Key('botonVolverAlInicio'),
              onPressed: () => context.go('/inicio'),
              child: const Text('Volver al inicio'),
            ),
          ],
        );
    }
  }
}

/// Un estado de la compra: icono, título, texto y las acciones que corresponden.
class _Mensaje extends StatelessWidget {
  final IconData icono;
  final String titulo;
  final String texto;
  final TonoAviso? tono;
  final bool cargando;
  final List<Widget> acciones;

  const _Mensaje({
    required this.icono,
    required this.titulo,
    required this.texto,
    this.tono,
    this.cargando = false,
    this.acciones = const [],
  });

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final color = switch (tono) {
      TonoAviso.exito => context.cockpit.exito,
      TonoAviso.error => tema.colorScheme.error,
      _ => tema.colorScheme.onSurfaceVariant,
    };

    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Icon(icono, size: 56, color: color),
            const SizedBox(height: 16),
            Text(
              titulo,
              style: tema.textTheme.titleLarge,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              texto,
              style: tema.textTheme.bodyMedium?.copyWith(
                color: tema.colorScheme.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
            if (cargando) ...[
              const SizedBox(height: 20),
              const Center(
                child: SizedBox(
                  width: 24,
                  height: 24,
                  child: CircularProgressIndicator(strokeWidth: 2.5),
                ),
              ),
            ],
            if (acciones.isNotEmpty) ...[
              const SizedBox(height: 24),
              ...acciones,
            ],
          ],
        ),
      ),
    );
  }
}
