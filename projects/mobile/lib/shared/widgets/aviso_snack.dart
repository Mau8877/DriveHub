import 'package:flutter/material.dart';

/// Muestra un aviso breve (snackbar) con el texto dado, reemplazando el que hubiera.
void avisar(BuildContext context, String texto) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(texto)));
}
