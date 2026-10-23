import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_theme.dart';
import '../models/fechas.dart';
import '../models/franja.dart';
import '../providers/franjas_provider.dart';
import '../widgets/errores.dart';
import '../widgets/status_chip.dart';

/// Las franjas de la semana del instructor, agrupadas por día (CU12): verlas, bloquear o desbloquear una
/// franja suelta o un día completo.
///
/// * Es un widget de cuerpo, sin `Scaffold`: es la pestaña «Franjas» de `HorariosScreen`.
/// * Una franja reservada, o que ya empezó, no ofrece acción. Eso es solo la interfaz: el backend
/// * igual rechaza con `ERR_ACA_08`, `ERR_ACA_09` o `ERR_ACA_10`.
class FranjasView extends ConsumerStatefulWidget {
  const FranjasView({super.key});

  @override
  ConsumerState<FranjasView> createState() => _FranjasViewState();
}

class _FranjasViewState extends ConsumerState<FranjasView> {
  // * Franjas con una acción en curso: su tarjeta queda deshabilitada hasta que responda el backend
  final Set<String> _enProceso = {};

  Future<void> _cambiarEstado(Franja franja, AccionFranja accion) async {
    final mensajes = ScaffoldMessenger.of(context);
    setState(() => _enProceso.add(franja.codigo));
    try {
      final notifier = ref.read(franjasProvider.notifier);
      if (accion == AccionFranja.bloquear) {
        await notifier.bloquear(franja.codigo);
        mensajes.showSnackBar(const SnackBar(content: Text('Franja bloqueada')));
      } else {
        await notifier.desbloquear(franja.codigo);
        mensajes.showSnackBar(const SnackBar(content: Text('Franja desbloqueada')));
      }
    } catch (e) {
      mensajes.showSnackBar(SnackBar(content: Text(mensajeDeError(e))));
      // * Probablemente cambió de estado en otro lado: se recarga para mostrar lo real
      ref.invalidate(franjasListProvider);
    } finally {
      if (mounted) setState(() => _enProceso.remove(franja.codigo));
    }
  }

  Future<void> _cambiarDia(DateTime fecha, {required bool bloquear}) async {
    final mensajes = ScaffoldMessenger.of(context);
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (dialogo) => AlertDialog(
        title: Text(bloquear ? 'Bloquear el día' : 'Desbloquear el día'),
        content: Text(
          bloquear
              ? 'Se bloquearán las franjas libres del ${diaConNumero(fecha)} que todavía no empezaron. Las reservadas no se tocan.'
              : 'Se volverán a ofrecer las franjas bloqueadas del ${diaConNumero(fecha)} que todavía no empezaron.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogo, false), child: const Text('Volver')),
          FilledButton(
            onPressed: () => Navigator.pop(dialogo, true),
            style: FilledButton.styleFrom(minimumSize: const Size(0, 48)),
            child: Text(bloquear ? 'Bloquear' : 'Desbloquear'),
          ),
        ],
      ),
    );
    if (confirmado != true) return;
    try {
      final notifier = ref.read(franjasProvider.notifier);
      final resultado = bloquear ? await notifier.bloquearDia(fecha) : await notifier.desbloquearDia(fecha);
      mensajes.showSnackBar(SnackBar(content: Text(_avisoDelDia(resultado.franjasAfectadas, bloquear))));
    } catch (e) {
      mensajes.showSnackBar(SnackBar(content: Text(mensajeDeError(e))));
      ref.invalidate(franjasListProvider);
    }
  }

  static String _avisoDelDia(int cantidad, bool bloquear) {
    if (cantidad == 0) return bloquear ? 'No había franjas libres' : 'No había franjas bloqueadas';
    final franjas = cantidad == 1 ? '1 franja' : '$cantidad franjas';
    final accion = bloquear
        ? (cantidad == 1 ? 'bloqueada' : 'bloqueadas')
        : (cantidad == 1 ? 'desbloqueada' : 'desbloqueadas');
    return '$franjas $accion';
  }

  @override
  Widget build(BuildContext context) {
    final lunes = ref.watch(franjasSemanaUiProvider);
    final franjas = ref.watch(franjasListProvider(lunes));
    final semana = ref.read(franjasSemanaUiProvider.notifier);
    final tema = Theme.of(context);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: AppSpace.sm),
          child: Row(
            children: [
              IconButton(tooltip: 'Semana anterior', icon: const Icon(Icons.chevron_left), onPressed: semana.anterior),
              Expanded(
                child: Text(
                  'Semana ${etiquetaDeSemana(lunes)}',
                  textAlign: TextAlign.center,
                  style: tema.textTheme.titleLarge,
                ),
              ),
              IconButton(
                tooltip: 'Semana siguiente',
                icon: const Icon(Icons.chevron_right),
                onPressed: semana.siguiente,
              ),
            ],
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: () async => ref.refresh(franjasListProvider(lunes).future),
            child: franjas.when(
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, _) => ListView(
                children: [
                  const SizedBox(height: 48),
                  Center(child: Text(mensajeDeError(error), textAlign: TextAlign.center)),
                  const SizedBox(height: AppSpace.sm),
                  Center(
                    child: OutlinedButton(
                      onPressed: () => ref.invalidate(franjasListProvider(lunes)),
                      style: OutlinedButton.styleFrom(minimumSize: const Size(0, 48)),
                      child: const Text('Reintentar'),
                    ),
                  ),
                ],
              ),
              data: (lista) {
                if (lista.isEmpty) {
                  return ListView(
                    children: const [
                      SizedBox(height: 48),
                      Center(child: Text('Aún no tienes franjas. Crea un horario para generarlas.')),
                    ],
                  );
                }
                return _SemanaConFranjas(
                  franjas: lista,
                  ocupadas: _enProceso,
                  alAccionar: _cambiarEstado,
                  alCambiarDia: _cambiarDia,
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}

class _SemanaConFranjas extends StatelessWidget {
  final List<Franja> franjas;
  final Set<String> ocupadas;
  final void Function(Franja franja, AccionFranja accion) alAccionar;
  final Future<void> Function(DateTime fecha, {required bool bloquear}) alCambiarDia;

  const _SemanaConFranjas({
    required this.franjas,
    required this.ocupadas,
    required this.alAccionar,
    required this.alCambiarDia,
  });

  @override
  Widget build(BuildContext context) {
    final resumen = ResumenFranjas.de(franjas);
    final ahora = DateTime.now();
    final hoy = soloFecha(ahora);

    // * Agrupadas por día en el orden que ya trae el backend (fecha y hora)
    final porDia = <DateTime, List<Franja>>{};
    for (final franja in franjas) {
      porDia.putIfAbsent(soloFecha(franja.fecha), () => []).add(franja);
    }

    return ListView(
      padding: const EdgeInsets.fromLTRB(AppSpace.lg, AppSpace.sm, AppSpace.lg, AppSpace.lg),
      children: [
        Wrap(
          spacing: AppSpace.sm,
          runSpacing: AppSpace.sm,
          children: [
            StatusChip.neutro(context, '${resumen.libres} Libres', Icons.radio_button_unchecked),
            StatusChip.azul(context, '${resumen.reservadas} Reservadas', Icons.check),
            StatusChip.neutro(context, '${resumen.bloqueadas} Bloqueadas', Icons.block, atenuado: true),
          ],
        ),
        for (final entrada in porDia.entries) ...[
          const SizedBox(height: AppSpace.lg),
          _EncabezadoDeDia(
            fecha: entrada.key,
            puedeBloquear: !entrada.key.isBefore(hoy) && entrada.value.any((f) => f.estado == EstadoFranja.libre),
            puedeDesbloquear:
                !entrada.key.isBefore(hoy) && entrada.value.any((f) => f.estado == EstadoFranja.bloqueada),
            alBloquear: () => alCambiarDia(entrada.key, bloquear: true),
            alDesbloquear: () => alCambiarDia(entrada.key, bloquear: false),
          ),
          const SizedBox(height: AppSpace.sm),
          _Mosaicos(franjas: entrada.value, ahora: ahora, ocupadas: ocupadas, alAccionar: alAccionar),
        ],
      ],
    );
  }
}

class _EncabezadoDeDia extends StatelessWidget {
  final DateTime fecha;
  final bool puedeBloquear;
  final bool puedeDesbloquear;
  final VoidCallback alBloquear;
  final VoidCallback alDesbloquear;

  const _EncabezadoDeDia({
    required this.fecha,
    required this.puedeBloquear,
    required this.puedeDesbloquear,
    required this.alBloquear,
    required this.alDesbloquear,
  });

  @override
  Widget build(BuildContext context) {
    final clave = fechaParaApi(fecha);
    return Row(
      children: [
        Expanded(child: Text(diaConNumero(fecha), style: Theme.of(context).textTheme.labelMedium)),
        if (puedeBloquear)
          TextButton(key: Key('bloquearDia-$clave'), onPressed: alBloquear, child: const Text('Bloquear día')),
        if (puedeDesbloquear)
          TextButton(key: Key('desbloquearDia-$clave'), onPressed: alDesbloquear, child: const Text('Desbloquear día')),
      ],
    );
  }
}

/// Las franjas de un día en dos columnas.
class _Mosaicos extends StatelessWidget {
  final List<Franja> franjas;
  final DateTime ahora;
  final Set<String> ocupadas;
  final void Function(Franja franja, AccionFranja accion) alAccionar;

  const _Mosaicos({required this.franjas, required this.ahora, required this.ocupadas, required this.alAccionar});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, restricciones) {
        final ancho = (restricciones.maxWidth - AppSpace.sm) / 2;
        return Wrap(
          spacing: AppSpace.sm,
          runSpacing: AppSpace.sm,
          children: [
            for (final franja in franjas)
              SizedBox(
                width: ancho,
                child: _Mosaico(
                  franja: franja,
                  accion: franja.accionDisponible(ahora),
                  ocupada: ocupadas.contains(franja.codigo),
                  alAccionar: (accion) => alAccionar(franja, accion),
                ),
              ),
          ],
        );
      },
    );
  }
}

class _Mosaico extends StatelessWidget {
  final Franja franja;
  final AccionFranja? accion;
  final bool ocupada;
  final void Function(AccionFranja accion) alAccionar;

  const _Mosaico({required this.franja, required this.accion, required this.ocupada, required this.alAccionar});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final bloqueada = franja.estado == EstadoFranja.bloqueada;

    // * Libre y reservada con sombra; bloqueada gris suave y sin sombra (design.md 3.6)
    return Container(
      padding: const EdgeInsets.symmetric(vertical: AppSpace.md, horizontal: AppSpace.md),
      decoration: BoxDecoration(
        color: switch (franja.estado) {
          EstadoFranja.libre => tema.colorScheme.surface,
          EstadoFranja.reservada => tema.colorScheme.tertiaryContainer,
          EstadoFranja.bloqueada => context.semantica.neutralContainer,
        },
        borderRadius: BorderRadius.circular(AppRadius.tile),
        boxShadow: bloqueada ? null : context.semantica.cardShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${franja.horaInicio} – ${franja.horaFin}',
            style: tema.textTheme.titleMedium?.copyWith(color: context.semantica.blueText, fontSize: 17),
          ),
          const SizedBox(height: AppSpace.xs),
          _EtiquetaEstado(estado: franja.estado),
          if (ocupada)
            const Padding(
              padding: EdgeInsets.only(top: AppSpace.sm),
              child: SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2)),
            )
          else if (accion != null)
            TextButton(
              onPressed: () => alAccionar(accion!),
              style: TextButton.styleFrom(padding: EdgeInsets.zero, alignment: Alignment.centerLeft),
              child: Text(accion == AccionFranja.bloquear ? 'Bloquear' : 'Desbloquear'),
            ),
        ],
      ),
    );
  }
}

class _EtiquetaEstado extends StatelessWidget {
  final EstadoFranja estado;

  const _EtiquetaEstado({required this.estado});

  @override
  Widget build(BuildContext context) {
    return switch (estado) {
      EstadoFranja.libre => StatusChip.neutro(context, 'Libre', Icons.radio_button_unchecked),
      EstadoFranja.reservada => StatusChip.azul(context, 'Reservada', Icons.check),
      EstadoFranja.bloqueada => StatusChip.neutro(context, 'Bloqueada', Icons.block, atenuado: true),
    };
  }
}
