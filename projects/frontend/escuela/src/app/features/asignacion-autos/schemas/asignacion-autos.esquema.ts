import type { Campo, EsquemaGestion } from '@drivehub/gestion'

/**
 * Los dos campos del formulario de asignar y de cambiar de instructor: a quién y desde cuándo.
 *
 * * Los instructores salen de `instructores` filtrado por activos: es lo mismo que exige el backend
 * * (ERR_ESC_18). En "cambiar" el instructor actual sigue en la lista: si se elige, el backend responde
 * * ERR_ESC_16 y el mensaje queda bajo el campo.
 * * La fecha no puede ser futura (hoy por defecto); el backend manda y valida también que no sea
 * * anterior al inicio de la asignación que se cierra.
 */
const camposDeAsignacion = (): Campo[] => [
  {
    campo: 'instructorCodigo',
    titulo: 'Instructor',
    tipo: 'seleccion',
    requerido: true,
    opcionesDe: {
      recurso: 'instructores',
      valor: 'codigo',
      etiqueta: 'nombreCompleto',
      params: { 'filter[estado]': 'ACTIVO' },
    },
    mensajes: { requerido: 'Elige un instructor' },
  },
  {
    campo: 'fecha',
    titulo: 'Desde',
    tipo: 'fecha',
    requerido: true,
    porDefecto: 'hoy',
    validaciones: ['hastaHoy'],
    ayuda: 'El día en que el instructor empieza a usar el auto. No puede ser futuro.',
    mensajes: { requerido: 'La fecha es obligatoria' },
  },
]

/**
 * Asignación de autos a instructores (CU10, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * El recurso es "la asignación de cada auto": la fila es un auto (`vehiculoCodigo`), con su
 * * instructor o sin él. No hay crear ni editar: solo las acciones asignar, cambiar de instructor y
 * * liberar, y el historial como detalle en forma de lista.
 * * Los códigos de `erroresDeCampo` son los del backend: ERR_ESC_13 (instructor), ERR_ESC_16 (ya
 * * asignado o mismo instructor) y ERR_ESC_18 (instructor no activo). ERR_ESC_17, ERR_ESC_09 y
 * * ERR_ESC_19 no tienen campo: el panel muestra el mensaje del backend.
 */
export const ESQUEMA_ASIGNACION_AUTOS: EsquemaGestion = {
  recurso: 'asignaciones-vehiculo',
  titulo: 'Asignación de autos',
  descripcion: 'Qué instructor usa cada auto de la escuela.',
  idCampo: 'vehiculoCodigo',
  nombreSingular: 'auto',
  columnas: [
    { campo: 'placa', titulo: 'Placa', principal: true, icono: 'car', ancho: '12rem' },
    { campo: 'marca', titulo: 'Marca', secundaria: true },
    { campo: 'modelo', titulo: 'Modelo', secundaria: true },
    {
      campo: 'estadoVehiculo',
      titulo: 'Estado del auto',
      tipo: 'insignia',
      insignias: {
        DISPONIBLE: { etiqueta: 'Disponible', tono: 'exito' },
        MANTENIMIENTO: { etiqueta: 'En mantenimiento', tono: 'ambar' },
        BAJA: { etiqueta: 'De baja' },
      },
      ancho: '10rem',
    },
    { campo: 'instructor.nombreCompleto', titulo: 'Instructor', vacioTexto: 'Sin asignar' },
    { campo: 'fechaInicio', titulo: 'Desde', tipo: 'fecha' },
  ],
  busqueda: { placeholder: 'Buscar por placa, marca, modelo o instructor' },
  filtros: [
    {
      clave: 'filter[asignacion]',
      titulo: 'Asignación',
      opciones: [
        { valor: 'ASIGNADO', etiqueta: 'Asignados' },
        { valor: 'SIN_ASIGNAR', etiqueta: 'Sin asignar' },
      ],
    },
  ],
  detalle: {
    titulo: 'Historial del auto',
    descripcion: 'Los instructores que usaron este auto, del más reciente al más antiguo.',
    etiqueta: 'Ver historial',
    cargar: '{id}/historial?pageSize=100',
    // * Con `lista` el panel dibuja las filas del historial y no usa las secciones
    secciones: [],
    lista: {
      vacio: 'Este auto todavía no tuvo instructor.',
      columnas: [
        { campo: 'instructor.nombreCompleto', titulo: 'Instructor', vacioTexto: 'Sin nombre' },
        { campo: 'fechaInicio', titulo: 'Desde', tipo: 'fecha' },
        { campo: 'fechaFin', titulo: 'Hasta', tipo: 'fecha', vacioTexto: 'Vigente' },
      ],
    },
  },
  acciones: [
    {
      id: 'asignar',
      etiqueta: 'Asignar',
      metodo: 'POST',
      ruta: '{id}/asignar',
      mensajeExito: 'Auto asignado correctamente.',
      // * Solo un auto disponible y sin instructor se asigna (ERR_ESC_17 y ERR_ESC_16 en el backend)
      visibleSi: { campo: 'instructor', igual: null },
      formulario: {
        titulo: 'Asignar auto',
        descripcion: 'Solo se asigna un auto disponible a un instructor activo.',
        etiquetaEnviar: 'Asignar auto',
        secciones: [{ campos: camposDeAsignacion() }],
      },
    },
    {
      id: 'cambiar-instructor',
      etiqueta: 'Cambiar instructor',
      metodo: 'POST',
      ruta: '{id}/cambiar-instructor',
      mensajeExito: 'Instructor cambiado correctamente.',
      visibleSi: { campo: 'instructor', distinto: null },
      formulario: {
        titulo: 'Cambiar instructor',
        descripcion:
          'El instructor actual deja el auto en la fecha elegida y el nuevo lo empieza a usar.',
        etiquetaEnviar: 'Cambiar instructor',
        secciones: [{ campos: camposDeAsignacion() }],
      },
    },
    {
      id: 'liberar',
      etiqueta: 'Liberar',
      metodo: 'POST',
      ruta: '{id}/liberar',
      confirmar: '¿Liberar el auto?',
      confirmarTexto: 'El auto queda sin instructor. Su historial se conserva.',
      mensajeExito: 'Auto liberado correctamente.',
      visibleSi: { campo: 'instructor', distinto: null },
      tono: 'peligro',
    },
  ],
  erroresDeCampo: {
    ERR_ESC_13: { campo: 'instructorCodigo', mensaje: 'El instructor no existe en tu escuela' },
    ERR_ESC_16: {
      campo: 'instructorCodigo',
      mensaje: 'El auto ya está asignado a ese instructor o a otro',
    },
    ERR_ESC_18: {
      campo: 'instructorCodigo',
      mensaje: 'Solo se asigna un auto a un instructor activo',
    },
  },
  mensajes: {
    vacio: 'Todavía no hay autos registrados. Regístralos en Escuela → Autos.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar la asignación. Inténtalo de nuevo más tarde.',
  },
}
