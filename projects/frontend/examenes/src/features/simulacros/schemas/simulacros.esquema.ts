import type { Campo, EsquemaGestion } from '@drivehub/gestion'

// * Igual que en el banco de preguntas: hasta que el catálogo de categorías de la escuela tenga su caso de uso,
// * las siglas son fijas. GENERAL es para lo que sirve a todas.
const CATEGORIAS = [
  { valor: 'GENERAL', etiqueta: 'General' },
  { valor: 'A', etiqueta: 'A (motos)' },
  { valor: 'B', etiqueta: 'B (autos)' },
  { valor: 'C', etiqueta: 'C (carga)' },
]

// * Los campos del formulario son los mismos al crear y al editar: se declaran una vez
const CAMPOS_SIMULACRO: Campo[] = [
  {
    campo: 'nombre',
    titulo: 'Nombre',
    requerido: true,
    min: 3,
    max: 80,
    mensajes: { requerido: 'El nombre es obligatorio' },
  },
  {
    campo: 'categoria',
    titulo: 'Categoría',
    tipo: 'seleccion',
    requerido: true,
    opciones: CATEGORIAS,
    mensajes: { requerido: 'La categoría es obligatoria' },
  },
  {
    campo: 'duracionMin',
    titulo: 'Duración (minutos)',
    tipo: 'numero',
    requerido: true,
    min: 5,
    max: 180,
    mitad: true,
    mensajes: { requerido: 'La duración es obligatoria' },
  },
  {
    campo: 'nroPreguntas',
    titulo: 'Número de preguntas',
    tipo: 'numero',
    requerido: true,
    min: 1,
    max: 100,
    mitad: true,
    mensajes: { requerido: 'El número de preguntas es obligatorio' },
  },
  {
    campo: 'puntajeAprobacion',
    titulo: 'Puntaje de aprobación (%)',
    tipo: 'decimal',
    requerido: true,
    decimales: 1,
    min: 1,
    max: 100,
    ayuda: 'Porcentaje mínimo de aciertos para aprobar.',
    mensajes: { requerido: 'El puntaje de aprobación es obligatorio' },
  },
  {
    campo: 'reglas',
    titulo: 'Preguntas por tema',
    tipo: 'reglas',
    requerido: true,
    min: 1,
    max: 10,
    ayuda:
      'Escribe los temas tal como están en el banco de preguntas. Las cantidades deben sumar el número de preguntas.',
    mensajes: { requerido: 'Agrega al menos un tema' },
  },
]

/**
 * Configurar simulacros escritos (CU20, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * Las reglas (nombre de 3 a 80, duración de 5 a 180 minutos, de 1 a 100 preguntas, aprobación de 1 a 100, de 1 a
 *   10 temas de hasta 80 caracteres con cantidad de 1 a 100) tienen que seguir iguales a las del backend, que es la
 *   fuente de verdad. Las que cruzan campos (las cantidades suman el número de preguntas) las valida solo el backend.
 * * Un simulacro no se borra: se desactiva y deja de ofrecerse a los estudiantes.
 */
export const ESQUEMA_SIMULACROS: EsquemaGestion = {
  recurso: 'simulacros',
  titulo: 'Simulacros',
  descripcion: 'Configuración de los simulacros escritos de la escuela.',
  nombreSingular: 'simulacro',
  columnas: [
    { campo: 'nombre', titulo: 'Nombre' },
    { campo: 'categoria', titulo: 'Categoría', secundaria: true, ancho: '8rem' },
    { campo: 'duracionMin', titulo: 'Minutos', secundaria: true, ancho: '7rem' },
    { campo: 'nroPreguntas', titulo: 'Preguntas', secundaria: true, ancho: '8rem' },
    { campo: 'puntajeAprobacion', titulo: 'Aprueba con (%)', secundaria: true, ancho: '9rem' },
    {
      campo: 'activo',
      titulo: 'Estado',
      tipo: 'insignia',
      insignias: {
        true: { etiqueta: 'Activo', tono: 'exito' },
        false: { etiqueta: 'Desactivado' },
      },
      ancho: '9rem',
    },
  ],
  busqueda: { placeholder: 'Buscar por nombre' },
  filtros: [
    {
      clave: 'filter[categoria]',
      titulo: 'Categoría',
      opciones: CATEGORIAS.map(({ valor, etiqueta }) => ({ valor, etiqueta })),
    },
    {
      clave: 'filter[activo]',
      titulo: 'Estado',
      opciones: [
        { valor: 'true', etiqueta: 'Activos' },
        { valor: 'false', etiqueta: 'Desactivados' },
      ],
    },
  ],
  crear: {
    etiqueta: 'Nuevo simulacro',
    titulo: 'Nuevo simulacro',
    descripcion: 'Se crea activo: los estudiantes podrán rendirlo desde ahora.',
    etiquetaEnviar: 'Crear simulacro',
    mensajeExito: 'Simulacro creado correctamente.',
    secciones: [{ titulo: 'Simulacro', campos: CAMPOS_SIMULACRO }],
  },
  editar: {
    titulo: 'Editar simulacro',
    etiquetaEnviar: 'Guardar',
    mensajeExito: 'Simulacro actualizado correctamente.',
    secciones: [{ campos: CAMPOS_SIMULACRO }],
  },
  acciones: [
    {
      id: 'desactivar',
      etiqueta: 'Desactivar',
      metodo: 'POST',
      ruta: '{id}/desactivar',
      confirmar: '¿Desactivar?',
      mensajeExito: 'Simulacro desactivado correctamente.',
      visibleSi: { campo: 'activo', igual: true },
      tono: 'peligro',
    },
    {
      id: 'activar',
      etiqueta: 'Activar',
      metodo: 'POST',
      ruta: '{id}/activar',
      mensajeExito: 'Simulacro activado correctamente.',
      visibleSi: { campo: 'activo', igual: false },
    },
  ],
  erroresDeCampo: {
    ERR_ACA_23: { campo: 'nombre', mensaje: 'Ya existe un simulacro con ese nombre' },
  },
  mensajes: {
    vacio: 'Todavía no hay simulacros configurados.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar el simulacro. Inténtalo de nuevo más tarde.',
  },
}
