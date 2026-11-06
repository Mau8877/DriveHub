import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/asistente/services/resolvedor_de_datos.dart';
import 'package:mobile/features/clases/models/instructor_disponible.dart';
import 'package:mobile/features/clases/models/momento_dia.dart';

void main() {
  const resolvedor = ResolvedorDeDatos();

  // Martes 4 de noviembre de 2025
  final martes = DateTime(2025, 11, 4, 15, 30);

  group('resolverDia', () {
    test('hoy, mañana y pasado mañana', () {
      expect(resolvedor.resolverDia('hoy', martes), DateTime(2025, 11, 4));
      expect(resolvedor.resolverDia('manana', martes), DateTime(2025, 11, 5));
      expect(resolvedor.resolverDia('pasado_manana', martes), DateTime(2025, 11, 6));
    });

    test('un día de esta semana que aún no llegó', () {
      expect(resolvedor.resolverDia('viernes', martes), DateTime(2025, 11, 7));
    });

    test('un día que ya pasó esta semana va a la próxima', () {
      expect(resolvedor.resolverDia('lunes', martes), DateTime(2025, 11, 10));
    });

    test('el mismo día de la semana que hoy es hoy', () {
      expect(resolvedor.resolverDia('martes', martes), DateTime(2025, 11, 4));
    });

    test('cruza de mes y de año', () {
      expect(resolvedor.resolverDia('manana', DateTime(2025, 12, 31)), DateTime(2026, 1, 1));
      expect(resolvedor.resolverDia('domingo', DateTime(2025, 12, 30)), DateTime(2026, 1, 4));
    });

    test('descarta la hora de hoy', () {
      expect(resolvedor.resolverDia('hoy', DateTime(2025, 11, 4, 23, 59))?.hour, 0);
    });

    test('un día que no es del catálogo da null', () {
      expect(resolvedor.resolverDia('jueveees', martes), isNull);
      expect(resolvedor.resolverDia('', martes), isNull);
    });
  });

  group('resolverMomento', () {
    test('mañana, tarde y noche', () {
      expect(resolvedor.resolverMomento('manana'), MomentoDia.manana);
      expect(resolvedor.resolverMomento('tarde'), MomentoDia.tarde);
      expect(resolvedor.resolverMomento('noche'), MomentoDia.noche);
    });

    test('«todos» no es algo que se diga, y lo desconocido da null', () {
      expect(resolvedor.resolverMomento('todos'), isNull);
      expect(resolvedor.resolverMomento('madrugada'), isNull);
    });
  });

  group('resolverInstructor', () {
    const lucia = InstructorDisponible(codigo: 'i1', nombre: 'Lucía Rojas');
    const carlos = InstructorDisponible(codigo: 'i2', nombre: 'Carlos Quispe');
    const carlosM = InstructorDisponible(codigo: 'i3', nombre: 'Carlos Mamani');
    const jose = InstructorDisponible(codigo: 'i4', nombre: 'José Pérez');
    const todos = [lucia, carlos, carlosM, jose];

    InstructorDisponible? encontrado(ResolucionInstructor r) => r is InstructorEncontrado ? r.instructor : null;

    test('por el primer nombre, sin tilde ni mayúsculas', () {
      expect(encontrado(resolvedor.resolverInstructor('lucia', todos)), lucia);
      expect(encontrado(resolvedor.resolverInstructor('JOSE', todos)), jose);
    });

    test('ignora el tratamiento («la profe Lucía»)', () {
      expect(encontrado(resolvedor.resolverInstructor('la profe Lucía', todos)), lucia);
      expect(encontrado(resolvedor.resolverInstructor('el instructor José', todos)), jose);
    });

    test('con nombre y apellido distingue a los que comparten nombre', () {
      expect(encontrado(resolvedor.resolverInstructor('Carlos Quispe', todos)), carlos);
      expect(encontrado(resolvedor.resolverInstructor('carlos mam', todos)), carlosM);
    });

    test('un nombre que comparten varios es ambiguo, con los candidatos', () {
      final r = resolvedor.resolverInstructor('Carlos', todos);
      expect(r, isA<InstructorAmbiguo>());
      expect((r as InstructorAmbiguo).candidatos, [carlos, carlosM]);
    });

    test('uno que no existe no se inventa: da las opciones', () {
      final r = resolvedor.resolverInstructor('Roberto', todos);
      expect(r, isA<InstructorSinCoincidencia>());
      expect((r as InstructorSinCoincidencia).opciones, todos);
    });

    test('solo tratamiento o vacío no coincide con nadie', () {
      expect(resolvedor.resolverInstructor('la profe', todos), isA<InstructorSinCoincidencia>());
      expect(resolvedor.resolverInstructor('', todos), isA<InstructorSinCoincidencia>());
    });

    test('sin instructores no hay coincidencia', () {
      final r = resolvedor.resolverInstructor('Lucía', const []);
      expect((r as InstructorSinCoincidencia).opciones, isEmpty);
    });

    test('el nombre completo exacto gana aunque otro lo contenga', () {
      const ana = InstructorDisponible(codigo: 'a1', nombre: 'Ana');
      const anaMaria = InstructorDisponible(codigo: 'a2', nombre: 'Ana María');
      expect(encontrado(resolvedor.resolverInstructor('Ana', const [ana, anaMaria])), ana);
    });
  });
}
