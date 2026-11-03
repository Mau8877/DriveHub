import type { EsquemaGestion } from '@drivehub/gestion'

/**
 * Disponibilidad de los instructores (CU11, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * Es de solo lectura: un renglón por instructor de la escuela, identificado por el `codigo` de su ficha.
 * * Sin crear, editar ni acciones. La disponibilidad se calcula en el backend para un día y un momento del
 * * día; la pantalla solo los elige.
 * * Los valores de las insignias y de los filtros son los del backend: `Disponibilidad` (DISPONIBLE, COMPLETO,
 * * SIN_HORARIO, NO_DISPONIBLE), `EstadoAutos` (DISPONIBLE, NO_DISPONIBLE, SIN_AUTO) y `MomentoDelDia`
 * * (MANANA, TARDE, NOCHE).
 * * El filtro de fecha arranca en hoy y no deja elegir días pasados (el backend responde 400 si llegara uno).
 * * Un instructor sin autos muestra "Sin auto"; los autos sin instructor se ven en "Asignación de autos".
 */
export const ESQUEMA_DISPONIBILIDAD: EsquemaGestion = {
  recurso: 'disponibilidad-instructores',
  titulo: 'Disponibilidad',
  descripcion: 'Qué instructores pueden dar clase un día y con qué autos cuentan.',
  idCampo: 'instructorCodigo',
  nombreSingular: 'instructor',
  columnas: [
    {
      campo: 'nombreCompleto',
      titulo: 'Instructor',
      principal: true,
      icono: 'user',
      ancho: '14rem',
    },
    {
      campo: 'disponibilidad',
      titulo: 'Disponibilidad',
      tipo: 'insignia',
      insignias: {
        DISPONIBLE: { etiqueta: 'Disponible', tono: 'exito' },
        COMPLETO: { etiqueta: 'Completo', tono: 'info' },
        SIN_HORARIO: { etiqueta: 'Sin horario' },
        NO_DISPONIBLE: { etiqueta: 'No disponible', tono: 'ambar' },
      },
      ancho: '10rem',
    },
    { campo: 'libres', titulo: 'Libres', ancho: '6rem' },
    { campo: 'reservadas', titulo: 'Reservadas', ancho: '7rem' },
    { campo: 'bloqueadas', titulo: 'Bloqueadas', ancho: '7rem' },
    {
      campo: 'autos',
      titulo: 'Autos',
      tipo: 'etiquetas',
      etiqueta: 'placa',
      vacioTexto: 'Sin auto',
    },
    {
      campo: 'estadoAutos',
      titulo: 'Estado del auto',
      tipo: 'insignia',
      insignias: {
        DISPONIBLE: { etiqueta: 'Disponible', tono: 'exito' },
        NO_DISPONIBLE: { etiqueta: 'No disponible', tono: 'ambar' },
        SIN_AUTO: { etiqueta: 'Sin auto' },
      },
      ancho: '10rem',
    },
  ],
  busqueda: { placeholder: 'Buscar por instructor, placa, marca o modelo' },
  filtros: [
    {
      clave: 'filter[fecha]',
      titulo: 'Día',
      tipo: 'fecha',
      porDefecto: 'hoy',
      desdeHoy: true,
      opciones: [],
    },
    {
      clave: 'filter[momento]',
      titulo: 'Momento',
      todas: 'Todo el día',
      opciones: [
        { valor: 'MANANA', etiqueta: 'Mañana' },
        { valor: 'TARDE', etiqueta: 'Tarde' },
        { valor: 'NOCHE', etiqueta: 'Noche' },
      ],
    },
    {
      clave: 'filter[disponibilidad]',
      titulo: 'Disponibilidad',
      opciones: [
        { valor: 'DISPONIBLE', etiqueta: 'Disponibles' },
        { valor: 'COMPLETO', etiqueta: 'Completos' },
        { valor: 'SIN_HORARIO', etiqueta: 'Sin horario' },
        { valor: 'NO_DISPONIBLE', etiqueta: 'No disponibles' },
      ],
    },
    {
      clave: 'filter[auto]',
      titulo: 'Autos',
      opciones: [
        { valor: 'DISPONIBLE', etiqueta: 'Con auto disponible' },
        { valor: 'NO_DISPONIBLE', etiqueta: 'Auto no disponible' },
        { valor: 'SIN_AUTO', etiqueta: 'Sin auto' },
      ],
    },
  ],
  mensajes: {
    vacio: 'Todavía no hay instructores en tu escuela. Regístralos en Escuela → Instructores.',
    sinResultados: 'Ningún instructor coincide con esa búsqueda o esos filtros.',
  },
}
