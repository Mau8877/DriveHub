import 'package:dio/dio.dart';

import 'package:mobile/features/notificaciones/models/preferencias_notificacion.dart';
import 'package:mobile/features/notificaciones/services/preferencias_service.dart';
import 'package:mobile/features/perfil/models/perfil.dart';
import 'package:mobile/features/perfil/services/perfil_service.dart';

/// El perfil de la cuenta de prueba: de él sale el correo que se manda al guardar.
class PerfilFalso extends PerfilService {
  PerfilFalso() : super(dio: Dio());

  @override
  Future<Perfil> obtener() async => Perfil(
    codigo: 'u1',
    nombres: 'Ana',
    apellidos: 'Pérez',
    fechaNacimiento: DateTime(2000, 1, 31),
    email: 'ana@gmail.com',
    correoVerificado: true,
    roles: const ['ESTUDIANTE'],
    ci: 'AB12345',
    sexo: 'FEMENINO',
  );
}

/// Servicio de preferencias controlado desde el test.
class ServicioPreferenciasFalso extends PreferenciasService {
  ServicioPreferenciasFalso() : super(dio: Dio());

  PreferenciasNotificacion guardadas = PreferenciasNotificacion.porDefecto();
  Object? errorAlObtener;
  Object? errorAlGuardar;
  int obtenciones = 0;
  PreferenciasNotificacion? enviadas;
  String? correoEnviado;

  @override
  Future<PreferenciasNotificacion> obtener() async {
    obtenciones++;
    if (errorAlObtener != null) throw errorAlObtener!;
    return guardadas;
  }

  @override
  Future<PreferenciasNotificacion> guardar(
    PreferenciasNotificacion preferencias, {
    required String email,
  }) async {
    if (errorAlGuardar != null) throw errorAlGuardar!;
    enviadas = preferencias;
    correoEnviado = email;
    return guardadas = preferencias;
  }
}
