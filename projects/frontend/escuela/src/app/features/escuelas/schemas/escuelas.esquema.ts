import type { EsquemaGestion } from '@drivehub/gestion'

/**
 * Gestionar escuelas (CU05, Super admin) como esquema de <dh-gestion>.
 *
 * * Las reglas del Administrador (contraseña de 9 a 64, edad mínima de 18, longitudes) son las del
 * * registro del estudiante de `identidad`: tienen que seguir iguales a esas, a las del backend y a
 * * las de mobile. El backend es la fuente de verdad.
 * * Los códigos de `erroresDeCampo` son los del backend: ERR_ESC_03 (slug en uso), ERR_IDE_01
 * * (correo en uso) y ERR_IDE_02 (el administrador no alcanza la edad mínima).
 */
export const ESQUEMA_ESCUELAS: EsquemaGestion = {
  recurso: 'escuelas',
  titulo: 'Gestionar escuelas',
  descripcion: 'Escuelas activas e inactivas de la plataforma.',
  nombreSingular: 'escuela',
  columnas: [
    { campo: 'nombre', titulo: 'Nombre', principal: true, icono: 'school' },
    { campo: 'slug', titulo: 'Slug', secundaria: true, codigo: true },
    {
      campo: 'estado',
      titulo: 'Estado',
      tipo: 'insignia',
      insignias: {
        ACTIVA: { etiqueta: 'Activa', tono: 'exito' },
        INACTIVA: { etiqueta: 'Inactiva' },
      },
    },
    { campo: 'creadoEn', titulo: 'Alta', tipo: 'fecha', secundaria: true },
  ],
  busqueda: { placeholder: 'Buscar por nombre o slug' },
  filtros: [
    {
      clave: 'filter[estado]',
      titulo: 'Estado',
      opciones: [
        { valor: 'ACTIVA', etiqueta: 'Activas' },
        { valor: 'INACTIVA', etiqueta: 'Inactivas' },
      ],
    },
  ],
  crear: {
    etiqueta: 'Nueva escuela',
    titulo: 'Nueva escuela',
    descripcion: 'Se crea la escuela junto con la cuenta de su Administrador.',
    etiquetaEnviar: 'Crear escuela',
    mensajeExito: 'Escuela creada correctamente.',
    secciones: [
      {
        titulo: 'Escuela',
        campos: [
          {
            campo: 'nombre',
            titulo: 'Nombre',
            requerido: true,
            min: 3,
            max: 120,
            mensajes: { requerido: 'El nombre es obligatorio' },
          },
          {
            campo: 'slug',
            titulo: 'Slug (enlace de registro)',
            requerido: true,
            min: 3,
            max: 60,
            patron: 'slug',
            sugeridoDe: 'nombre',
            ayuda: 'Va en el enlace de registro de los estudiantes y no se puede cambiar después.',
            mensajes: { requerido: 'El slug es obligatorio' },
          },
        ],
      },
      {
        titulo: 'Administrador',
        campos: [
          {
            campo: 'administrador.nombres',
            titulo: 'Nombres',
            requerido: true,
            max: 80,
            mitad: true,
            mensajes: { requerido: 'Los nombres son obligatorios' },
          },
          {
            campo: 'administrador.apellidos',
            titulo: 'Apellidos',
            requerido: true,
            max: 80,
            mitad: true,
            mensajes: { requerido: 'Los apellidos son obligatorios' },
          },
          {
            campo: 'administrador.fechaNacimiento',
            titulo: 'Fecha de nacimiento',
            tipo: 'fecha',
            requerido: true,
            validaciones: ['fechaNoFutura', 'edadMinima:18'],
            mitad: true,
            mensajes: {
              requerido: 'La fecha de nacimiento es obligatoria',
              fechaNoFutura: 'La fecha de nacimiento debe estar en el pasado',
              edadMinima: 'El administrador debe tener al menos 18 años',
            },
          },
          {
            campo: 'administrador.telefono',
            titulo: 'Teléfono',
            tipo: 'telefono',
            max: 20,
            mitad: true,
          },
          {
            campo: 'administrador.email',
            titulo: 'Correo',
            tipo: 'email',
            requerido: true,
            max: 120,
            mensajes: { requerido: 'El correo es obligatorio' },
          },
          {
            campo: 'administrador.password',
            titulo: 'Contraseña inicial',
            tipo: 'password',
            requerido: true,
            min: 9,
            max: 64,
            mensajes: {
              requerido: 'La contraseña es obligatoria',
              min: 'La contraseña debe tener entre 9 y 64 caracteres',
              max: 'La contraseña debe tener entre 9 y 64 caracteres',
            },
            ayuda: 'Entre 9 y 64 caracteres.',
          },
        ],
      },
    ],
  },
  editar: {
    titulo: 'Editar escuela',
    etiquetaEnviar: 'Guardar',
    // La consulta por código devuelve la escuela en cualquier estado (también si está inactiva)
    cargar: 'por-codigo/{id}',
    mensajeExito: 'Escuela actualizada correctamente.',
    visibleSi: { campo: 'estado', igual: 'ACTIVA' },
    bloqueo: {
      campo: 'activa',
      valor: false,
      mensaje: 'La escuela está inactiva. Reactívala para editarla.',
    },
    secciones: [
      {
        campos: [
          {
            campo: 'slug',
            titulo: 'Slug (no se puede cambiar)',
            soloLectura: true,
            requerido: true,
          },
          {
            campo: 'nombre',
            titulo: 'Nombre',
            requerido: true,
            min: 3,
            max: 120,
            mensajes: { requerido: 'El nombre es obligatorio' },
          },
        ],
      },
    ],
  },
  acciones: [
    {
      id: 'desactivar',
      etiqueta: 'Desactivar',
      metodo: 'DELETE',
      ruta: '{id}',
      icono: 'power',
      confirmar: '¿Desactivar la escuela?',
      confirmarTexto: 'Sus cuentas no podrán iniciar sesión hasta que la reactives.',
      mensajeExito: 'Escuela desactivada correctamente.',
      visibleSi: { campo: 'estado', igual: 'ACTIVA' },
      tono: 'peligro',
    },
    {
      id: 'reactivar',
      etiqueta: 'Reactivar',
      icono: 'refresh',
      metodo: 'POST',
      ruta: '{id}/reactivar',
      mensajeExito: 'Escuela reactivada correctamente.',
      visibleSi: { campo: 'estado', igual: 'INACTIVA' },
    },
  ],
  erroresDeCampo: {
    ERR_ESC_03: { campo: 'slug', mensaje: 'Ese slug ya está en uso' },
    ERR_IDE_01: { campo: 'administrador.email', mensaje: 'Este correo ya está registrado' },
    ERR_IDE_02: {
      campo: 'administrador.fechaNacimiento',
      mensaje: 'El administrador debe tener al menos 18 años',
    },
  },
  mensajes: {
    vacio: 'Todavía no hay escuelas.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar la escuela. Inténtalo de nuevo más tarde.',
  },
}
