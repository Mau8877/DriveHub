import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/features/servidor/services/probar_servidor.dart';

import '../../helpers/adaptador_con_red.dart';
import '../../helpers/adaptador_en_cola.dart';

void main() {
  Dio clienteCon(HttpClientAdapter adaptador) =>
      Dio(BaseOptions(baseUrl: 'http://192.168.1.10:8080/api/v1'))..httpClientAdapter = adaptador;

  test('un 404 con el sobre del gateway es un servidor de DriveHub', () async {
    final dio = clienteCon(AdaptadorEnCola([RespuestaFalsa(404, fallo('ERR_SYS_03'))]));

    expect(await probarServidor('http://192.168.1.10:8080/api/v1', dio: dio), ResultadoPrueba.servidorDriveHub);
  });

  test('un 401 con el sobre también lo es (el gateway pide sesión)', () async {
    final dio = clienteCon(AdaptadorEnCola([RespuestaFalsa(401, fallo('ERR_SYS_04'))]));

    expect(await probarServidor('http://192.168.1.10:8080/api/v1', dio: dio), ResultadoPrueba.servidorDriveHub);
  });

  test('un 200 con el sobre de éxito también', () async {
    final dio = clienteCon(AdaptadorEnCola([RespuestaFalsa(200, exito({}))]));

    expect(await probarServidor('http://192.168.1.10:8080/api/v1', dio: dio), ResultadoPrueba.servidorDriveHub);
  });

  test('un servidor que responde otra cosa no se toma por DriveHub', () async {
    final dio = clienteCon(AdaptadorEnCola([RespuestaFalsa(200, {'hola': 'mundo'})]));

    expect(await probarServidor('http://192.168.1.10:8080/api/v1', dio: dio), ResultadoPrueba.respondeOtraCosa);
  });

  test('un cuerpo con status pero sin timestamp no es el sobre de DriveHub', () async {
    final dio = clienteCon(AdaptadorEnCola([RespuestaFalsa(200, {'status': 'success'})]));

    expect(await probarServidor('http://192.168.1.10:8080/api/v1', dio: dio), ResultadoPrueba.respondeOtraCosa);
  });

  test('sin red no lanza: es sin respuesta', () async {
    final dio = clienteCon(AdaptadorConRed(conRed: false));

    expect(await probarServidor('http://192.168.1.10:8080/api/v1', dio: dio), ResultadoPrueba.sinRespuesta);
  });

  test('pide la raíz de la API sin llevar sesión', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(404, fallo('ERR_SYS_03'))]);
    final dio = clienteCon(adaptador);

    await probarServidor('http://192.168.1.10:8080/api/v1', dio: dio);

    expect(adaptador.peticiones.single.uri.toString(), 'http://192.168.1.10:8080/api/v1/');
    expect(adaptador.peticiones.single.headers['Authorization'], isNull);
  });
}
