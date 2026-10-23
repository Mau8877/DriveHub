import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/horarios_service.dart';

/// Servicio de horarios y franjas; es un provider aparte para poder sustituirlo en tests.
final horariosServiceProvider = Provider<HorariosService>((ref) => HorariosService());
