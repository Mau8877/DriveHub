import '../../clases/models/instructor_disponible.dart';
import '../../clases/models/momento_dia.dart';
import '../../horarios/models/fechas.dart';

/// El resultado de buscar un instructor por el nombre que dijo la persona.
sealed class ResolucionInstructor {
  const ResolucionInstructor();
}

class InstructorEncontrado extends ResolucionInstructor {
  final InstructorDisponible instructor;

  const InstructorEncontrado(this.instructor);
}

/// Ninguno coincide: [opciones] son los que sí hay, para mostrárselos a la persona.
class InstructorSinCoincidencia extends ResolucionInstructor {
  final List<InstructorDisponible> opciones;

  const InstructorSinCoincidencia(this.opciones);
}

/// Varios coinciden: la persona tiene que elegir entre [candidatos].
class InstructorAmbiguo extends ResolucionInstructor {
  final List<InstructorDisponible> candidatos;

  const InstructorAmbiguo(this.candidatos);
}

/// Traduce lo que dijo la persona a los valores reales de la pantalla de Clases (CU50).
///
/// * Es puro: recibe la fecha de hoy y las listas por parámetro, sin leer providers ni la red, para probarlo sin
///   montar nada.
/// * Nunca inventa: un dato que no cuadra con nada devuelve `null` o [InstructorSinCoincidencia], y la app se lo
///   dice a la persona en vez de aplicar un filtro que ella no pidió.
class ResolvedorDeDatos {
  const ResolvedorDeDatos();

  /// La fecha del [dia] que dijo la persona, a partir de [hoy]: `hoy`, `manana`, `pasado_manana` o el nombre de
  /// un día de la semana (el primero que llega, hoy incluido si es ese día). `null` si no es un día del catálogo.
  DateTime? resolverDia(String dia, DateTime hoy) {
    final base = soloFecha(hoy);
    switch (dia) {
      case 'hoy':
        return base;
      case 'manana':
        return sumarDias(base, 1);
      case 'pasado_manana':
        return sumarDias(base, 2);
    }
    final indice = _diasSemana.indexOf(dia);
    if (indice < 0) return null;
    final destino = indice + 1; // 1 = lunes a 7 = domingo, como DateTime.weekday
    return sumarDias(base, (destino - base.weekday + 7) % 7);
  }

  /// El [MomentoDia] del catálogo (`manana`, `tarde`, `noche`), o `null`.
  MomentoDia? resolverMomento(String momento) {
    for (final m in MomentoDia.values) {
      if (m != MomentoDia.todos && m.name == momento) return m;
    }
    return null;
  }

  /// Busca el instructor por el nombre dicho, sin distinguir tildes ni mayúsculas y por nombre parcial
  /// («Lucia», «la profe Lucía», «Carlos Q»). Un nombre completo que coincide exacto gana aunque otro lo contenga.
  ResolucionInstructor resolverInstructor(String dicho, List<InstructorDisponible> instructores) {
    final palabras = _palabras(dicho).where((p) => !_tratamientos.contains(p)).toList();
    if (palabras.isEmpty) return InstructorSinCoincidencia(instructores);

    final coinciden = [
      for (final i in instructores)
        if (_contiene(_palabras(i.nombre), palabras)) i,
    ];
    if (coinciden.isEmpty) return InstructorSinCoincidencia(instructores);
    if (coinciden.length == 1) return InstructorEncontrado(coinciden.first);

    final exactos = coinciden.where((i) => _palabras(i.nombre).join(' ') == palabras.join(' ')).toList();
    if (exactos.length == 1) return InstructorEncontrado(exactos.first);
    return InstructorAmbiguo(coinciden);
  }

  // * Cada palabra dicha tiene que ser el comienzo de alguna palabra del nombre («Lu» → «Lucía»)
  bool _contiene(List<String> nombre, List<String> dichas) =>
      dichas.every((d) => nombre.any((n) => n.startsWith(d)));

  List<String> _palabras(String texto) => _sinTildes(texto.toLowerCase())
      .split(RegExp(r'[^a-z0-9ñ]+'))
      .where((p) => p.isNotEmpty)
      .toList();
}

const _diasSemana = ['lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo'];

/// Palabras que acompañan al nombre y no son parte de él («la profe Lucía»).
const _tratamientos = {
  'el', 'la', 'los', 'las', 'de', 'del', 'con', 'profe', 'profesor', 'profesora', 'instructor', 'instructora',
  'senor', 'senora', 'sr', 'sra', 'don', 'dona',
};

String _sinTildes(String texto) {
  const con = 'áàäâéèëêíìïîóòöôúùüû';
  const sin = 'aaaaeeeeiiiioooouuuu';
  final buffer = StringBuffer();
  for (final codigo in texto.runes) {
    final c = String.fromCharCode(codigo);
    final i = con.indexOf(c);
    buffer.write(i >= 0 ? sin[i] : c);
  }
  return buffer.toString();
}
