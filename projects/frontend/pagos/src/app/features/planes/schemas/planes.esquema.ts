import type { Campo, EsquemaGestion, Insignia } from '@drivehub/gestion'

const ESTADOS: Record<string, Insignia> = {
  true: { etiqueta: 'Activo', tono: 'exito' },
  false: { etiqueta: 'Desactivado' },
}

// * Mismas reglas que el backend (que manda): nombre de 1 a 80, horas enteras de 1 a 500 y costo mayor
// * a 0 con hasta dos decimales. Se declaran UNA vez: crear y editar usan los mismos campos.
const CAMPOS: Campo[] = [
  {
    campo: 'nombre',
    titulo: 'Nombre',
    requerido: true,
    max: 80,
    mensajes: { requerido: 'El nombre es obligatorio' },
  },
  {
    campo: 'horas',
    titulo: 'Horas',
    tipo: 'numero',
    requerido: true,
    min: 1,
    max: 500,
    mitad: true,
    mensajes: {
      requerido: 'Las horas son obligatorias',
      patron: 'Las horas deben ser un número entero de 1 a 500',
      min: 'Las horas deben ser un número entero de 1 a 500',
      max: 'Las horas deben ser un número entero de 1 a 500',
    },
  },
  {
    campo: 'costoBs',
    titulo: 'Costo en bolivianos (Bs)',
    tipo: 'decimal',
    decimales: 2,
    requerido: true,
    min: 0.01,
    max: 99999999.99,
    mitad: true,
    ayuda: 'El equivalente en dólares se calcula solo.',
    mensajes: {
      requerido: 'El costo es obligatorio',
      patron: 'El costo debe ser un número con hasta 2 decimales',
      min: 'El costo debe ser mayor a 0',
      max: 'El costo no puede superar 99999999,99',
    },
  },
]

/**
 * Gestionar planes de horas (CU25, Administrador de escuela) como esquema de <dh-gestion>.
 *
 * * El recurso `planes-pago` lo atiende Pagos; el cliente solo conoce el gateway.
 * * El costo en dólares (`costoUsd`) es solo una referencia que calcula el backend con el tipo de cambio
 * * configurado: es una columna de la tabla y NO un campo del formulario.
 * * Un plan no se borra: se desactiva (deja de ofrecerse pero conserva su historia) y se reactiva.
 * * Sin detalle de solo lectura: el formulario de edición ya muestra todos los datos del plan.
 * * Los códigos de `erroresDeCampo` son los de `docs/errors/`.
 */
export const ESQUEMA_PLANES: EsquemaGestion = {
  recurso: 'planes-pago',
  titulo: 'Planes de horas',
  descripcion: 'Planes de horas de tu escuela.',
  nombreSingular: 'plan',
  columnas: [
    { campo: 'nombre', titulo: 'Nombre', principal: true, icono: 'receipt' },
    { campo: 'horas', titulo: 'Horas', ancho: '6rem' },
    { campo: 'costoBs', titulo: 'Costo (Bs)', tipo: 'moneda', moneda: 'BOB' },
    { campo: 'costoUsd', titulo: 'Costo (USD)', tipo: 'moneda', moneda: 'USD' },
    { campo: 'activo', titulo: 'Estado', tipo: 'insignia', insignias: ESTADOS, ancho: '8rem' },
  ],
  busqueda: { placeholder: 'Buscar por nombre' },
  filtros: [
    {
      clave: 'filter[activo]',
      titulo: 'Estado',
      todas: 'Todos',
      opciones: [
        { valor: 'true', etiqueta: 'Activos' },
        { valor: 'false', etiqueta: 'Desactivados' },
      ],
    },
  ],
  crear: {
    etiqueta: 'Nuevo plan',
    titulo: 'Nuevo plan',
    descripcion: 'Se ofrece a los estudiantes de tu escuela apenas lo creas.',
    etiquetaEnviar: 'Crear plan',
    mensajeExito: 'Plan creado correctamente.',
    secciones: [{ campos: CAMPOS }],
  },
  editar: {
    titulo: 'Editar plan',
    descripcion: 'Cambiar un plan no afecta lo que ya contrataron los estudiantes.',
    etiquetaEnviar: 'Guardar',
    metodo: 'PATCH',
    mensajeExito: 'Plan actualizado correctamente.',
    secciones: [{ campos: CAMPOS }],
  },
  acciones: [
    {
      id: 'desactivar',
      etiqueta: 'Desactivar',
      metodo: 'POST',
      ruta: '{id}/desactivar',
      icono: 'power',
      tono: 'peligro',
      confirmar: '¿Desactivar el plan?',
      confirmarTexto:
        'Deja de ofrecerse a los estudiantes, pero conserva su historia y puedes reactivarlo cuando quieras.',
      mensajeExito: 'Plan desactivado correctamente.',
      visibleSi: { campo: 'activo', igual: true },
    },
    {
      id: 'activar',
      etiqueta: 'Reactivar',
      metodo: 'POST',
      ruta: '{id}/activar',
      icono: 'refresh',
      mensajeExito: 'Plan reactivado correctamente.',
      visibleSi: { campo: 'activo', igual: false },
    },
  ],
  erroresDeCampo: {
    ERR_PAG_01: { campo: 'nombre', mensaje: 'Ya existe un plan con ese nombre en tu escuela' },
  },
  mensajes: {
    vacio: 'Todavía no hay planes en tu escuela.',
    sinResultados: 'Sin resultados.',
    errorGuardar: 'No pudimos guardar el plan. Inténtalo de nuevo más tarde.',
  },
}
