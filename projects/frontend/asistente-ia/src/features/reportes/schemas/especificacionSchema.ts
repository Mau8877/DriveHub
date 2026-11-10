import { z } from 'zod'

/**
 * La especificación de un reporte (CU52), con las mismas reglas que valida el backend
 * (`reporte_schemas.py` y `ReporteService.validar`): el backend manda, esto es para avisar antes.
 */
export const FORMATOS = ['pdf', 'xlsx'] as const

const fecha = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, 'La fecha no es válida')
  .nullable()
  .optional()

export const especificacionSchema = z
  .object({
    fuente: z.string().min(1, 'Elige qué datos quieres'),
    titulo: z.string().trim().min(1, 'El título es obligatorio').max(120, 'El título es demasiado largo'),
    columnas: z.array(z.string()).min(1, 'Elige al menos una columna').max(20, 'Son demasiadas columnas'),
    filtros: z.object({
      desde: fecha,
      hasta: fecha,
      estado: z.string().nullable().optional(),
      tipo: z.string().nullable().optional(),
    }),
    orden: z
      .object({ columna: z.string(), descendente: z.boolean() })
      .nullable()
      .optional(),
    formato: z.enum(FORMATOS),
  })
  .refine((e) => !e.filtros.desde || !e.filtros.hasta || e.filtros.desde <= e.filtros.hasta, {
    message: 'La fecha inicial no puede ser posterior a la final',
    path: ['filtros', 'hasta'],
  })

export type EspecificacionReporte = z.infer<typeof especificacionSchema>

/** El pedido escrito o dictado: igual que `InterpretarRequest` del backend. */
export const pedidoSchema = z
  .string()
  .trim()
  .min(3, 'Escribe o dicta qué reporte quieres')
  .max(500, 'El pedido es demasiado largo (máximo 500 caracteres)')
