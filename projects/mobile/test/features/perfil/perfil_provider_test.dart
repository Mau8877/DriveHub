import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/core/network/api_client.dart';
import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';
import 'package:mobile/features/perfil/models/cambiar_contrasena_request.dart';
import 'package:mobile/features/perfil/models/cambiar_correo_request.dart';
import 'package:mobile/features/perfil/models/editar_perfil_request.dart';
import 'package:mobile/features/perfil/models/perfil.dart';
import 'package:mobile/features/perfil/providers/perfil_provider.dart';
import 'package:mobile/features/perfil/services/perfil_service.dart';

import '../../helpers/sesion_falsa.dart';

Perfil _perfil({
  String nombres = 'Ana',
  String? fotoUrl,
  String email = 'ana@gmail.com',
}) => Perfil(
  codigo: 'u1',
  nombres: nombres,
  apellidos: 'Pérez',
  fechaNacimiento: DateTime(2000, 1, 31),
  email: email,
  correoVerificado: true,
  roles: const ['ESTUDIANTE'],
  fotoUrl: fotoUrl,
);

class _ServicioFalso extends PerfilService {
  _ServicioFalso() : super(dio: Dio());

  Perfil actual = _perfil();
  int obtenciones = 0;
  Object? errorAlEditar;
  Object? errorAlObtener;
  CambiarContrasenaRequest? contrasena;
  CambiarCorreoRequest? correo;

  @override
  Future<Perfil> obtener() async {
    obtenciones++;
    if (errorAlObtener != null) throw errorAlObtener!;
    return actual;
  }

  @override
  Future<Perfil> editar(EditarPerfilRequest request) async {
    if (errorAlEditar != null) throw errorAlEditar!;
    return actual = _perfil(
      nombres: request.nombres ?? actual.nombres,
      fotoUrl: actual.fotoUrl,
    );
  }

  @override
  Future<Perfil> subirFoto(
    Uint8List bytes, {
    required String nombre,
    required String tipo,
  }) async {
    return actual = _perfil(fotoUrl: 'http://minio/nueva.png');
  }

  @override
  Future<Perfil> quitarFoto() async => actual = _perfil();

  @override
  Future<void> cambiarContrasena(CambiarContrasenaRequest request) async =>
      contrasena = request;

  @override
  Future<void> solicitarCambioCorreo(CambiarCorreoRequest request) async =>
      correo = request;
}

class _SesionConRegistro extends SesionFalsa {
  final cambios = <Map<String, Object?>>[];

  _SesionConRegistro() : super(const EstadoSesion.conSesion(usuarioDePrueba));

  @override
  Future<void> actualizarUsuario({
    String? nombres,
    String? apellidos,
    String? fotoUrl,
    bool quitarFoto = false,
  }) async {
    cambios.add({
      'nombres': nombres,
      'apellidos': apellidos,
      'fotoUrl': fotoUrl,
      'quitarFoto': quitarFoto,
    });
  }
}

void main() {
  late _ServicioFalso servicio;
  late _SesionConRegistro sesion;
  late ProviderContainer contenedor;

  setUp(() {
    servicio = _ServicioFalso();
    sesion = _SesionConRegistro();
    contenedor = ProviderContainer(
      overrides: [
        perfilServiceProvider.overrideWithValue(servicio),
        sesionFalsa(sesion),
      ],
    );
    addTearDown(contenedor.dispose);
  });

  Future<PerfilNotifier> cargado() async {
    await contenedor.read(perfilProvider.future);
    return contenedor.read(perfilProvider.notifier);
  }

  test('si entra otra persona, vuelve a pedir el perfil en vez de mostrar el anterior', () async {
    final anterior = await contenedor.read(perfilProvider.future);
    expect(anterior.nombres, 'Ana');
    expect(servicio.obtenciones, 1);

    servicio.actual = _perfil(nombres: 'Carlos');
    sesion.cambiar(
      const EstadoSesion.conSesion(
        UsuarioSesion(codigo: 'u2', nombres: 'Carlos', apellidos: 'Otsubo', roles: ['ESTUDIANTE']),
      ),
    );

    final nuevo = await contenedor.read(perfilProvider.future);
    expect(nuevo.nombres, 'Carlos');
    expect(servicio.obtenciones, 2);
  });

  test('editar los datos no vuelve a pedir el perfil', () async {
    final notifier = await cargado();

    await notifier.editar(const EditarPerfilRequest(nombres: 'Anita'));

    expect(servicio.obtenciones, 1);
  });

  test('build carga el perfil', () async {
    final perfil = await contenedor.read(perfilProvider.future);

    expect(perfil.email, 'ana@gmail.com');
    expect(servicio.obtenciones, 1);
  });

  test(
    'editar actualiza el estado y la sesión con los nombres devueltos',
    () async {
      final notifier = await cargado();

      await notifier.editar(const EditarPerfilRequest(nombres: 'Luisa'));

      expect(contenedor.read(perfilProvider).value!.nombres, 'Luisa');
      expect(sesion.cambios.single['nombres'], 'Luisa');
      expect(sesion.cambios.single['quitarFoto'], isTrue); // sin foto
    },
  );

  test('subirFoto actualiza el estado y la sesión con la fotoUrl', () async {
    final notifier = await cargado();

    await notifier.subirFoto(
      Uint8List.fromList([1]),
      nombre: 'f.png',
      tipo: 'image/png',
    );

    expect(
      contenedor.read(perfilProvider).value!.fotoUrl,
      'http://minio/nueva.png',
    );
    expect(sesion.cambios.single['fotoUrl'], 'http://minio/nueva.png');
    expect(sesion.cambios.single['quitarFoto'], isFalse);
  });

  test(
    'quitarFoto deja el perfil sin foto y quita la foto de la sesión',
    () async {
      servicio.actual = _perfil(fotoUrl: 'http://minio/f.png');
      final notifier = await cargado();

      await notifier.quitarFoto();

      expect(contenedor.read(perfilProvider).value!.fotoUrl, isNull);
      expect(sesion.cambios.single['quitarFoto'], isTrue);
    },
  );

  test(
    'un ApiException al editar se relanza y el estado conserva el perfil anterior',
    () async {
      final notifier = await cargado();
      servicio.errorAlEditar = ApiException(
        code: 'ERR_IDE_02',
        message: 'menor',
      );

      await expectLater(
        notifier.editar(const EditarPerfilRequest(nombres: 'Luisa')),
        throwsA(isA<ApiException>()),
      );

      expect(contenedor.read(perfilProvider).value!.nombres, 'Ana');
      expect(contenedor.read(perfilProvider).hasError, isFalse);
      expect(sesion.cambios, isEmpty);
    },
  );

  test(
    'cambiarContrasena y solicitarCambioCorreo no cambian el perfil ni la sesión',
    () async {
      final notifier = await cargado();

      await notifier.cambiarContrasena(
        const CambiarContrasenaRequest(
          passwordActual: 'a',
          passwordNueva: 'nueva-clave-1',
        ),
      );
      await notifier.solicitarCambioCorreo(
        const CambiarCorreoRequest(
          passwordActual: 'a',
          correoNuevo: 'n@gmail.com',
        ),
      );

      expect(servicio.contrasena!.passwordNueva, 'nueva-clave-1');
      expect(servicio.correo!.correoNuevo, 'n@gmail.com');
      expect(sesion.cambios, isEmpty);
      expect(servicio.obtenciones, 1);
    },
  );

  test('recargar vuelve a pedir el perfil', () async {
    final notifier = await cargado();
    servicio.actual = _perfil(email: 'nuevo@gmail.com');

    await notifier.recargar();

    expect(servicio.obtenciones, 2);
    expect(contenedor.read(perfilProvider).value!.email, 'nuevo@gmail.com');
  });

  test('recargar sin red conserva el perfil que había', () async {
    final notifier = await cargado();
    servicio.errorAlObtener = Exception('sin red');

    await notifier.recargar();

    expect(contenedor.read(perfilProvider).value!.email, 'ana@gmail.com');
  });

  test('correoCambio compara sin importar mayúsculas ni espacios', () {
    expect(correoCambio('ana@gmail.com', ' ANA@gmail.com '), isFalse);
    expect(correoCambio('ana@gmail.com', 'nuevo@gmail.com'), isTrue);
  });
}
