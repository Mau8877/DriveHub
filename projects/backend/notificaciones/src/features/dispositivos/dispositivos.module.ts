import { Module } from '@nestjs/common'

import { SecurityModule } from '../../common/security/security.module.js'
import { DispositivosController } from './controllers/dispositivos.controller.js'
import { DispositivosResolver } from './graphql/dispositivos.resolver.js'
import { DispositivosRepository } from './repositories/dispositivos.repository.js'
import { DispositivosService } from './services/dispositivos.service.js'

@Module({
  imports: [SecurityModule],
  controllers: [DispositivosController],
  providers: [DispositivosService, DispositivosRepository, DispositivosResolver],
  exports: [DispositivosRepository],
})
export class DispositivosModule {}
