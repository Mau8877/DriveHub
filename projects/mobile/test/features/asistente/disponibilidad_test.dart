import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/models/catalogo_acciones.dart';
import 'package:mobile/features/asistente/models/estado_asistente.dart';
import 'package:mobile/features/asistente/providers/asistente_provider.dart';
import 'package:mobile/features/asistente/providers/modelo_de_acciones_provider.dart';
import 'package:mobile/features/asistente/providers/reconocedor_de_voz_provider.dart';
import 'package:mobile/features/asistente/services/reconocedor_de_voz.dart';

import 'asistente_test.dart';

void main() {
  late VozFalsa voz;

  setUp(() => voz = VozFalsa());

  /// El asistente con el modelo REAL de acciones apuntando a [ruta]: sin el archivo, nunca toca el plugin nativo.
  ProviderContainer crearConModeloReal(String ruta) {
    final catalogo = CatalogoAcciones.desdeJson(File(CatalogoAcciones.rutaAsset).readAsStringSync());
    final container = ProviderContainer(
      overrides: [
        reconocedorDeVozProvider.overrideWith((ref) async => voz),
        catalogoAccionesProvider.overrideWith((ref) async => catalogo),
        rutaModeloDeAccionesProvider.overrideWith((ref) async => ruta),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  group('modelo de acciones ausente (A12)', () {
    test('la persona ve que no está disponible y la app no se cierra', () async {
      final c = crearConModeloReal('${Directory.systemTemp.path}/no_existe_${DateTime.now().microsecondsSinceEpoch}.litertlm');
      final n = c.read(asistenteProvider.notifier);

      await n.empezarAEscuchar();
      await n.terminarDeEscuchar();

      final estado = c.read(asistenteProvider);
      expect(estado, isA<Fallo>());
      expect((estado as Fallo).mensaje, 'El asistente no está disponible en este teléfono.');
    });

    test('se puede volver a intentar sin reiniciar nada', () async {
      final c = crearConModeloReal('${Directory.systemTemp.path}/no_existe_otra_vez.litertlm');
      final n = c.read(asistenteProvider.notifier);
      await n.empezarAEscuchar();
      await n.terminarDeEscuchar();

      await n.cerrar();
      await n.empezarAEscuchar();
      await n.terminarDeEscuchar();

      expect(c.read(asistenteProvider), isA<Fallo>());
      expect(voz.empezadas, 2);
    });
  });

  group('micrófono (A11)', () {
    test('permiso negado: el estado lo dice y no queda nada grabando', () async {
      voz.errorAlEmpezar = const PermisoDeMicrofonoDenegado();
      final c = crearConModeloReal('x');
      final n = c.read(asistenteProvider.notifier);

      await n.empezarAEscuchar();
      await n.terminarDeEscuchar();

      expect(c.read(asistenteProvider), isA<SinPermiso>());
      expect(voz.terminadas, 0);
    });

    test('permiso concedido después: vuelve a funcionar al intentar de nuevo', () async {
      voz.errorAlEmpezar = const PermisoDeMicrofonoDenegado();
      final c = crearConModeloReal('x');
      final n = c.read(asistenteProvider.notifier);
      await n.empezarAEscuchar();
      expect(c.read(asistenteProvider), isA<SinPermiso>());

      voz.errorAlEmpezar = null;
      await n.cerrar();
      await n.empezarAEscuchar();

      expect(c.read(asistenteProvider), isA<Escuchando>());
    });
  });

  test('el modelo de voz ausente se avisa al terminar de hablar', () async {
    voz.errorAlTerminar = const ModeloDeVozNoDisponible('sin whisper');
    final c = crearConModeloReal('x');
    final n = c.read(asistenteProvider.notifier);

    await n.empezarAEscuchar();
    await n.terminarDeEscuchar();

    expect((c.read(asistenteProvider) as Fallo).mensaje, contains('voz no está disponible'));
  });
}
