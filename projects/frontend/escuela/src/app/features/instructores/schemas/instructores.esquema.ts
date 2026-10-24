import type { Campo, EsquemaGestion, Insignia } from '@drivehub/gestion'

const ESTADOS: Record<string, Insignia> = {
  ACTIVO: { etiqueta: 'Activo', tono: 'exito' },
  LICENCIA: { etiqueta: 'Licencia', tono: 'ambar' },
  INACTIVO: { etiqueta: 'Inactivo' },
}

const OPCIONES_ESTADO = [
  { valor: 'ACTIVO', etiqueta: 'Activo' },
  { valor: 'LICENCIA', etiqueta: 'Licencia' },
  { valor: 'INACTIVO', etiqueta: 'Inactivo' },
]

const OPCIONES_SEXO = [
  { valor: 'MASCULINO', etiqueta: 'Masculino' },
  { valor: 'FEMENINO', etiqueta: 'Femenino' },
]

const MENSAJE_CI = 'El CI debe tener entre 5 y 15 caracteres: letras, números y guion'
const MENSAJE_LICENCIA =
  'El número de licencia debe tener entre 3 y 30 caracteres: letras, números y guion'

const CAMPO_LICENCIA: Campo = {
  campo: 'nroLicencia',
  titulo: 'Número de licencia',
  requerido: true,
  min: 3,
  max: 30,
  patron: '^[A-Za-z0-9-]+$',
  mitad: true,
  mensajes: {
    requerido: 'El número de licencia es obligatorio',
    min: MENSAJE_LICENCIA,
    max: MENSAJE_LICENCIA,
    patron: MENSAJE_LICENCIA,
  },
}

const CAMPO_CONTRATACION: Campo = {
  campo: 'fechaContratacion',
  titulo: 'Fecha de contratación',
  tipo: 'fecha',
  porDefecto: 'hoy',
  // * Hoy es válido (la contratación empieza siendo hoy): por eso `hastaHoy` y no `fechaNoFutura`
  validaciones: ['hastaHoy'],
  mitad: true,
  mensajes: { hastaHoy: 'La fecha de contratación no puede ser futura' },
}

// * Las categorías salen del catálogo de Escuela. `SIN` (sin categoría) no se ofrece: un instructor
// * enseña al menos una categoría real. La edición trae `categorias: [{ sigla, descripcion }]`.
const CAMPO_CATEGORIAS: Campo = {
  campo: 'categorias',
  titulo: 'Categorías que enseña',
  tipo: 'seleccion',
  multiple: true,
  requerido: true,
  valorDe: 'sigla',
  opcionesDe: {
    recurso: 'categorias-licencia',
    valor: 'sigla',
    etiqueta: 'descripcion',
    excluir: ['SIN'],
  },
  mensajes: { requerido: 'Elige al menos una categoría' },
}

const CAMPO_UBICACION: Campo = {
  campo: 'ubicacion',
  titulo: 'Ubicación',
  tipo: 'ubicacion',
  requerido: true,
  mensajes: { requerido: 'Elige la ubicación en el mapa' },
}

/**
 * Gestionar instructores (CU09, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * El recurso `instructores` lo atiende Escuela, que le pide a Identidad lo que es de la persona
 * * (cuenta, nombre, foto): el cliente solo conoce el gateway.
 * * Las reglas del alta (CI de 5 a 15, contraseña de 9 a 64, edad mínima de 18, licencia de 3 a 30)
 * * son las de los estudiantes, las de mobile y las del backend, que es la fuente de verdad.
 * * El Administrador solo puede cambiar la ficha profesional y la ubicación de un instructor
 * * existente: nombre, CI y correo se muestran de solo lectura y no viajan.
 * * El vencimiento de la licencia es `fechaFutura` en el alta y, al editar, `soloSiCambia`: así un
 * * instructor con la licencia vencida puede pasar a LICENCIA o INACTIVO sin inventar una fecha.
 * * Los códigos de `erroresDeCampo` son los de `docs/errors/`.
 */
export const ESQUEMA_INSTRUCTORES: EsquemaGestion = {
  recurso: 'instructores',
  titulo: 'Instructores',
  descripcion: 'Instructores de tu escuela.',
  nombreSingular: 'instructor',
  columnas: [
    // * Solo el nombre se estira: las demás columnas son cortas (ver 'La tabla' en GESTION_FRONTEND.md)
    { campo: 'fotoUrl', titulo: 'Foto', tipo: 'foto', nombre: 'nombreCompleto' },
    { campo: 'nombreCompleto', titulo: 'Nombre', principal: true },
    { campo: 'nroLicencia', titulo: 'Licencia', codigo: true, ancho: '9rem' },
    { campo: 'licenciaVence', titulo: 'Vence', tipo: 'fecha', ancho: '9.5rem' },
    {
      campo: 'categorias',
      titulo: 'Categorías',
      tipo: 'etiquetas',
      etiqueta: 'sigla',
      ancho: '11rem',
    },
    { campo: 'estado', titulo: 'Estado', tipo: 'insignia', insignias: ESTADOS, ancho: '7rem' },
  ],
  busqueda: { placeholder: 'Buscar por nombre' },
  filtros: [
    {
      clave: 'filter[estado]',
      titulo: 'Estado',
      todas: 'Todos',
      opciones: [
        { valor: 'ACTIVO', etiqueta: 'Activos' },
        { valor: 'LICENCIA', etiqueta: 'En licencia' },
        { valor: 'INACTIVO', etiqueta: 'Inactivos' },
      ],
    },
  ],
  detalle: {
    titulo: 'Datos del instructor',
    secciones: [
      {
        titulo: 'Datos personales',
        campos: [
          { campo: 'fotoUrl', titulo: 'Foto', tipo: 'foto', nombre: 'nombreCompleto' },
          { campo: 'nombreCompleto', titulo: 'Nombre' },
          { campo: 'edad', titulo: 'Edad', mitad: true },
          { campo: 'fechaNacimiento', titulo: 'Fecha de nacimiento', tipo: 'fecha', mitad: true },
          {
            campo: 'sexo',
            titulo: 'Sexo',
            tipo: 'insignia',
            insignias: { MASCULINO: { etiqueta: 'Masculino' }, FEMENINO: { etiqueta: 'Femenino' } },
            mitad: true,
          },
          { campo: 'ci', titulo: 'CI', mitad: true },
        ],
      },
      {
        titulo: 'Contacto',
        campos: [
          { campo: 'email', titulo: 'Correo' },
          { campo: 'telefono', titulo: 'Teléfono' },
        ],
      },
      {
        titulo: 'Ubicación',
        campos: [
          { campo: 'direccion', titulo: 'Dirección' },
          { campo: 'latitud', titulo: 'Latitud', mitad: true },
          { campo: 'longitud', titulo: 'Longitud', mitad: true },
        ],
      },
      {
        titulo: 'Ficha profesional',
        campos: [
          { campo: 'nroLicencia', titulo: 'Número de licencia', mitad: true },
          { campo: 'licenciaVence', titulo: 'Vencimiento', tipo: 'fecha', mitad: true },
          { campo: 'fechaContratacion', titulo: 'Contratación', tipo: 'fecha', mitad: true },
          { campo: 'fechaAlta', titulo: 'Alta en la plataforma', tipo: 'fecha', mitad: true },
          { campo: 'categorias', titulo: 'Categorías', tipo: 'etiquetas', etiqueta: 'sigla' },
          { campo: 'estado', titulo: 'Estado', tipo: 'insignia', insignias: ESTADOS },
        ],
      },
    ],
  },
  crear: {
    etiqueta: 'Nuevo instructor',
    titulo: 'Nuevo instructor',
    descripcion: 'Se crea la cuenta del instructor en tu escuela y su ficha profesional.',
    etiquetaEnviar: 'Crear instructor',
    mensajeExito: 'Instructor creado correctamente.',
    secciones: [
      {
        titulo: 'Datos personales',
        campos: [
          {
            campo: 'nombres',
            titulo: 'Nombres',
            requerido: true,
            max: 80,
            mitad: true,
            mensajes: { requerido: 'Los nombres son obligatorios' },
          },
          {
            campo: 'apellidos',
            titulo: 'Apellidos',
            requerido: true,
            max: 80,
            mitad: true,
            mensajes: { requerido: 'Los apellidos son obligatorios' },
          },
          {
            campo: 'fechaNacimiento',
            titulo: 'Fecha de nacimiento',
            tipo: 'fecha',
            requerido: true,
            validaciones: ['fechaNoFutura', 'edadMinima:18'],
            mitad: true,
            mensajes: {
              requerido: 'La fecha de nacimiento es obligatoria',
              fechaNoFutura: 'La fecha de nacimiento debe estar en el pasado',
              edadMinima: 'El instructor debe tener al menos 18 años',
            },
          },
          {
            campo: 'sexo',
            titulo: 'Sexo',
            tipo: 'seleccion',
            requerido: true,
            opciones: OPCIONES_SEXO,
            mitad: true,
            mensajes: { requerido: 'Elige el sexo' },
          },
          {
            campo: 'ci',
            titulo: 'CI',
            requerido: true,
            min: 5,
            max: 15,
            patron: '^[A-Za-z0-9-]+$',
            mitad: true,
            mensajes: {
              requerido: 'El CI es obligatorio',
              min: MENSAJE_CI,
              max: MENSAJE_CI,
              patron: MENSAJE_CI,
            },
          },
          { campo: 'telefono', titulo: 'Teléfono', tipo: 'telefono', max: 20, mitad: true },
        ],
      },
      {
        titulo: 'Acceso',
        campos: [
          {
            campo: 'email',
            titulo: 'Correo',
            tipo: 'email',
            requerido: true,
            max: 120,
            mensajes: { requerido: 'El correo es obligatorio' },
          },
          {
            campo: 'password',
            titulo: 'Contraseña inicial',
            tipo: 'password',
            requerido: true,
            min: 9,
            max: 64,
            ayuda: 'La escribes tú y se la comunicas al instructor.',
            mensajes: {
              requerido: 'La contraseña es obligatoria',
              min: 'La contraseña debe tener entre 9 y 64 caracteres',
              max: 'La contraseña debe tener entre 9 y 64 caracteres',
            },
          },
        ],
      },
      {
        titulo: 'Ubicación',
        campos: [
          {
            ...CAMPO_UBICACION,
            ayuda: 'Marca en el mapa dónde vive el instructor. La dirección se calcula sola.',
          },
        ],
      },
      {
        titulo: 'Ficha profesional',
        campos: [
          CAMPO_LICENCIA,
          {
            campo: 'licenciaVence',
            titulo: 'Vencimiento de la licencia',
            tipo: 'fecha',
            requerido: true,
            validaciones: ['fechaFutura'],
            mitad: true,
            mensajes: {
              requerido: 'El vencimiento de la licencia es obligatorio',
              fechaFutura: 'La licencia debe vencer después de hoy',
            },
          },
          CAMPO_CONTRATACION,
          CAMPO_CATEGORIAS,
        ],
      },
    ],
  },
  editar: {
    titulo: 'Editar instructor',
    descripcion: 'Solo puedes corregir su ficha profesional y su ubicación.',
    etiquetaEnviar: 'Guardar',
    metodo: 'PATCH',
    mensajeExito: 'Instructor actualizado correctamente.',
    secciones: [
      {
        titulo: 'Instructor',
        campos: [
          { campo: 'nombreCompleto', titulo: 'Nombre', soloLectura: true },
          { campo: 'ci', titulo: 'CI', soloLectura: true, mitad: true },
          { campo: 'email', titulo: 'Correo', soloLectura: true, mitad: true },
        ],
      },
      {
        titulo: 'Ficha profesional',
        campos: [
          CAMPO_LICENCIA,
          {
            campo: 'licenciaVence',
            titulo: 'Vencimiento de la licencia',
            tipo: 'fecha',
            requerido: true,
            validaciones: ['fechaFutura'],
            // * Solo se exige futuro si el Administrador cambia la fecha: una licencia ya vencida
            // * no impide pasar al instructor a LICENCIA o INACTIVO
            soloSiCambia: true,
            mitad: true,
            mensajes: {
              requerido: 'El vencimiento de la licencia es obligatorio',
              fechaFutura: 'La licencia debe vencer después de hoy',
            },
          },
          CAMPO_CONTRATACION,
          {
            campo: 'estado',
            titulo: 'Estado',
            tipo: 'seleccion',
            requerido: true,
            opciones: OPCIONES_ESTADO,
            mitad: true,
            mensajes: { requerido: 'Elige el estado' },
          },
          CAMPO_CATEGORIAS,
        ],
      },
      {
        titulo: 'Ubicación',
        campos: [{ ...CAMPO_UBICACION, direccionDe: 'direccion' }],
      },
    ],
  },
  erroresDeCampo: {
    ERR_IDE_01: { campo: 'email', mensaje: 'Este correo ya está registrado' },
    ERR_IDE_02: { campo: 'fechaNacimiento', mensaje: 'El instructor debe tener al menos 18 años' },
    ERR_IDE_19: { campo: 'ci', mensaje: 'Este CI ya está registrado' },
    ERR_ESC_12: { campo: 'nroLicencia', mensaje: 'Este número de licencia ya está en uso' },
    ERR_ESC_14: { campo: 'categorias', mensaje: 'Una de las categorías no es válida' },
  },
  mensajes: {
    vacio: 'Todavía no hay instructores en tu escuela.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar el instructor. Inténtalo de nuevo más tarde.',
  },
}
