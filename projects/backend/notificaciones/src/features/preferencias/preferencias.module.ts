import { Module } from '@nestjs/common'

import { SecurityModule } from '../../common/security/security.module.js'
import { PreferenciasController } from './controllers/preferencias.controller.js'
import { PreferenciasRepository } from './repositories/preferencias.repository.js'
import { PreferenciasService } from './services/preferencias.service.js'

@Module({
  imports: [SecurityModule],
  controllers: [PreferenciasController],
  providers: [PreferenciasService, PreferenciasRepository],
  exports: [PreferenciasRepository],
})
export class PreferenciasModule {}
