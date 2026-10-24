import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/crear_plantilla_horario_request.dart';
import '../models/estado_plantilla.dart';
import '../models/plantilla_horario.dart';
import '../models/plantilla_horario_creada.dart';
import 'franjas_provider.dart';
import 'horarios_service_provider.dart';

/// Si el listado incluye los horarios inactivos (estado puro de UI). No por defecto.
class IncluirInactivosNotifier extends Notifier<bool> {
  @override
  bool build() => false;

  void fijar(bool incluir) => state = incluir;
}

final horariosIncluirInactivosUiProvider = NotifierProvider<IncluirInactivosNotifier, bool>(
  IncluirInactivosNotifier.new,
);

/// Las dos pestañas de la sección «Horarios».
enum PestanaHorarios { plantillas, franjas }

/// La pestaña que se está viendo (estado puro de UI). Plantillas al abrir.
class PestanaHorariosNotifier extends Notifier<PestanaHorarios> {
  @override
  PestanaHorarios build() => PestanaHorarios.plantillas;

  void elegir(PestanaHorarios pestana) => state = pestana;
}

final horariosPestanaUiProvider = NotifierProvider<PestanaHorariosNotifier, PestanaHorarios>(
  PestanaHorariosNotifier.new,
);

/// Los horarios del instructor: activos y pausados, y también los inactivos si así se pidió.
///
/// * Pide la página 1 de 50: un instructor tiene muy pocos horarios (decisión de diseño del CU12).
final horariosListProvider = FutureProvider.autoDispose<List<PlantillaHorario>>((ref) async {
  final incluirInactivos = ref.watch(horariosIncluirInactivosUiProvider);
  final pagina = await ref
      .watch(horariosServiceProvider)
      .listarPlantillas(
        estados: {EstadoPlantilla.activa, EstadoPlantilla.pausada, if (incluirInactivos) EstadoPlantilla.inactiva},
        pageSize: 50,
      );
  return pagina.items;
});

/// Crear, pausar, reanudar y desactivar horarios (CU12). No guarda datos: al terminar con éxito invalida
/// las listas de horarios y de franjas (ambas cambian).
///
/// * Los errores (`ApiException` o de red) se propagan al llamador: la pantalla decide cómo mostrarlos.
class HorariosNotifier extends Notifier<void> {
  @override
  void build() {}

  Future<PlantillaHorarioCreada> crear(CrearPlantillaHorarioRequest request) async {
    final creada = await ref.read(horariosServiceProvider).crearPlantilla(request);
    _refrescar();
    return creada;
  }

  Future<void> pausar(String codigo) async {
    await ref.read(horariosServiceProvider).pausarPlantilla(codigo);
    _refrescar();
  }

  Future<void> reanudar(String codigo) async {
    await ref.read(horariosServiceProvider).reanudarPlantilla(codigo);
    _refrescar();
  }

  Future<void> desactivar(String codigo) async {
    await ref.read(horariosServiceProvider).desactivarPlantilla(codigo);
    _refrescar();
  }

  void _refrescar() {
    ref.invalidate(horariosListProvider);
    ref.invalidate(franjasListProvider);
  }
}

final horariosProvider = NotifierProvider<HorariosNotifier, void>(HorariosNotifier.new);
