import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';

import '../helpers/adaptador_en_cola.dart';

void main() {
  late String? token;
  late int renovaciones;
  late String? tokenAlRenovar;

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    token = 'token-viejo';
    renovaciones = 0;
    tokenAlRenovar = 'token-nuevo';
    ApiClient.configurarSesion(
      obtenerToken: () => token,
      // * Como la sesión real: al renovar, su token pasa a ser el nuevo
      renovar: () async {
        renovaciones++;
        token = tokenAlRenovar;
        return tokenAlRenovar;
      },
    );
  });

  String? bearerDe(AdaptadorEnCola a, int i) => a.peticiones[i].headers['Authorization'] as String?;

  test('agrega el Bearer a las llamadas de negocio', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, exito({'x': 1}))]);
    ApiClient.instance.httpClientAdapter = adaptador;

    await ApiClient.instance.get<dynamic>('/estudiantes');

    expect(bearerDe(adaptador, 0), 'Bearer token-viejo');
  });

  test('no agrega el Bearer a /auth/', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, exito({'x': 1}))]);
    ApiClient.instance.httpClientAdapter = adaptador;

    await ApiClient.instance.post<dynamic>('/auth/login', data: {});

    expect(bearerDe(adaptador, 0), isNull);
  });

  test('agrega el Bearer a /auth/verificar-correo, que exige sesión', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, exito({}))]);
    ApiClient.instance.httpClientAdapter = adaptador;

    await ApiClient.instance.post<dynamic>('/auth/verificar-correo');

    expect(bearerDe(adaptador, 0), 'Bearer token-viejo');
  });

  test('ante un 401 renueva una vez y repite con el token nuevo', () async {
    final adaptador = AdaptadorEnCola([
      RespuestaFalsa(401, fallo('ERR_SYS_04')),
      RespuestaFalsa(200, exito({'x': 1})),
    ]);
    ApiClient.instance.httpClientAdapter = adaptador;

    final respuesta = await ApiClient.instance.get<dynamic>('/estudiantes');

    expect(respuesta.data, {'x': 1});
    expect(renovaciones, 1);
    expect(adaptador.peticiones, hasLength(2));
    expect(bearerDe(adaptador, 1), 'Bearer token-nuevo');
  });

  test('si no se puede renovar propaga el error original y no repite', () async {
    tokenAlRenovar = null;
    final adaptador = AdaptadorEnCola([RespuestaFalsa(401, fallo('ERR_SYS_04'))]);
    ApiClient.instance.httpClientAdapter = adaptador;

    await expectLater(
      ApiClient.instance.get<dynamic>('/estudiantes'),
      throwsA(predicate((e) => e is Exception && e.toString().contains('ERR_SYS_04'))),
    );

    expect(adaptador.peticiones, hasLength(1));
  });

  test('un segundo 401 tras el reintento no entra en bucle', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(401, fallo('ERR_SYS_04'))]);
    ApiClient.instance.httpClientAdapter = adaptador;

    await expectLater(ApiClient.instance.get<dynamic>('/estudiantes'), throwsA(anything));

    expect(renovaciones, 1);
    expect(adaptador.peticiones, hasLength(2));
  });

  test('un 401 de /auth/ no dispara renovación', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(401, fallo('ERR_IDE_04'))]);
    ApiClient.instance.httpClientAdapter = adaptador;

    await expectLater(ApiClient.instance.post<dynamic>('/auth/login', data: {}), throwsA(anything));

    expect(renovaciones, 0);
    expect(adaptador.peticiones, hasLength(1));
  });
}
