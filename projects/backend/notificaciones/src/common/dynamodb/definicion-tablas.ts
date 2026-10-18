import type { CreateTableCommandInput } from '@aws-sdk/client-dynamodb'

/**
 * Definición de las tablas de DynamoDB del servicio: el equivalente a las
 * migraciones Flyway de los servicios PostgreSQL.
 * Fuente: docs/DriveHub/DriveHub_Diseno_Base_de_Datos.pdf (sección 6).
 *
 * * DynamoDB no tiene id autoincremental: la "clave primaria" ES la clave de
 * * partición (+ clave de orden). Las claves usan el codigo público (UUID v7)
 * * de la persona, y cada ítem lleva su propio codigo{Tabla} (UUID v7), que
 * * por ser ordenable por tiempo también sirve dentro de la clave de orden.
 * * Todas las consultas son por usuario y fecha: clave de partición
 * * usuarioCodigo + clave de orden '<fecha ISO>#<codigo>'.
 */
export interface DefinicionTabla {
  tabla: CreateTableCommandInput
  // Atributo con la fecha de expiración (epoch en segundos), si la tabla usa TTL.
  atributoTtl?: string
}

export const TABLA_NOTIFICACIONES = 'Notificaciones'
export const TABLA_PREFERENCIAS = 'PreferenciasNotificacion'
export const TABLA_DISPOSITIVOS = 'DispositivosPush'
export const TABLA_EVENTOS_PROCESADOS = 'EventosProcesados'
export const INDICE_NO_LEIDAS = 'NoLeidas'

export const DEFINICION_TABLAS: DefinicionTabla[] = [
  {
    // Historial por usuario (CU36, CU38). Expira a los 180 días.
    tabla: {
      TableName: TABLA_NOTIFICACIONES,
      BillingMode: 'PAY_PER_REQUEST',
      AttributeDefinitions: [
        { AttributeName: 'usuarioCodigo', AttributeType: 'S' },
        { AttributeName: 'sk', AttributeType: 'S' },
        { AttributeName: 'noLeidaTs', AttributeType: 'S' },
      ],
      KeySchema: [
        { AttributeName: 'usuarioCodigo', KeyType: 'HASH' },
        { AttributeName: 'sk', KeyType: 'RANGE' },
      ],
      // Índice disperso: noLeidaTs solo existe mientras la notificación no se lea,
      // así el índice lista únicamente las no leídas.
      GlobalSecondaryIndexes: [
        {
          IndexName: INDICE_NO_LEIDAS,
          KeySchema: [
            { AttributeName: 'usuarioCodigo', KeyType: 'HASH' },
            { AttributeName: 'noLeidaTs', KeyType: 'RANGE' },
          ],
          Projection: { ProjectionType: 'ALL' },
        },
      ],
    },
    atributoTtl: 'ttl',
  },
  {
    // Preferencias del usuario (CU37). Un ítem por usuario.
    tabla: {
      TableName: TABLA_PREFERENCIAS,
      BillingMode: 'PAY_PER_REQUEST',
      AttributeDefinitions: [{ AttributeName: 'usuarioCodigo', AttributeType: 'S' }],
      KeySchema: [{ AttributeName: 'usuarioCodigo', KeyType: 'HASH' }],
    },
  },
  {
    // Tokens de la app móvil (CU36).
    tabla: {
      TableName: TABLA_DISPOSITIVOS,
      BillingMode: 'PAY_PER_REQUEST',
      AttributeDefinitions: [
        { AttributeName: 'usuarioCodigo', AttributeType: 'S' },
        { AttributeName: 'codigoDispositivoPush', AttributeType: 'S' },
      ],
      KeySchema: [
        { AttributeName: 'usuarioCodigo', KeyType: 'HASH' },
        { AttributeName: 'codigoDispositivoPush', KeyType: 'RANGE' },
      ],
    },
  },
  {
    // Idempotencia de RabbitMQ (CU36): el broker puede entregar el mismo
    // evento más de una vez. Expira a los 7 días.
    tabla: {
      TableName: TABLA_EVENTOS_PROCESADOS,
      BillingMode: 'PAY_PER_REQUEST',
      AttributeDefinitions: [{ AttributeName: 'eventoCodigo', AttributeType: 'S' }],
      KeySchema: [{ AttributeName: 'eventoCodigo', KeyType: 'HASH' }],
    },
    atributoTtl: 'ttl',
  },
]
