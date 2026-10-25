import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/abrir_pagina_de_pago.dart';

/// Quien abre la página de pago de la pasarela. Es un provider para sustituirlo en los tests: ningún test abre
/// un navegador de verdad.
final abridorDePaginaProvider = Provider<Future<bool> Function(String url)>((ref) => abrirPaginaDePago);
