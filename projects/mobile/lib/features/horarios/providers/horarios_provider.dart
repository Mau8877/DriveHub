import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/crear_plantilla_horario_request.dart';
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

/// Los horarios del instructor: solo los activos, o también los inactivos si así se pidió.
///
/// * Pide la página 1 de 50: un instructor tiene muy pocos horarios (decisión de diseño del CU12).
final horariosListProvider = FutureProvider.autoDispose<List<PlantillaHorario>>((ref) async {
  final incluirInactivos = ref.watch(horariosIncluirInactivosUiProvider);
  final pagina = await ref.watch(horariosServiceProvider).listarPlantillas(
    activa: incluirInactivos ? null : true,
    pageSize: 50,
  );
  return pagina.items;
});

/// Crear y desactivar horarios (CU12). No guarda datos: al terminar con éxito invalida las listas
/// de horarios y de franjas (ambas cambian).
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
