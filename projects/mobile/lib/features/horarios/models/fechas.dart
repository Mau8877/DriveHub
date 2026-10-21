// * Formatos de fecha y hora del CU12. El backend usa `yyyy-MM-dd` para las fechas y `HH:mm`
// * (24 horas) para las horas: la app los manda y los lee en ese formato y los muestra como
// * `dd/MM/yyyy` y `HH:mm`.

/// Nombres de los días de la semana, de lunes (índice 0) a domingo.
const List<String> nombresDeDia = ['lunes', 'martes', 'miércoles', 'jueves', 'viernes', 'sábado', 'domingo'];

/// Nombre del día de la semana (1 = lunes a 7 = domingo).
String nombreDeDia(int diaSemana) => nombresDeDia[diaSemana - 1];

String _dos(int n) => n.toString().padLeft(2, '0');

/// `yyyy-MM-dd`, como lo espera y lo devuelve la API.
String fechaParaApi(DateTime fecha) =>
    '${fecha.year.toString().padLeft(4, '0')}-${_dos(fecha.month)}-${_dos(fecha.day)}';

/// Lee una fecha `yyyy-MM-dd` de la API (solo fecha, a medianoche local).
DateTime fechaDesdeApi(String valor) {
  final partes = valor.split('-');
  return DateTime(int.parse(partes[0]), int.parse(partes[1]), int.parse(partes[2]));
}

/// `dd/MM/yyyy`, para mostrarla en pantalla.
String fechaParaMostrar(DateTime fecha) => '${_dos(fecha.day)}/${_dos(fecha.month)}/${fecha.year}';

/// `HH:mm` desde las horas y minutos.
String horaParaApi(int hora, int minuto) => '${_dos(hora)}:${_dos(minuto)}';

/// Solo la fecha: descarta la hora, para comparar días y para usar de clave.
DateTime soloFecha(DateTime fecha) => DateTime(fecha.year, fecha.month, fecha.day);

/// Letra con la que se dibuja un día en los selectores (1 = lunes: `L`, `M`, `X`, `J`, `V`, `S`, `D`).
String letraDeDia(int diaSemana) => 'LMXJVSD'[diaSemana - 1];

/// Nombre del día con la primera en mayúscula (1 = lunes: «Lunes»).
String nombreDeDiaCapitalizado(int diaSemana) {
  final nombre = nombreDeDia(diaSemana);
  return '${nombre[0].toUpperCase()}${nombre.substring(1)}';
}

const List<String> _mesesCortos = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

/// Abreviatura del mes (1 = enero: `ene`).
String mesCorto(int mes) => _mesesCortos[mes - 1];

/// El lunes de la semana de [fecha] (a medianoche).
DateTime lunesDeLaSemana(DateTime fecha) => DateTime(fecha.year, fecha.month, fecha.day - (fecha.weekday - 1));

/// Suma días sin arrastrar la hora, aunque haya cambio de horario de verano.
DateTime sumarDias(DateTime fecha, int dias) => DateTime(fecha.year, fecha.month, fecha.day + dias);

/// «26 – 1 nov» o «26 oct – 1 nov»: de lunes a domingo de la semana que empieza en [lunes].
String etiquetaDeSemana(DateTime lunes) {
  final domingo = sumarDias(lunes, 6);
  if (lunes.month == domingo.month) {
    return '${lunes.day} – ${domingo.day} ${mesCorto(domingo.month)}';
  }
  return '${lunes.day} ${mesCorto(lunes.month)} – ${domingo.day} ${mesCorto(domingo.month)}';
}

/// «Lunes 26»: para el subtítulo de cada día.
String diaConNumero(DateTime fecha) => '${nombreDeDiaCapitalizado(fecha.weekday)} ${fecha.day}';

final RegExp _formatoHora = RegExp(r'^([01]\d|2[0-3]):[0-5]\d$');

/// Minutos del día de una hora `HH:mm`, o `null` si no tiene ese formato.
int? minutosDeHora(String? hora) {
  if (hora == null || !_formatoHora.hasMatch(hora)) return null;
  final partes = hora.split(':');
  return int.parse(partes[0]) * 60 + int.parse(partes[1]);
}

/// Cuántas clases de [duracionMin] caben en la ventana entre [horaInicio] y [horaFin]; cero si los
/// datos no son válidos. Es solo una ayuda para la pantalla: el backend es quien genera las franjas.
int franjasDeLaVentana(String? horaInicio, String? horaFin, int? duracionMin) {
  final inicio = minutosDeHora(horaInicio);
  final fin = minutosDeHora(horaFin);
  if (inicio == null || fin == null || duracionMin == null || duracionMin <= 0 || fin <= inicio) return 0;
  return (fin - inicio) ~/ duracionMin;
}
