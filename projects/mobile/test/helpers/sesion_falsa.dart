import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:mobile/features/auth/models/usuario_sesion.dart';
import 'package:mobile/features/auth/providers/sesion_provider.dart';

const usuarioDePrueba = UsuarioSesion(
  codigo: 'u1',
  nombres: 'Ana',
  apellidos: 'Pérez',
  roles: ['ESTUDIANTE'],
);

/// Sesión controlada desde el test: arranca en el estado dado y se puede cambiar.
class SesionFalsa extends SesionNotifier {
  final EstadoSesion inicial;
  Future<void> Function(String email, String password)? alIniciar;

  SesionFalsa(this.inicial);

  @override
  EstadoSesion build() => inicial;

  void cambiar(EstadoSesion nuevo) => state = nuevo;

  @override
  Future<void> iniciarSesion(String email, String password) async {
    await alIniciar?.call(email, password);
  }
}

Override sesionFalsa(SesionFalsa falsa) => sesionProvider.overrideWith(() => falsa);
