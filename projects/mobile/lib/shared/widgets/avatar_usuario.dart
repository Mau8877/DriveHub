import 'package:flutter/material.dart';

/// Iniciales (hasta dos) de nombres y apellidos, en mayúscula.
String inicialesDe(String nombres, String apellidos) {
  String inicial(String texto) {
    final limpio = texto.trim();
    return limpio.isEmpty ? '' : limpio.substring(0, 1).toUpperCase();
  }

  return '${inicial(nombres)}${inicial(apellidos)}';
}

/// Avatar de la persona (CU06): su foto o, sin foto o si la imagen falla (por ejemplo la URL
/// prefirmada venció), las iniciales de nombres y apellidos con los colores del tema.
///
/// * Es el primer widget compartido: lo usan la barra del `UsuarioShell` y la pantalla de perfil.
class AvatarUsuario extends StatelessWidget {
  final String nombres;
  final String apellidos;
  final String? fotoUrl;
  final double radio;

  const AvatarUsuario({
    super.key,
    required this.nombres,
    required this.apellidos,
    this.fotoUrl,
    this.radio = 20,
  });

  @override
  Widget build(BuildContext context) {
    final colores = Theme.of(context).colorScheme;
    final iniciales = Container(
      width: radio * 2,
      height: radio * 2,
      alignment: Alignment.center,
      color: colores.primary,
      child: Text(
        inicialesDe(nombres, apellidos),
        style: TextStyle(
          color: colores.onPrimary,
          fontWeight: FontWeight.w600,
          fontSize: radio * 0.8,
        ),
      ),
    );

    return Semantics(
      label: 'Foto de perfil',
      child: ClipOval(
        child: SizedBox(
          width: radio * 2,
          height: radio * 2,
          child: fotoUrl == null
              ? iniciales
              : Image.network(
                  fotoUrl!,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stack) => iniciales,
                ),
        ),
      ),
    );
  }
}
