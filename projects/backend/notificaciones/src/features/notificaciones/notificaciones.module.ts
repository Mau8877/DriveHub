import { Module } from '@nestjs/common'
import { ConfigService } from '@nestjs/config'

import { IdempotenciaModule } from '../../common/idempotencia/idempotencia.module.js'
import { SecurityModule } from '../../common/security/security.module.js'
import { DispositivosModule } from '../dispositivos/dispositivos.module.js'
import { PreferenciasModule } from '../preferencias/preferencias.module.js'
import { SolicitudNotificacionConsumer } from './consumers/solicitud-notificacion.consumer.js'
import { NotificacionesController } from './controllers/notificaciones.controller.js'
import { NotificacionesInternasController } from './controllers/notificaciones-internas.controller.js'
import { NotificacionesResolver } from './graphql/notificaciones.resolver.js'
import { NotificacionesRepository } from './repositories/notificaciones.repository.js'
import { EnvioNotificacionService } from './services/envio-notificacion.service.js'
import { HistorialNotificacionesService } from './services/historial-notificaciones.service.js'
import { FirebaseProveedorPush } from './services/firebase-proveedor-push.js'
import { NodemailerProveedorCorreo } from './services/nodemailer-proveedor-correo.js'
import { PROVEEDOR_CORREO } from './services/proveedor-correo.js'
import { PROVEEDOR_PUSH } from './services/proveedor-push.js'

@Module({
  imports: [DispositivosModule, PreferenciasModule, IdempotenciaModule, SecurityModule],
  controllers: [NotificacionesController, NotificacionesInternasController],
  providers: [
    EnvioNotificacionService,
    HistorialNotificacionesService,
    NotificacionesResolver,
    SolicitudNotificacionConsumer,
    NotificacionesRepository,
    { provide: PROVEEDOR_CORREO, useClass: NodemailerProveedorCorreo },
    {
      provide: PROVEEDOR_PUSH,
      inject: [ConfigService],
      useFactory: (config: ConfigService) => new FirebaseProveedorPush(config),
    },
  ],
  exports: [EnvioNotificacionService],
})
export class NotificacionesModule {}
