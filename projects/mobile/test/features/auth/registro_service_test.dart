import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/models/registro_estudiante_request.dart';
import 'package:mobile/features/auth/services/registro_service.dart';

/// Adaptador falso: responde un cuerpo fijo y guarda lo último que recibió.
class _AdaptadorFalso implements HttpClientAdapter {
  final int status;
  final Map<String, dynamic> cuerpo;
  RequestOptions? ultimaPeticion;

  _AdaptadorFalso(this.status, this.cuerpo);

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    ultimaPeticion = options;
    return ResponseBody.fromString(
      jsonEncode(cuerpo),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

void main() {
  late RegistroService servicio;

  final request = RegistroEstudianteRequest(
    nombres: 'Ana',
    apellidos: 'Pérez',
    fechaNacimiento: DateTime(2000, 1, 5),
    email: 'ana@gmail.com',
    password: '123456789',
    escuelaSlug: 'escuelaexample',
    ci: 'AB12345',
    sexo: 'FEMENINO',
    latitud: -16.4897,
    longitud: -68.1193,
  );

  setUp(() {
    dotenv.loadFromString(envString: 'API_BASE_URL=http://localhost:8080/api/v1');
    servicio = RegistroService();
  });

  test('envía el cuerpo esperado, con la fecha yyyy-MM-dd y sin rol', () async {
    final adaptador = _AdaptadorFalso(201, {
      'status': 'success',
      'data': {'codigo': 'u1', 'email': 'ana@gmail.com', 'nombres': 'Ana', 'apellidos': 'Pérez'},
      'message': 'ok',
    });
    ApiClient.instance.httpClientAdapter = adaptador;

    await servicio.registrar(request);

    expect(adaptador.ultimaPeticion!.method, 'POST');
    expect(adaptador.ultimaPeticion!.path, '/usuarios');
    final cuerpo = adaptador.ultimaPeticion!.data as Map<String, dynamic>;
    expect(cuerpo['fechaNacimiento'], '2000-01-05');
    expect(cuerpo.containsKey('rol'), isFalse);
    expect(cuerpo.containsKey('telefono'), isFalse);
    expect(cuerpo['escuelaSlug'], 'escuelaexample');
  });

  test('envía el CI, el sexo y la ubicación, y nunca la dirección ni la categoría', () async {
    final adaptador = _AdaptadorFalso(201, {
      'status': 'success',
      'data': {'codigo': 'u1', 'email': 'ana@gmail.com', 'nombres': 'Ana', 'apellidos': 'Pérez'},
      'message': 'ok',
    });
    ApiClient.instance.httpClientAdapter = adaptador;

    await servicio.registrar(request);

    final cuerpo = adaptador.ultimaPeticion!.data as Map<String, dynamic>;
    expect(cuerpo['ci'], 'AB12345');
    expect(cuerpo['sexo'], 'FEMENINO');
    expect(cuerpo['latitud'], -16.4897);
    expect(cuerpo['longitud'], -68.1193);
    expect(cuerpo.containsKey('direccion'), isFalse);
    expect(cuerpo.containsKey('categoriaSigla'), isFalse);
  });

  test('un 409 con ERR_IDE_19 llega como ApiException con su código', () async {
    ApiClient.instance.httpClientAdapter = _AdaptadorFalso(409, {
      'status': 'failed',
      'data': null,
      'message': 'Ya existe una cuenta con ese CI',
      'error': 'ERR_IDE_19',
    });

    expect(
      servicio.registrar(request),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_IDE_19')),
    );
  });

  test('parsea la cuenta creada', () async {
    ApiClient.instance.httpClientAdapter = _AdaptadorFalso(201, {
      'status': 'success',
      'data': {'codigo': 'u1', 'email': 'ana@gmail.com', 'nombres': 'Ana', 'apellidos': 'Pérez'},
      'message': 'ok',
    });

    final usuario = await servicio.registrar(request);

    expect(usuario.codigo, 'u1');
    expect(usuario.email, 'ana@gmail.com');
  });

  test('un 409 con ERR_IDE_01 llega como ApiException con su código', () async {
    ApiClient.instance.httpClientAdapter = _AdaptadorFalso(409, {
      'status': 'failed',
      'data': null,
      'message': 'Ya existe una cuenta con ese correo',
      'error': 'ERR_IDE_01',
    });

    expect(
      servicio.registrar(request),
      throwsA(isA<ApiException>().having((e) => e.code, 'code', 'ERR_IDE_01')),
    );
  });
}
