/// Lo que el instructor corrige de una clase ya cerrada (CU17). Es un `PATCH`: solo viaja lo que cambió.
///
/// * Un texto vacío (`''`) SÍ viaja: así se borra ese texto. Un kilometraje no se puede borrar, solo cambiar.
/// * La asistencia no se puede corregir, por eso no está acá.
class ActualizarClaseRequest {
  final String? tema;
  final String? observaciones;
  final int? kmInicio;
  final int? kmFin;

  const ActualizarClaseRequest({this.tema, this.observaciones, this.kmInicio, this.kmFin});

  /// No cambia nada: no hace falta llamar al servidor.
  bool get estaVacio => tema == null && observaciones == null && kmInicio == null && kmFin == null;

  Map<String, dynamic> toJson() => {
    'tema': ?tema,
    'observaciones': ?observaciones,
    'kmInicio': ?kmInicio,
    'kmFin': ?kmFin,
  };
}
