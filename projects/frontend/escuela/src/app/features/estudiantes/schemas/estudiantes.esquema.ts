import type { EsquemaGestion, Insignia } from '@drivehub/gestion'

const ESTADOS: Record<string, Insignia> = {
  ACTIVO: { etiqueta: 'Activo', tono: 'exito' },
  INACTIVO: { etiqueta: 'Inactivo' },
}

const OPCIONES_SEXO = [
  { valor: 'MASCULINO', etiqueta: 'Masculino' },
  { valor: 'FEMENINO', etiqueta: 'Femenino' },
]

const MENSAJE_CI = 'El CI debe tener entre 5 y 15 caracteres: letras, números y guion'

// * Las categorías salen del catálogo de Escuela: una categoría nueva aparece sin tocar la web
const CATEGORIAS = { recurso: 'categorias-licencia', valor: 'sigla', etiqueta: 'descripcion' }

/**
 * Gestionar estudiantes (CU08, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * El recurso `estudiantes` lo atiende Identidad (un estudiante es una cuenta con su perfil, CU08a),
 * * aunque la pantalla viva en el remoto `escuela`: el cliente solo conoce el gateway.
 * * Las reglas de validación del alta (CI de 5 a 15, contraseña de 9 a 64, edad mínima de 18,
 * * longitudes) son las del registro del estudiante, las de la web de registro y las de mobile:
 * * tienen que seguir iguales a esas y a las del backend, que es la fuente de verdad.
 * * El Administrador solo puede cambiar la categoría y la ubicación de un estudiante existente: los
 * * datos personales son del propio estudiante (CU06). Por eso en `editar` solo esos dos viajan.
 * * Los códigos de `erroresDeCampo` son los de `docs/errors/`.
 * * TODO: columna y filtro de saldo, y montos pagados y clases tomadas en el detalle, cuando exista
 * * Pagos (ver "Pendiente para cuando exista Pagos" en el CU08). Hoy no hay nada de saldo.
 */
export const ESQUEMA_ESTUDIANTES: EsquemaGestion = {
  recurso: 'estudiantes',
  titulo: 'Gestionar estudiantes',
  descripcion: 'Estudiantes de tu escuela.',
  nombreSingular: 'estudiante',
  columnas: [
    // * Solo el nombre se estira: las demás columnas son cortas y quedan juntas (ver 'La tabla' en GESTION_FRONTEND.md)
    // * La foto es la URL prefirmada que trae el listado (vale 60 minutos); sin foto, las iniciales del nombre
    { campo: 'fotoUrl', titulo: 'Foto', tipo: 'foto', nombre: 'nombreCompleto' },
    { campo: 'nombreCompleto', titulo: 'Nombre', principal: true },
    { campo: 'edad', titulo: 'Edad', ancho: '5rem' },
    { campo: 'categoriaSigla', titulo: 'Categoría', codigo: true, ancho: '7rem' },
    { campo: 'fechaIncorporacion', titulo: 'Incorporación', tipo: 'fecha', ancho: '9.5rem' },
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
        { valor: 'INACTIVO', etiqueta: 'Inactivos' },
      ],
    },
  ],
  detalle: {
    titulo: 'Datos del estudiante',
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
          { campo: 'direccion', titulo: 'Dirección' },
        ],
      },
      {
        titulo: 'En la escuela',
        campos: [
          { campo: 'categoriaSigla', titulo: 'Categoría', mitad: true },
          { campo: 'estado', titulo: 'Estado', tipo: 'insignia', insignias: ESTADOS, mitad: true },
          { campo: 'fechaIncorporacion', titulo: 'Incorporación', tipo: 'fecha' },
        ],
      },
    ],
  },
  crear: {
    etiqueta: 'Nuevo estudiante',
    titulo: 'Nuevo estudiante',
    descripcion: 'Se crea la cuenta del estudiante en tu escuela.',
    etiquetaEnviar: 'Crear estudiante',
    mensajeExito: 'Estudiante creado correctamente.',
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
              edadMinima: 'El estudiante debe tener al menos 18 años',
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
            ayuda: 'La escribes tú y se la comunicas al estudiante.',
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
            campo: 'ubicacion',
            titulo: 'Ubicación',
            tipo: 'ubicacion',
            requerido: true,
            ayuda: 'Marca en el mapa dónde vive el estudiante. La dirección se calcula sola.',
            mensajes: { requerido: 'Elige la ubicación en el mapa' },
          },
        ],
      },
      {
        titulo: 'Licencia',
        campos: [
          {
            campo: 'categoriaSigla',
            titulo: 'Categoría',
            tipo: 'seleccion',
            opcionesDe: CATEGORIAS,
            porDefecto: 'SIN',
          },
        ],
      },
    ],
  },
  editar: {
    titulo: 'Editar estudiante',
    descripcion: 'Solo puedes corregir su categoría y su ubicación.',
    etiquetaEnviar: 'Guardar',
    metodo: 'PATCH',
    mensajeExito: 'Estudiante actualizado correctamente.',
    secciones: [
      {
        titulo: 'Estudiante',
        campos: [
          { campo: 'nombreCompleto', titulo: 'Nombre', soloLectura: true },
          { campo: 'ci', titulo: 'CI', soloLectura: true, mitad: true },
          { campo: 'email', titulo: 'Correo', soloLectura: true, mitad: true },
        ],
      },
      {
        titulo: 'Licencia',
        campos: [
          {
            campo: 'categoriaSigla',
            titulo: 'Categoría',
            tipo: 'seleccion',
            opcionesDe: CATEGORIAS,
          },
        ],
      },
      {
        titulo: 'Ubicación',
        campos: [
          {
            campo: 'ubicacion',
            titulo: 'Ubicación',
            tipo: 'ubicacion',
            requerido: true,
            direccionDe: 'direccion',
            mensajes: { requerido: 'Elige la ubicación en el mapa' },
          },
        ],
      },
    ],
  },
  erroresDeCampo: {
    ERR_IDE_01: { campo: 'email', mensaje: 'Este correo ya está registrado' },
    ERR_IDE_02: { campo: 'fechaNacimiento', mensaje: 'El estudiante debe tener al menos 18 años' },
    ERR_IDE_19: { campo: 'ci', mensaje: 'Este CI ya está registrado' },
    ERR_IDE_21: { campo: 'categoriaSigla', mensaje: 'La categoría ya no existe' },
  },
  mensajes: {
    vacio: 'Todavía no hay estudiantes en tu escuela.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar el estudiante. Inténtalo de nuevo más tarde.',
  },
}
