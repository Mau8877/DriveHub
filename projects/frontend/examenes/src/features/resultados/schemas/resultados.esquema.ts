import type { EsquemaGestion } from '@drivehub/gestion'

const ORIGENES = [
  { valor: 'SIMULACRO', etiqueta: 'Simulacro escrito' },
  { valor: 'PRUEBA_CLASE', etiqueta: 'Prueba de clase' },
  { valor: 'EXAMEN_CONDUCCION', etiqueta: 'Examen de conducción' },
]

/**
 * Resultados y notas de la escuela (CU23, Administrador de escuela) como esquema de <dh-gestion>: solo lectura.
 *
 * * Es el historial consolidado de todas las evaluaciones de los estudiantes: lo escriben los casos de uso que las
 *   cierran (simulacro, examen de conducción) y acá solo se consulta, con filtro por tipo de evaluación.
 * * El nombre del estudiante lo une el backend desde Identidad: no se guarda en Académico.
 */
export const ESQUEMA_RESULTADOS: EsquemaGestion = {
  recurso: 'resultados',
  titulo: 'Resultados y notas',
  descripcion: 'Notas de los simulacros, las pruebas de clase y los exámenes de conducción de tus estudiantes.',
  nombreSingular: 'resultado',
  columnas: [
    { campo: 'estudianteNombre', titulo: 'Estudiante', principal: true },
    { campo: 'titulo', titulo: 'Evaluación', secundaria: true },
    {
      campo: 'origen',
      titulo: 'Tipo',
      tipo: 'insignia',
      insignias: {
        SIMULACRO: { etiqueta: 'Simulacro', tono: 'info' },
        PRUEBA_CLASE: { etiqueta: 'Prueba', tono: 'ambar' },
        EXAMEN_CONDUCCION: { etiqueta: 'Examen', tono: 'info' },
      },
      ancho: '8rem',
    },
    { campo: 'puntaje', titulo: 'Puntaje', secundaria: true, ancho: '7rem' },
    {
      campo: 'aprobado',
      titulo: 'Resultado',
      tipo: 'insignia',
      insignias: {
        true: { etiqueta: 'Aprobado', tono: 'exito' },
        false: { etiqueta: 'No aprobado' },
      },
      ancho: '9rem',
    },
    { campo: 'dia', titulo: 'Fecha', tipo: 'fecha', ancho: '9.5rem' },
  ],
  filtros: [{ clave: 'filter[origen]', titulo: 'Tipo', opciones: ORIGENES }],
  mensajes: {
    vacio: 'Todavía no hay resultados. Aparecerán cuando tus estudiantes terminen una evaluación.',
    sinResultados: 'Sin resultados.',
  },
}
