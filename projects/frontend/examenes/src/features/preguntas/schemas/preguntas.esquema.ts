import type { Campo, EsquemaGestion } from '@drivehub/gestion'

// * El catálogo de categorías de licencia (Escuela) todavía no tiene un caso de uso que lo exponga:
// * por ahora las siglas son fijas. GENERAL es para lo que sirve a todas.
const CATEGORIAS = [
  { valor: 'GENERAL', etiqueta: 'General' },
  { valor: 'A', etiqueta: 'A (motos)' },
  { valor: 'B', etiqueta: 'B (autos)' },
  { valor: 'C', etiqueta: 'C (carga)' },
]

const TIPOS = [
  { valor: 'OPCION_UNICA', etiqueta: 'Opción única' },
  { valor: 'OPCION_MULTIPLE', etiqueta: 'Opción múltiple' },
  { valor: 'VERDADERO_FALSO', etiqueta: 'Verdadero o falso' },
]

const DIFICULTADES = [
  { valor: '1', etiqueta: 'Fácil' },
  { valor: '2', etiqueta: 'Media' },
  { valor: '3', etiqueta: 'Difícil' },
]

// * Los campos del formulario son los mismos al crear y al editar: se declaran una vez
const CAMPOS_PREGUNTA: Campo[] = [
  {
    campo: 'categoria',
    titulo: 'Categoría',
    tipo: 'seleccion',
    requerido: true,
    opciones: CATEGORIAS,
    mensajes: { requerido: 'La categoría es obligatoria' },
  },
  {
    campo: 'tema',
    titulo: 'Tema',
    requerido: true,
    min: 2,
    max: 80,
    ayuda:
      'Los simulacros arman sus preguntas por tema: escríbelo siempre igual (Señales, Prioridad de paso...).',
    mensajes: { requerido: 'El tema es obligatorio' },
  },
  {
    campo: 'tipo',
    titulo: 'Tipo de pregunta',
    tipo: 'seleccion',
    requerido: true,
    opciones: TIPOS,
    mensajes: { requerido: 'El tipo de pregunta es obligatorio' },
  },
  {
    campo: 'dificultad',
    titulo: 'Dificultad',
    tipo: 'seleccion',
    numerico: true,
    requerido: true,
    opciones: DIFICULTADES,
    mensajes: { requerido: 'La dificultad es obligatoria' },
  },
  {
    campo: 'enunciado',
    titulo: 'Enunciado',
    tipo: 'textarea',
    requerido: true,
    min: 5,
    max: 500,
    mensajes: { requerido: 'El enunciado es obligatorio' },
  },
  {
    campo: 'opciones',
    titulo: 'Opciones de respuesta',
    tipo: 'opciones',
    requerido: true,
    min: 2,
    max: 6,
    maxTexto: 200,
    ayuda:
      'Opción única: marca una correcta. Opción múltiple: marca una o más. Verdadero o falso: exactamente 2 opciones y una correcta.',
    mensajes: { requerido: 'Marca al menos una opción correcta' },
  },
  {
    campo: 'explicacion',
    titulo: 'Explicación',
    tipo: 'textarea',
    max: 500,
    ayuda: 'Se muestra al estudiante cuando corrige su simulacro.',
  },
]

/**
 * Gestionar el banco de preguntas (CU19, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * Las reglas (enunciado de 5 a 500, tema de 2 a 80, de 2 a 6 opciones de hasta 200 caracteres, explicación de
 *   hasta 500, dificultad de 1 a 3) tienen que seguir iguales a las del backend, que es la fuente de verdad. Las
 *   que cruzan campos (cuántas opciones y cuántas correctas según el tipo) las valida solo el backend.
 * * Una pregunta no se borra: se desactiva y deja de entrar en los simulacros nuevos.
 */
export const ESQUEMA_PREGUNTAS: EsquemaGestion = {
  recurso: 'preguntas',
  titulo: 'Banco de preguntas',
  descripcion: 'Preguntas de los simulacros escritos de la escuela.',
  nombreSingular: 'pregunta',
  columnas: [
    { campo: 'enunciado', titulo: 'Enunciado' },
    { campo: 'categoria', titulo: 'Categoría', secundaria: true, ancho: '8rem' },
    { campo: 'tema', titulo: 'Tema', secundaria: true, ancho: '10rem' },
    {
      campo: 'tipo',
      titulo: 'Tipo',
      tipo: 'insignia',
      insignias: {
        OPCION_UNICA: { etiqueta: 'Única', tono: 'info' },
        OPCION_MULTIPLE: { etiqueta: 'Múltiple', tono: 'info' },
        VERDADERO_FALSO: { etiqueta: 'V / F', tono: 'info' },
      },
      ancho: '8rem',
    },
    {
      campo: 'dificultad',
      titulo: 'Dificultad',
      tipo: 'insignia',
      insignias: {
        1: { etiqueta: 'Fácil', tono: 'exito' },
        2: { etiqueta: 'Media', tono: 'ambar' },
        3: { etiqueta: 'Difícil' },
      },
      ancho: '8rem',
    },
    {
      campo: 'activa',
      titulo: 'Estado',
      tipo: 'insignia',
      insignias: {
        true: { etiqueta: 'Activa', tono: 'exito' },
        false: { etiqueta: 'Desactivada' },
      },
      ancho: '9rem',
    },
  ],
  busqueda: { placeholder: 'Buscar por enunciado o tema' },
  filtros: [
    {
      clave: 'filter[categoria]',
      titulo: 'Categoría',
      opciones: CATEGORIAS.map(({ valor, etiqueta }) => ({ valor, etiqueta })),
    },
    { clave: 'filter[tipo]', titulo: 'Tipo', opciones: TIPOS },
    { clave: 'filter[dificultad]', titulo: 'Dificultad', opciones: DIFICULTADES },
    {
      clave: 'filter[activa]',
      titulo: 'Estado',
      opciones: [
        { valor: 'true', etiqueta: 'Activas' },
        { valor: 'false', etiqueta: 'Desactivadas' },
      ],
    },
  ],
  crear: {
    etiqueta: 'Nueva pregunta',
    titulo: 'Nueva pregunta',
    descripcion: 'Se crea activa: entra en los simulacros que se armen desde ahora.',
    etiquetaEnviar: 'Crear pregunta',
    mensajeExito: 'Pregunta creada correctamente.',
    secciones: [{ titulo: 'Pregunta', campos: CAMPOS_PREGUNTA }],
  },
  editar: {
    titulo: 'Editar pregunta',
    etiquetaEnviar: 'Guardar',
    mensajeExito: 'Pregunta actualizada correctamente.',
    secciones: [{ campos: CAMPOS_PREGUNTA }],
  },
  acciones: [
    {
      id: 'desactivar',
      etiqueta: 'Desactivar',
      metodo: 'POST',
      ruta: '{id}/desactivar',
      confirmar: '¿Desactivar?',
      mensajeExito: 'Pregunta desactivada correctamente.',
      visibleSi: { campo: 'activa', igual: true },
      tono: 'peligro',
    },
    {
      id: 'activar',
      etiqueta: 'Activar',
      metodo: 'POST',
      ruta: '{id}/activar',
      mensajeExito: 'Pregunta activada correctamente.',
      visibleSi: { campo: 'activa', igual: false },
    },
  ],
  mensajes: {
    vacio: 'Todavía no hay preguntas en el banco.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar la pregunta. Inténtalo de nuevo más tarde.',
  },
}
