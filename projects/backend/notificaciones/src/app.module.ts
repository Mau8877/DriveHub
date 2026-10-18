import { Module } from '@nestjs/common'
import { ConfigModule } from '@nestjs/config'

import { DynamoDbModule } from './common/dynamodb/dynamodb.module.js'
import { HealthController } from './health/health.controller.js'

/**
 * Módulo raíz. Cada feature (notificaciones, preferencias, dispositivos)
 * suma acá su propio *.module.ts cuando tenga su primer controller/service
 * (hoy solo tienen sus Models: ningún caso de uso implementado todavía).
 */
@Module({
  imports: [ConfigModule.forRoot({ isGlobal: true }), DynamoDbModule],
  controllers: [HealthController],
})
export class AppModule {}
