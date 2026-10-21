// * Formatos de fecha y hora del CU12. El backend usa `yyyy-MM-dd` para las fechas y `HH:mm`
// * (24 horas) para las horas: la app los manda y los lee en ese formato y los muestra como
// * `dd/MM/yyyy` y `HH:mm`.

/// Nombres de los días de la semana, de lunes (índice 0) a domingo.
const List<String> nombresDeDia = [
  'lunes',
  'martes',
  'miércoles',
  'jueves',
  'viernes',
  'sábado',
  'domingo',
];

/// Nombre del día de la semana (1 = lunes a 7 = domingo).
String nombreDeDia(int diaSemana) => nombresDeDia[diaSemana - 1];

String _dos(int n) => n.toString().padLeft(2, '0');

/// `yyyy-MM-dd`, como lo espera y lo devuelve la API.
String fechaParaApi(DateTime fecha) => '${fecha.year.toString().padLeft(4, '0')}-${_dos(fecha.month)}-${_dos(fecha.day)}';

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
