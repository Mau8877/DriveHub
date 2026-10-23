import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/auth/providers/sesion_provider.dart';
import 'estudiante/estudiante_shell.dart';
import 'instructor/instructor_shell.dart';

/// Elige el shell de `/inicio` según el rol de la sesión.
///
/// * Si los roles cambian al renovarse la sesión, el shell se reemplaza solo.
/// * ? Una cuenta con los dos roles entra como Instructor: falta definir si elige.
class InicioPorRol extends ConsumerWidget {
  const InicioPorRol({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final roles = ref.watch(sesionProvider).usuario?.roles ?? const [];
    return roles.contains('INSTRUCTOR')
        ? const InstructorShell()
        : const EstudianteShell();
  }
}
