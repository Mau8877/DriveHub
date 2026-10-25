import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/aviso_cockpit.dart';
import '../../../shared/widgets/estado_vacio.dart';
import '../models/hora_alumno.dart';
import '../providers/alumnos_busqueda_provider.dart';
import '../providers/mis_alumnos_provider.dart';

/// La sección «Alumnos» del instructor (CU29): sus alumnos con las horas compradas, las gastadas con él y las
/// que les quedan, con búsqueda por nombre.
///
/// * La búsqueda la hace el backend (con una pausa de 400 ms desde la última tecla); la app no filtra.
/// * «Cargar más» agrega la siguiente página (no hay scroll infinito); deslizar hacia abajo lo vuelve a pedir todo.
/// * Solo lectura: tocar un alumno abre su detalle.
class MisAlumnosScreen extends ConsumerStatefulWidget {
  const MisAlumnosScreen({super.key});

  @override
  ConsumerState<MisAlumnosScreen> createState() => _MisAlumnosScreenState();
}

class _MisAlumnosScreenState extends ConsumerState<MisAlumnosScreen> {
  late final TextEditingController _busqueda;

  @override
  void initState() {
    super.initState();
    _busqueda = TextEditingController(text: ref.read(alumnosBusquedaUiProvider));
  }

  @override
  void dispose() {
    _busqueda.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final listado = ref.watch(misAlumnosProvider);
    final hayBusqueda = ref.watch(alumnosBusquedaUiProvider).trim().isNotEmpty;

    return RefreshIndicator(
      onRefresh: () => ref.read(misAlumnosProvider.notifier).refrescar(),
      child: ListView(
        padding: const EdgeInsets.fromLTRB(20, 4, 20, 24),
        children: [
          TextField(
            key: const Key('campoBuscarAlumno'),
            controller: _busqueda,
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(
              hintText: 'Buscar por nombre',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: hayBusqueda
                  ? IconButton(
                      key: const Key('botonLimpiarBusqueda'),
                      tooltip: 'Borrar búsqueda',
                      icon: const Icon(Icons.close),
                      onPressed: () {
                        _busqueda.clear();
                        ref.read(alumnosBusquedaUiProvider.notifier).escribir('');
                      },
                    )
                  : null,
            ),
            onChanged: (texto) =>
                ref.read(alumnosBusquedaUiProvider.notifier).escribir(texto),
          ),
          const SizedBox(height: 12),
          ...listado.when(
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (_, _) => [
              const AvisoCockpit(
                texto:
                    'No pudimos cargar tus alumnos. Revisa tu conexión e inténtalo de nuevo.',
                tono: TonoAviso.error,
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                key: const Key('botonReintentarAlumnos'),
                onPressed: () => ref.invalidate(misAlumnosProvider),
                child: const Text('Reintentar'),
              ),
            ],
            data: (datos) => _lista(datos, hayBusqueda),
          ),
        ],
      ),
    );
  }

  List<Widget> _lista(AlumnosListado datos, bool hayBusqueda) {
    if (datos.items.isEmpty) {
      return [
        EstadoVacio(
          icono: Icons.groups_outlined,
          titulo: hayBusqueda ? 'Sin resultados' : 'Todavía no tienes alumnos asignados',
          texto: hayBusqueda
              ? 'Prueba con otro nombre.'
              : 'Cuando la escuela te asigne alumnos, aparecerán aquí.',
        ),
      ];
    }
    return [
      for (final alumno in datos.items) ...[
        _TarjetaAlumno(alumno: alumno),
        const SizedBox(height: 10),
      ],
      if (datos.errorAlCargarMas != null) ...[
        const AvisoCockpit(
          texto: 'No pudimos cargar más alumnos. Inténtalo de nuevo.',
          tono: TonoAviso.error,
        ),
        const SizedBox(height: 8),
      ],
      if (datos.hayMas)
        OutlinedButton(
          key: const Key('botonCargarMasAlumnos'),
          onPressed: datos.cargandoMas
              ? null
              : () => ref.read(misAlumnosProvider.notifier).cargarMas(),
          child: datos.cargandoMas
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text(datos.errorAlCargarMas != null ? 'Reintentar' : 'Cargar más'),
        ),
    ];
  }
}

class _TarjetaAlumno extends StatelessWidget {
  final HoraAlumno alumno;

  const _TarjetaAlumno({required this.alumno});

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Card(
      child: InkWell(
        key: Key('alumno-${alumno.estudianteCodigo}'),
        borderRadius: BorderRadius.circular(16),
        onTap: () => context.push('/students/${alumno.estudianteCodigo}'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(alumno.nombreCompleto, style: tema.textTheme.titleMedium),
              const SizedBox(height: 10),
              Row(
                children: [
                  _Dato('Comprado', alumno.horasCompradas),
                  _Dato('Conmigo', alumno.horasGastadasConmigo),
                  _Dato('Restantes', alumno.horasRestantes),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Un dato de horas: el número grande y su nombre debajo.
class _Dato extends StatelessWidget {
  final String etiqueta;
  final int horas;

  const _Dato(this.etiqueta, this.horas);

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '$horas h',
            style: tema.textTheme.titleMedium?.copyWith(
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
