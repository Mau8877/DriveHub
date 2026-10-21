import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/fechas.dart';
import '../models/franja.dart';
import '../providers/franjas_provider.dart';
import '../widgets/errores.dart';

/// Las franjas del instructor en una fecha: verlas, bloquear las libres y desbloquear las
/// bloqueadas (CU12).
///
/// * Una franja reservada, o que ya empezó, no ofrece acción. Eso es solo la interfaz: el backend
/// * igual rechaza con `ERR_ACA_08`, `ERR_ACA_09` o `ERR_ACA_10`.
class FranjasScreen extends ConsumerStatefulWidget {
  const FranjasScreen({super.key});

  @override
  ConsumerState<FranjasScreen> createState() => _FranjasScreenState();
}

class _FranjasScreenState extends ConsumerState<FranjasScreen> {
  // * Franjas con una acción en curso: su fila queda deshabilitada hasta que responda el backend
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

  Future<void> _elegirFecha(DateTime actual) async {
    final elegida = await showDatePicker(
      context: context,
      initialDate: actual,
      firstDate: DateTime(2020),
      lastDate: DateTime.now().add(const Duration(days: 365 * 2)),
      helpText: 'Ver franjas del día',
    );
    if (elegida != null) ref.read(franjasFechaUiProvider.notifier).fijar(elegida);
  }

  @override
  Widget build(BuildContext context) {
    final fecha = ref.watch(franjasFechaUiProvider);
    final franjas = ref.watch(franjasListProvider(fecha));
    final notifierFecha = ref.read(franjasFechaUiProvider.notifier);

    return Scaffold(
      appBar: AppBar(title: const Text('Mis franjas')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                IconButton(
                  tooltip: 'Día anterior',
                  icon: const Icon(Icons.chevron_left),
                  onPressed: () => notifierFecha.fijar(fecha.subtract(const Duration(days: 1))),
                ),
                TextButton.icon(
                  key: const Key('botonElegirFecha'),
                  onPressed: () => _elegirFecha(fecha),
                  icon: const Icon(Icons.calendar_today_outlined),
                  label: Text('${fechaParaMostrar(fecha)} · ${nombreDeDia(fecha.weekday)}'),
                ),
                IconButton(
                  tooltip: 'Día siguiente',
                  icon: const Icon(Icons.chevron_right),
                  onPressed: () => notifierFecha.fijar(fecha.add(const Duration(days: 1))),
                ),
              ],
            ),
          ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () async => ref.refresh(franjasListProvider(fecha).future),
              child: franjas.when(
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (error, _) => ListView(
                  children: [
                    const SizedBox(height: 48),
                    Center(child: Text(mensajeDeError(error), textAlign: TextAlign.center)),
                    const SizedBox(height: 8),
                    Center(
                      child: OutlinedButton(
                        onPressed: () => ref.invalidate(franjasListProvider(fecha)),
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
                        Center(child: Text('No tienes franjas este día')),
                      ],
                    );
                  }
                  final ahora = DateTime.now();
                  return ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                    itemCount: lista.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (_, i) => _FilaFranja(
                      franja: lista[i],
                      accion: lista[i].accionDisponible(ahora),
                      ocupada: _enProceso.contains(lista[i].codigo),
                      alAccionar: (accion) => _cambiarEstado(lista[i], accion),
                    ),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _FilaFranja extends StatelessWidget {
  final Franja franja;
  final AccionFranja? accion;
  final bool ocupada;
  final void Function(AccionFranja accion) alAccionar;

  const _FilaFranja({
    required this.franja,
    required this.accion,
    required this.ocupada,
    required this.alAccionar,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        title: Text('${franja.horaInicio} – ${franja.horaFin}'),
        subtitle: Align(alignment: Alignment.centerLeft, child: _EtiquetaEstado(estado: franja.estado)),
        trailing: ocupada
            ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
            : accion == null
            ? null
            : TextButton(
                onPressed: () => alAccionar(accion!),
                child: Text(accion == AccionFranja.bloquear ? 'Bloquear' : 'Desbloquear'),
              ),
      ),
    );
  }
}

class _EtiquetaEstado extends StatelessWidget {
  final EstadoFranja estado;

  const _EtiquetaEstado({required this.estado});

  @override
  Widget build(BuildContext context) {
    final esquema = Theme.of(context).colorScheme;
    final (texto, fondo, letra) = switch (estado) {
      EstadoFranja.libre => ('Libre', esquema.primaryContainer, esquema.onPrimaryContainer),
      EstadoFranja.reservada => ('Reservada', esquema.secondaryContainer, esquema.onSecondaryContainer),
      EstadoFranja.bloqueada => ('Bloqueada', esquema.surfaceContainerHighest, esquema.onSurfaceVariant),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(8)),
      child: Text(texto, style: Theme.of(context).textTheme.labelSmall?.copyWith(color: letra)),
    );
  }
}
