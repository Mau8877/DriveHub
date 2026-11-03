import { Module } from '@nestjs/common'

import { EventosProcesadosRepository } from './eventos-procesados.repository.js'

@Module({
  providers: [EventosProcesadosRepository],
  exports: [EventosProcesadosRepository],
})
export class IdempotenciaModule {}
