import type { Campo, EsquemaGestion } from '@drivehub/gestion'

// * Las categorías de licencia con examen de conducción. El catálogo de la escuela todavía no tiene un caso de uso
// * que lo exponga a esta pantalla: por ahora las siglas son fijas (GENERAL no aplica, es solo del banco de preguntas).
const CATEGORIAS = [
  { valor: 'A', etiqueta: 'A (motos)' },
  { valor: 'B', etiqueta: 'B (autos)' },
  { valor: 'C', etiqueta: 'C (carga)' },
]

const ESTADOS = [
  { valor: 'PROGRAMADO', etiqueta: 'Programado' },
  { valor: 'REALIZADO', etiqueta: 'Realizado' },
  { valor: 'CANCELADO', etiqueta: 'Cancelado' },
  { valor: 'NO_ASISTIO', etiqueta: 'No asistió' },
]

const PATRON_HORA = '^([01]\\d|2[0-3]):[0-5]\\d$'

// * Se eligen de las listas de la escuela (estudiantes, instructores y vehículos) por su codigo público
const CAMPO_ESTUDIANTE: Campo = {
  campo: 'estudianteCodigo',
  titulo: 'Estudiante',
  tipo: 'seleccion',
  requerido: true,
  opcionesDe: { recurso: 'estudiantes', valor: 'codigo', etiqueta: 'nombreCompleto' },
  mensajes: { requerido: 'El estudiante es obligatorio' },
}

const CAMPO_CATEGORIA: Campo = {
  campo: 'categoria',
  titulo: 'Categoría',
  tipo: 'seleccion',
  requerido: true,
  opciones: CATEGORIAS,
  mensajes: { requerido: 'La categoría es obligatoria' },
}

// * Al reprogramar no son obligatorios: lo que no se toca queda como estaba
const camposDeCita = (requerido: boolean): Campo[] => [
  {
    campo: 'fecha',
    titulo: 'Fecha',
    tipo: 'fecha',
    requerido,
    validaciones: ['fechaFutura'],
    soloSiCambia: true,
    mitad: true,
    mensajes: { requerido: 'La fecha es obligatoria' },
  },
  {
    campo: 'hora',
    titulo: 'Hora (HH:mm)',
    requerido,
    patron: PATRON_HORA,
    mitad: true,
    ayuda: 'En la hora de la escuela, por ejemplo 09:30.',
    mensajes: { requerido: 'La hora es obligatoria', patron: 'La hora debe ser HH:mm, por ejemplo 09:30' },
  },
  {
    campo: 'evaluadorCodigo',
    titulo: 'Evaluador',
    tipo: 'seleccion',
    requerido,
    opcionesDe: { recurso: 'instructores', valor: 'codigo', etiqueta: 'nombreCompleto' },
    mensajes: { requerido: 'El evaluador es obligatorio' },
  },
  {
    campo: 'vehiculoCodigo',
    titulo: 'Vehículo',
    tipo: 'seleccion',
    requerido,
    opcionesDe: { recurso: 'vehiculos', valor: 'codigo', etiqueta: 'placa' },
    mensajes: { requerido: 'El vehículo es obligatorio' },
  },
]

const CAMPOS_RESULTADO: Campo[] = [
  {
    campo: 'puntaje',
    titulo: 'Puntaje (0 a 100)',
    tipo: 'decimal',
    requerido: true,
    decimales: 1,
    min: 0,
    max: 100,
    ayuda: 'Aprueba con 70 puntos o más.',
    mensajes: { requerido: 'El puntaje es obligatorio' },
  },
  { campo: 'observaciones', titulo: 'Observaciones', tipo: 'textarea', max: 500 },
]

/**
 * Gestionar los exámenes de conducción (CU22, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * Las reglas (fecha y hora futuras, puntaje de 0 a 100, observaciones de hasta 500) tienen que seguir iguales a las
 *   del backend, que es la fuente de verdad. Que el estudiante sea de la escuela, que no tenga otro examen programado
 *   en la categoría y el número de intento los resuelve el backend.
 * * Solo un examen programado se reprograma, se cancela, se marca como no asistido o recibe su resultado.
 */
export const ESQUEMA_CONDUCCION: EsquemaGestion = {
  recurso: 'examenes-conduccion',
  titulo: 'Examen de conducción',
  descripcion: 'Programa los exámenes oficiales de conducción y registra sus resultados.',
  nombreSingular: 'examen',
  columnas: [
    { campo: 'estudianteNombre', titulo: 'Estudiante', principal: true },
    { campo: 'categoria', titulo: 'Categoría', secundaria: true, ancho: '7rem' },
    { campo: 'fecha', titulo: 'Fecha', tipo: 'fecha', ancho: '9.5rem' },
    { campo: 'hora', titulo: 'Hora', secundaria: true, ancho: '6rem' },
    { campo: 'intento', titulo: 'Intento', secundaria: true, ancho: '6rem' },
    {
      campo: 'estado',
      titulo: 'Estado',
      tipo: 'insignia',
      insignias: {
        PROGRAMADO: { etiqueta: 'Programado', tono: 'info' },
        REALIZADO: { etiqueta: 'Realizado', tono: 'exito' },
        CANCELADO: { etiqueta: 'Cancelado' },
        NO_ASISTIO: { etiqueta: 'No asistió', tono: 'ambar' },
      },
      ancho: '9rem',
    },
    {
      campo: 'resultado',
      titulo: 'Resultado',
      tipo: 'insignia',
      insignias: {
        APROBADO: { etiqueta: 'Aprobado', tono: 'exito' },
        REPROBADO: { etiqueta: 'Reprobado' },
      },
      ancho: '9rem',
    },
  ],
  filtros: [
    {
      clave: 'filter[categoria]',
      titulo: 'Categoría',
      opciones: CATEGORIAS.map(({ valor, etiqueta }) => ({ valor, etiqueta })),
    },
    { clave: 'filter[estado]', titulo: 'Estado', opciones: ESTADOS },
  ],
  crear: {
    etiqueta: 'Programar examen',
    titulo: 'Programar examen',
    descripcion: 'El número de intento se calcula solo según los exámenes anteriores del estudiante.',
    etiquetaEnviar: 'Programar',
    mensajeExito: 'Examen programado correctamente.',
    secciones: [
      { titulo: 'Estudiante', campos: [CAMPO_ESTUDIANTE, CAMPO_CATEGORIA] },
      { titulo: 'Fecha y recursos', campos: camposDeCita(true) },
    ],
  },
  editar: {
    titulo: 'Reprogramar examen',
    descripcion: 'El estudiante y la categoría no cambian: para eso se cancela y se programa otro.',
    etiquetaEnviar: 'Guardar',
    mensajeExito: 'Examen reprogramado correctamente.',
    visibleSi: { campo: 'estado', igual: 'PROGRAMADO' },
    secciones: [{ campos: camposDeCita(false) }],
  },
  acciones: [
    {
      id: 'registrar-resultado',
      etiqueta: 'Registrar resultado',
      icono: 'check-circle',
      metodo: 'POST',
      ruta: '{id}/registrar-resultado',
      mensajeExito: 'Resultado registrado correctamente.',
      visibleSi: { campo: 'estado', igual: 'PROGRAMADO' },
      formulario: {
        titulo: 'Registrar resultado',
        descripcion: 'Cierra el examen: queda realizado y la nota pasa al historial del estudiante.',
        etiquetaEnviar: 'Guardar resultado',
        secciones: [{ campos: CAMPOS_RESULTADO }],
      },
    },
    {
      id: 'no-asistio',
      etiqueta: 'No asistió',
      metodo: 'POST',
      ruta: '{id}/no-asistio',
      confirmar: '¿Marcar que no asistió?',
      mensajeExito: 'Inasistencia registrada correctamente.',
      visibleSi: { campo: 'estado', igual: 'PROGRAMADO' },
    },
    {
      id: 'cancelar',
      etiqueta: 'Cancelar',
      metodo: 'POST',
      ruta: '{id}/cancelar',
      confirmar: '¿Cancelar el examen?',
      mensajeExito: 'Examen cancelado correctamente.',
      visibleSi: { campo: 'estado', igual: 'PROGRAMADO' },
      tono: 'peligro',
    },
  ],
  erroresDeCampo: {
    ERR_ACA_26: { campo: 'categoria', mensaje: 'El estudiante ya tiene un examen programado en esa categoría' },
    ERR_ACA_27: { campo: 'estudianteCodigo', mensaje: 'El estudiante no existe en tu escuela' },
  },
  mensajes: {
    vacio: 'Todavía no hay exámenes de conducción programados.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar el examen. Inténtalo de nuevo más tarde.',
  },
}
