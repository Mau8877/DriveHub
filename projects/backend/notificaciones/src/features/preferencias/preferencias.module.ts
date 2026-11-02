import { Module } from '@nestjs/common'

import { PreferenciasRepository } from './repositories/preferencias.repository.js'

@Module({
  providers: [PreferenciasRepository],
  exports: [PreferenciasRepository],
})
export class PreferenciasModule {}
