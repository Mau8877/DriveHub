import { Module } from '@nestjs/common'
import { ConfigModule } from '@nestjs/config'

import { DynamoDbModule } from './common/dynamodb/dynamodb.module.js'
import { modulosGraphql } from './common/graphql/graphql-api.module.js'
import { DispositivosModule } from './features/dispositivos/dispositivos.module.js'
import { NotificacionesModule } from './features/notificaciones/notificaciones.module.js'
import { PreferenciasModule } from './features/preferencias/preferencias.module.js'
import { HealthController } from './health/health.controller.js'

/**
 * Módulo raíz. Cada feature suma acá su propio *.module.ts cuando tiene su
 * primer controller/service. Hoy: notificaciones y dispositivos (CU36) y preferencias (CU37).
 */
@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    DynamoDbModule,
    DispositivosModule,
    NotificacionesModule,
    PreferenciasModule,
    // * CU45: GraphQL al lado de REST; GRAPHQL_ENABLED=false lo apaga sin tocar nada más
    ...modulosGraphql(),
  ],
  controllers: [HealthController],
})
export class AppModule {}
