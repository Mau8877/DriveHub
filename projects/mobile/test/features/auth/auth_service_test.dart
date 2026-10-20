import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/services/auth_service.dart';

import '../../helpers/adaptador_en_cola.dart';

void main() {
  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    ApiClient.configurarSesion(obtenerToken: () => null, renovar: () async => null);
  });

  test('login envía correo, contraseña y dispositivo MOVIL, y parsea la sesión', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, sesionJson('jwt', 'refresh'))]);
    ApiClient.instance.httpClientAdapter = adaptador;

    final sesion = await AuthService().login('ana@gmail.com', 'clave12345');

    final cuerpo = adaptador.peticiones.single.data as Map<String, dynamic>;
    expect(adaptador.peticiones.single.path, '/auth/login');
    expect(cuerpo, {'email': 'ana@gmail.com', 'password': 'clave12345', 'dispositivo': 'MOVIL'});
    expect(sesion.accessToken, 'jwt');
    expect(sesion.refreshToken, 'refresh');
    expect(sesion.expiraEnSegundos, 900);
    expect(sesion.usuario.roles, ['ESTUDIANTE']);
    expect(sesion.usuario.escuelaCodigo, 'e1');
  });

  test('renovar envía el refresh token y el dispositivo', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, sesionJson('jwt2', 'refresh2'))]);
    ApiClient.instance.httpClientAdapter = adaptador;

    final sesion = await AuthService().renovar('refresh-viejo');

    expect(adaptador.peticiones.single.path, '/auth/refresh');
    expect(adaptador.peticiones.single.data, {'dispositivo': 'MOVIL', 'refreshToken': 'refresh-viejo'});
    expect(sesion.refreshToken, 'refresh2');
  });

  test('cerrar envía el refresh token y el dispositivo a /auth/logout', () async {
    final adaptador = AdaptadorEnCola([
      RespuestaFalsa(200, {'status': 'success', 'data': null, 'message': 'ok', 'timestamp': 'x'}),
    ]);
    ApiClient.instance.httpClientAdapter = adaptador;

    await AuthService().cerrar('refresh-actual');

    expect(adaptador.peticiones.single.path, '/auth/logout');
    expect(adaptador.peticiones.single.data, {'dispositivo': 'MOVIL', 'refreshToken': 'refresh-actual'});
  });

  test('solicitarVerificacionCorreo hace POST a /auth/verificar-correo sin cuerpo', () async {
    final adaptador = AdaptadorEnCola([
      RespuestaFalsa(200, {'status': 'success', 'data': null, 'message': 'ok', 'timestamp': 'x'}),
    ]);
    ApiClient.instance.httpClientAdapter = adaptador;

    await AuthService().solicitarVerificacionCorreo();

    expect(adaptador.peticiones.single.path, '/auth/verificar-correo');
    expect(adaptador.peticiones.single.method, 'POST');
    expect(adaptador.peticiones.single.data, isNull);
  });

  test('un 409 ERR_IDE_10 al pedir la verificación llega como ApiException', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorEnCola([RespuestaFalsa(409, fallo('ERR_IDE_10'))]);

    await expectLater(
      AuthService().solicitarVerificacionCorreo(),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_IDE_10')),
    );
  });

  test('un 401 ERR_IDE_04 y un 423 ERR_IDE_05 llegan como ApiException con su código', () async {
    ApiClient.instance.httpClientAdapter = AdaptadorEnCola([RespuestaFalsa(401, fallo('ERR_IDE_04'))]);
    await expectLater(
      AuthService().login('a@gmail.com', 'mal'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_IDE_04')),
    );

    ApiClient.instance.httpClientAdapter = AdaptadorEnCola([RespuestaFalsa(423, fallo('ERR_IDE_05'))]);
    await expectLater(
      AuthService().login('a@gmail.com', 'mal'),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_IDE_05')),
    );
  });
}
