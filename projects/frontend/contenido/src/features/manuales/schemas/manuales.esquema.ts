import type { EsquemaGestion } from '@drivehub/gestion'

// * El catálogo de categorías de licencia (Escuela) todavía no tiene un caso de uso que lo exponga:
// * por ahora las siglas son fijas. GENERAL es para lo que sirve a todas.
const CATEGORIAS = [
  { valor: 'GENERAL', etiqueta: 'General' },
  { valor: 'A', etiqueta: 'A (motos)' },
  { valor: 'B', etiqueta: 'B (autos)' },
  { valor: 'C', etiqueta: 'C (carga)' },
]

const PDF_MAX_BYTES = 20 * 1024 * 1024

/**
 * Gestionar manuales (CU32, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * Las reglas (título de 3 a 150, descripción hasta 1000, tema hasta 80, hasta 10 etiquetas, PDF de
 *   hasta 20 MB) tienen que seguir iguales a las del backend, que es la fuente de verdad.
 * * El PDF se sube primero (`manuales/archivos`) y el manual lo referencia; los códigos de
 *   `erroresDeCampo` son los del backend: ERR_CON_03 (archivo no válido).
 */
export const ESQUEMA_MANUALES: EsquemaGestion = {
  recurso: 'manuales',
  titulo: 'Manuales',
  descripcion: 'Manuales en PDF de la escuela.',
  nombreSingular: 'manual',
  columnas: [
    { campo: 'titulo', titulo: 'Título' },
    { campo: 'categoria', titulo: 'Categoría', secundaria: true, ancho: '8rem' },
    { campo: 'version', titulo: 'Versión', secundaria: true, ancho: '7rem' },
    {
      campo: 'estado',
      titulo: 'Estado',
      tipo: 'insignia',
      insignias: {
        BORRADOR: { etiqueta: 'Borrador', tono: 'ambar' },
        PUBLICADO: { etiqueta: 'Publicado', tono: 'exito' },
        ARCHIVADO: { etiqueta: 'Archivado' },
      },
      ancho: '9rem',
    },
    { campo: 'archivo.url', titulo: 'PDF', tipo: 'enlace', etiqueta: 'Ver PDF' },
  ],
  busqueda: { placeholder: 'Buscar por título, descripción o etiqueta' },
  filtros: [
    {
      clave: 'filter[estado]',
      titulo: 'Estado',
      opciones: [
        { valor: 'BORRADOR', etiqueta: 'Borradores' },
        { valor: 'PUBLICADO', etiqueta: 'Publicados' },
        { valor: 'ARCHIVADO', etiqueta: 'Archivados' },
      ],
    },
    {
      clave: 'filter[categoria]',
      titulo: 'Categoría',
      opciones: CATEGORIAS.map(({ valor, etiqueta }) => ({ valor, etiqueta })),
    },
  ],
  crear: {
    etiqueta: 'Nuevo manual',
    titulo: 'Nuevo manual',
    descripcion: 'Se crea como borrador: publícalo cuando esté listo.',
    etiquetaEnviar: 'Crear manual',
    mensajeExito: 'Manual creado correctamente.',
    secciones: [
      {
        titulo: 'Manual',
        campos: [
          {
            campo: 'titulo',
            titulo: 'Título',
            requerido: true,
            min: 3,
            max: 150,
            mensajes: { requerido: 'El título es obligatorio' },
          },
          {
            campo: 'categoria',
            titulo: 'Categoría',
            tipo: 'seleccion',
            requerido: true,
            opciones: CATEGORIAS,
            mensajes: { requerido: 'La categoría es obligatoria' },
          },
          { campo: 'tema', titulo: 'Tema', max: 80 },
          { campo: 'descripcion', titulo: 'Descripción', max: 1000 },
          {
            campo: 'etiquetas',
            titulo: 'Etiquetas',
            tipo: 'lista',
            ayuda: 'Separadas por comas. Hasta 10 de 30 caracteres.',
          },
          {
            campo: 'archivo',
            titulo: 'PDF del manual',
            tipo: 'archivo',
            requerido: true,
            subida: 'manuales/archivos',
            accept: 'application/pdf',
            maxBytes: PDF_MAX_BYTES,
            ayuda: 'Solo PDF, de hasta 20 MB.',
            mensajes: {
              requerido: 'Sube el PDF del manual',
              tipo: 'El manual debe ser un archivo PDF',
              max: 'El PDF no puede pesar más de 20 MB',
            },
          },
        ],
      },
    ],
  },
  editar: {
    titulo: 'Editar manual',
    etiquetaEnviar: 'Guardar',
    mensajeExito: 'Manual actualizado correctamente.',
    visibleSi: { campo: 'estado', distinto: 'ARCHIVADO' },
    bloqueo: {
      campo: 'estado',
      valor: 'ARCHIVADO',
      mensaje: 'El manual está archivado. Publícalo de nuevo para editarlo.',
    },
    secciones: [
      {
        campos: [
          {
            campo: 'titulo',
            titulo: 'Título',
            requerido: true,
            min: 3,
            max: 150,
            mensajes: { requerido: 'El título es obligatorio' },
          },
          {
            campo: 'categoria',
            titulo: 'Categoría',
            tipo: 'seleccion',
            requerido: true,
            opciones: CATEGORIAS,
            mensajes: { requerido: 'La categoría es obligatoria' },
          },
          { campo: 'tema', titulo: 'Tema', max: 80 },
          { campo: 'descripcion', titulo: 'Descripción', max: 1000 },
          {
            campo: 'etiquetas',
            titulo: 'Etiquetas',
            tipo: 'lista',
            ayuda: 'Separadas por comas. Hasta 10 de 30 caracteres.',
          },
          {
            campo: 'archivo',
            titulo: 'PDF del manual',
            tipo: 'archivo',
            subida: 'manuales/archivos',
            accept: 'application/pdf',
            maxBytes: PDF_MAX_BYTES,
            ayuda: 'Elige un PDF solo si quieres reemplazar el actual: sube la versión del manual.',
            mensajes: {
              tipo: 'El manual debe ser un archivo PDF',
              max: 'El PDF no puede pesar más de 20 MB',
            },
          },
        ],
      },
    ],
  },
  acciones: [
    {
      id: 'publicar',
      etiqueta: 'Publicar',
      metodo: 'POST',
      ruta: '{id}/publicar',
      mensajeExito: 'Manual publicado correctamente.',
      visibleSi: { campo: 'estado', igual: 'BORRADOR' },
    },
    {
      // * Mismo endpoint que publicar: un manual archivado vuelve a PUBLICADO
      id: 'publicar-de-nuevo',
      etiqueta: 'Publicar de nuevo',
      metodo: 'POST',
      ruta: '{id}/publicar',
      mensajeExito: 'Manual publicado de nuevo.',
      visibleSi: { campo: 'estado', igual: 'ARCHIVADO' },
    },
    {
      id: 'archivar',
      etiqueta: 'Archivar',
      metodo: 'POST',
      ruta: '{id}/archivar',
      confirmar: '¿Archivar?',
      mensajeExito: 'Manual archivado correctamente.',
      visibleSi: { campo: 'estado', igual: 'PUBLICADO' },
      tono: 'peligro',
    },
  ],
  erroresDeCampo: {
    ERR_CON_03: { campo: 'archivo', mensaje: 'El archivo no es válido: sube el PDF otra vez' },
  },
  mensajes: {
    vacio: 'Todavía no hay manuales.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar el manual. Inténtalo de nuevo más tarde.',
  },
}
