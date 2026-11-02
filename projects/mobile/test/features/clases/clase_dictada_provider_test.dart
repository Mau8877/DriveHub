import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/clases/models/asistencia_clase.dart';
import 'package:mobile/features/clases/providers/clase_dictada_provider.dart';

import '../../helpers/clases_falsos.dart';

void main() {
  late ClasesServiceFalso servicio;

  setUp(() => servicio = ClasesServiceFalso());

  ProviderContainer crear() {
    final container = ProviderContainer(overrides: [clasesServiceFalso(servicio)]);
    addTearDown(container.dispose);
    return container;
  }

  test('pide la clase por su código y entrega lo guardado', () async {
    servicio.claseGuardada = claseDictadaDePrueba(
      codigo: 'c7',
      asistencia: AsistenciaClase.tarde,
      tema: 'Giros',
      kmInicio: 100,
    );

    final clase = await crear().read(claseDictadaDetailProvider('c7').future);

    expect(servicio.clasesDictadasPedidas, ['c7']);
    expect(clase.codigo, 'c7');
    expect(clase.asistencia, AsistenciaClase.tarde);
    expect(clase.tema, 'Giros');
    expect(clase.kmInicio, 100);
  });

  test('cada código tiene su propio pedido', () async {
    final container = crear();

    await container.read(claseDictadaDetailProvider('a').future);
    await container.read(claseDictadaDetailProvider('b').future);

    expect(servicio.clasesDictadasPedidas, ['a', 'b']);
  });

  test('un error de negocio queda en el AsyncValue', () async {
    servicio.errorClaseDictada = ApiException(code: 'ERR_ACA_35', message: 'No encontramos esa clase');
    final container = crear();
    container.listen(claseDictadaDetailProvider('c1'), (_, _) {});

    await expectLater(
      container.read(claseDictadaDetailProvider('c1').future),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_ACA_35')),
    );

    expect(container.read(claseDictadaDetailProvider('c1')).hasError, isTrue);
  });

  test('un error de red queda en el AsyncValue y se puede reintentar', () async {
    servicio.errorClaseDictada = DioException(requestOptions: RequestOptions(path: '/clases/c1'));
    final container = crear();
    container.listen(claseDictadaDetailProvider('c1'), (_, _) {});

    await expectLater(container.read(claseDictadaDetailProvider('c1').future), throwsA(isA<DioException>()));

    servicio.errorClaseDictada = null;
    container.invalidate(claseDictadaDetailProvider('c1'));
    expect((await container.read(claseDictadaDetailProvider('c1').future)).codigo, 'c1');
  });
}
