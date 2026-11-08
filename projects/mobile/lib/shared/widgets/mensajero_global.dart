import 'package:flutter/material.dart';

/// El `ScaffoldMessenger` de toda la app. Sirve para mostrar un aviso breve sin tener un `BuildContext` (por ejemplo,
/// un aviso push que llega con la app abierta, CU36). `MaterialApp` lo recibe como `scaffoldMessengerKey`, y los
/// `ScaffoldMessenger.of(context)` de las pantallas siguen funcionando igual.
final GlobalKey<ScaffoldMessengerState> mensajeroGlobal = GlobalKey<ScaffoldMessengerState>();
