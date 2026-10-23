import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'dart:typed_data';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/perfil/models/cambiar_contrasena_request.dart';
import 'package:mobile/features/perfil/models/editar_perfil_request.dart';
import 'package:mobile/features/perfil/services/perfil_service.dart';

import '../../helpers/adaptador_en_cola.dart';

Map<String, dynamic> _perfil() => exito({
  'codigo': 'u1',
  'nombres': 'Ana',
  'apellidos': 'Pérez',
  'fechaNacimiento': '2000-01-31',
  'telefono': null,
  'email': 'ana@gmail.com',
  'correoVerificado': true,
  'roles': ['ESTUDIANTE'],
  'escuelaCodigo': null,
  'escuelaNombre': null,
  'fotoUrl': null,
  'ci': 'AB12345',
  'sexo': 'FEMENINO',
  'direccion': null,
  'latitud': null,
  'longitud': null,
  'categoriaSigla': null,
});

/// Un Dio con el desempaquetado del sobre como el real, pero sin red.
Dio _dio(AdaptadorEnCola adaptador) {
  final dio = Dio(BaseOptions(baseUrl: 'http://api.test/api/v1'));
  dio.httpClientAdapter = adaptador;
  dio.interceptors.add(
    InterceptorsWrapper(
      onResponse: (response, handler) {
        response.data = (response.data as Map<String, dynamic>)['data'];
        handler.next(response);
      },
      onError: (error, handler) {
        final envelope = error.response?.data;
        if (envelope is Map<String, dynamic> && envelope['error'] != null) {
          handler.reject(
            DioException(
              requestOptions: error.requestOptions,
              error: ApiException(
                code: envelope['error'] as String,
                message: envelope['message'] as String,
              ),
              response: error.response,
            ),
          );
          return;
        }
        handler.next(error);
      },
    ),
  );
  return dio;
}

void main() {
  test('obtener hace GET /usuarios/me y devuelve el perfil', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, _perfil())]);
    final servicio = PerfilService(dio: _dio(adaptador));

    final perfil = await servicio.obtener();

    expect(perfil.email, 'ana@gmail.com');
    expect(adaptador.peticiones.single.method, 'GET');
    expect(adaptador.peticiones.single.path, '/usuarios/me');
  });

  test('editar hace PATCH con solo los campos presentes', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, _perfil())]);
    final servicio = PerfilService(dio: _dio(adaptador));

    await servicio.editar(
      const EditarPerfilRequest(nombres: 'Luisa', borrarTelefono: true),
    );

    final peticion = adaptador.peticiones.single;
    expect(peticion.method, 'PATCH');
    expect(peticion.data, {'nombres': 'Luisa', 'telefono': null});
  });

  test('subirFoto hace PUT /usuarios/me/foto con la parte archivo', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, _perfil())]);
    final servicio = PerfilService(dio: _dio(adaptador));

    await servicio.subirFoto(
      Uint8List.fromList([1, 2, 3]),
      nombre: 'foto.png',
      tipo: 'image/png',
    );

    final peticion = adaptador.peticiones.single;
    expect(peticion.method, 'PUT');
    expect(peticion.path, '/usuarios/me/foto');
    final cuerpo = peticion.data as FormData;
    expect(cuerpo.files.single.key, 'archivo');
    expect(cuerpo.files.single.value.filename, 'foto.png');
  });

  test('quitarFoto hace DELETE /usuarios/me/foto', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, _perfil())]);
    final servicio = PerfilService(dio: _dio(adaptador));

    await servicio.quitarFoto();

    expect(adaptador.peticiones.single.method, 'DELETE');
    expect(adaptador.peticiones.single.path, '/usuarios/me/foto');
  });

  test('cambiarContrasena hace POST con dispositivo MOVIL', () async {
    final adaptador = AdaptadorEnCola([RespuestaFalsa(200, exito({}))]);
    final servicio = PerfilService(dio: _dio(adaptador));

    await servicio.cambiarContrasena(
      const CambiarContrasenaRequest(
        passwordActual: 'a',
        passwordNueva: 'nueva-clave-1',
      ),
    );

    final peticion = adaptador.peticiones.single;
    expect(peticion.path, '/usuarios/me/cambiar-contrasena');
    expect((peticion.data as Map)['dispositivo'], 'MOVIL');
  });

  test(
    'un rechazo del backend llega como ApiException con su código',
    () async {
      final adaptador = AdaptadorEnCola([
        RespuestaFalsa(400, fallo('ERR_IDE_14')),
      ]);
      final servicio = PerfilService(dio: _dio(adaptador));

      await expectLater(
        servicio.cambiarContrasena(
          const CambiarContrasenaRequest(
            passwordActual: 'mala',
            passwordNueva: 'nueva-clave-1',
          ),
        ),
        throwsA(
          isA<ApiException>().having((e) => e.code, 'code', 'ERR_IDE_14'),
        ),
      );
    },
  );
}
