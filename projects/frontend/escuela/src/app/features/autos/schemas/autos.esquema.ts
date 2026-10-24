import type { EsquemaGestion } from '@drivehub/gestion'

const ANIO_MINIMO = 1980
// * El backend acepta hasta el año siguiente al actual (modelos que salen antes de fin de año)
const anioMaximo = () => new Date().getFullYear() + 1

/**
 * Gestionar autos (CU07, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * Las reglas de los campos (placa de 5 a 10 caracteres, año de 1980 al próximo, kilometraje >= 0)
 * * tienen que seguir iguales a las del backend, que es la fuente de verdad.
 * * Los códigos de `erroresDeCampo` son los del backend: ERR_ESC_07 (placa en uso).
 * * La categoría de licencia no se gestiona en este caso de uso.
 */
export const ESQUEMA_AUTOS: EsquemaGestion = {
  recurso: 'vehiculos',
  titulo: 'Autos',
  descripcion: 'Flota de autos de la escuela.',
  nombreSingular: 'auto',
  columnas: [
    { campo: 'placa', titulo: 'Placa', ancho: '8rem' },
    { campo: 'marca', titulo: 'Marca' },
    { campo: 'modelo', titulo: 'Modelo' },
    { campo: 'transmision', titulo: 'Transmisión', secundaria: true, ancho: '9rem' },
    { campo: 'kilometraje', titulo: 'Km', secundaria: true, ancho: '7rem' },
    {
      campo: 'estado',
      titulo: 'Estado',
      tipo: 'insignia',
      insignias: {
        DISPONIBLE: { etiqueta: 'Disponible', tono: 'exito' },
        MANTENIMIENTO: { etiqueta: 'En mantenimiento', tono: 'ambar' },
        BAJA: { etiqueta: 'De baja' },
      },
      ancho: '10rem',
    },
  ],
  busqueda: { placeholder: 'Buscar por placa, marca o modelo' },
  filtros: [
    {
      clave: 'filter[estado]',
      titulo: 'Estado',
      opciones: [
        { valor: 'DISPONIBLE', etiqueta: 'Disponibles' },
        { valor: 'MANTENIMIENTO', etiqueta: 'En mantenimiento' },
        { valor: 'BAJA', etiqueta: 'De baja' },
      ],
    },
  ],
  crear: {
    etiqueta: 'Nuevo auto',
    titulo: 'Nuevo auto',
    descripcion: 'Se registra disponible. La placa y la transmisión no se pueden cambiar después.',
    etiquetaEnviar: 'Registrar auto',
    mensajeExito: 'Auto registrado correctamente.',
    secciones: [
      {
        titulo: 'Auto',
        campos: [
          {
            campo: 'placa',
            titulo: 'Placa',
            requerido: true,
            patron: '^[A-Za-z0-9-]{5,10}$',
            ayuda: 'De 5 a 10 caracteres: letras, números y guiones.',
            mensajes: {
              requerido: 'La placa es obligatoria',
              patron: 'La placa debe tener entre 5 y 10 caracteres: letras, números y guiones',
            },
          },
          {
            campo: 'marca',
            titulo: 'Marca',
            requerido: true,
            max: 40,
            mensajes: { requerido: 'La marca es obligatoria' },
          },
          {
            campo: 'modelo',
            titulo: 'Modelo',
            requerido: true,
            max: 40,
            mensajes: { requerido: 'El modelo es obligatorio' },
          },
          {
            campo: 'anio',
            titulo: 'Año',
            tipo: 'numero',
            min: ANIO_MINIMO,
            max: anioMaximo(),
            mensajes: {
              min: `El año debe estar entre ${ANIO_MINIMO} y ${anioMaximo()}`,
              max: `El año debe estar entre ${ANIO_MINIMO} y ${anioMaximo()}`,
            },
          },
          { campo: 'color', titulo: 'Color', max: 20 },
          {
            campo: 'transmision',
            titulo: 'Transmisión',
            tipo: 'seleccion',
            requerido: true,
            opciones: [
              { valor: 'MANUAL', etiqueta: 'Manual' },
              { valor: 'AUTOMATICO', etiqueta: 'Automática' },
            ],
            mensajes: { requerido: 'La transmisión es obligatoria' },
          },
          {
            campo: 'kilometraje',
            titulo: 'Kilometraje',
            tipo: 'numero',
            min: 0,
            max: 9999999,
            mensajes: { min: 'El kilometraje no puede ser negativo' },
          },
          { campo: 'soatVence', titulo: 'Vencimiento del SOAT', tipo: 'fecha' },
          { campo: 'inspeccionVence', titulo: 'Vencimiento de la inspección', tipo: 'fecha' },
        ],
      },
    ],
  },
  editar: {
    titulo: 'Editar auto',
    etiquetaEnviar: 'Guardar',
    mensajeExito: 'Auto actualizado correctamente.',
    visibleSi: { campo: 'estado', distinto: 'BAJA' },
    bloqueo: {
      campo: 'estado',
      valor: 'BAJA',
      mensaje: 'El auto está dado de baja y ya no se puede editar.',
    },
    secciones: [
      {
        campos: [
          {
            campo: 'placa',
            titulo: 'Placa (no se puede cambiar)',
            soloLectura: true,
            requerido: true,
          },
          {
            campo: 'transmision',
            titulo: 'Transmisión (no se puede cambiar)',
            soloLectura: true,
            requerido: true,
          },
          {
            campo: 'marca',
            titulo: 'Marca',
            requerido: true,
            max: 40,
            mensajes: { requerido: 'La marca es obligatoria' },
          },
          {
            campo: 'modelo',
            titulo: 'Modelo',
            requerido: true,
            max: 40,
            mensajes: { requerido: 'El modelo es obligatorio' },
          },
          {
            campo: 'anio',
            titulo: 'Año',
            tipo: 'numero',
            min: ANIO_MINIMO,
            max: anioMaximo(),
            mensajes: {
              min: `El año debe estar entre ${ANIO_MINIMO} y ${anioMaximo()}`,
              max: `El año debe estar entre ${ANIO_MINIMO} y ${anioMaximo()}`,
            },
          },
          { campo: 'color', titulo: 'Color', max: 20 },
          {
            campo: 'kilometraje',
            titulo: 'Kilometraje',
            tipo: 'numero',
            min: 0,
            max: 9999999,
            ayuda: 'No puede ser menor al kilometraje actual.',
            mensajes: { min: 'El kilometraje no puede ser negativo' },
          },
          { campo: 'soatVence', titulo: 'Vencimiento del SOAT', tipo: 'fecha' },
          { campo: 'inspeccionVence', titulo: 'Vencimiento de la inspección', tipo: 'fecha' },
        ],
      },
    ],
  },
  acciones: [
    {
      id: 'enviar-a-mantenimiento',
      etiqueta: 'A mantenimiento',
      metodo: 'POST',
      ruta: '{id}/enviar-a-mantenimiento',
      mensajeExito: 'Auto enviado a mantenimiento.',
      visibleSi: { campo: 'estado', igual: 'DISPONIBLE' },
    },
    {
      id: 'habilitar',
      etiqueta: 'Habilitar',
      metodo: 'POST',
      ruta: '{id}/habilitar',
      mensajeExito: 'Auto habilitado correctamente.',
      visibleSi: { campo: 'estado', igual: 'MANTENIMIENTO' },
    },
    {
      id: 'dar-de-baja',
      etiqueta: 'Dar de baja',
      metodo: 'POST',
      ruta: '{id}/dar-de-baja',
      confirmar: '¿Dar de baja?',
      mensajeExito: 'Auto dado de baja correctamente.',
      visibleSi: { campo: 'estado', distinto: 'BAJA' },
      tono: 'peligro',
    },
  ],
  erroresDeCampo: {
    ERR_ESC_07: { campo: 'placa', mensaje: 'Ya existe un auto con esa placa' },
  },
  mensajes: {
    vacio: 'Todavía no hay autos registrados.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar el auto. Inténtalo de nuevo más tarde.',
  },
}
