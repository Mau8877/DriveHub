import { Module } from '@nestjs/common'
import { ConfigModule } from '@nestjs/config'

import { DynamoDbModule } from './common/dynamodb/dynamodb.module.js'
import { DispositivosModule } from './features/dispositivos/dispositivos.module.js'
import { NotificacionesModule } from './features/notificaciones/notificaciones.module.js'
import { HealthController } from './health/health.controller.js'

/**
 * Módulo raíz. Cada feature suma acá su propio *.module.ts cuando tiene su
 * primer controller/service. Hoy: notificaciones y dispositivos (CU36);
 * preferencias solo se usa por dentro (lectura) hasta CU37.
 */
@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    DynamoDbModule,
    DispositivosModule,
    NotificacionesModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
