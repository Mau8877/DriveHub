import { Field, ID, ObjectType, registerEnumType } from '@nestjs/graphql'

import { PageMetaType } from '../../../common/graphql/page-meta.type.js'
import type { NotificacionResponse } from '../dto/notificacion.response.js'
import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'

registerEnumType(TipoNotificacion, { name: 'TipoNotificacion', description: 'Qué originó el aviso' })

/** Un aviso de la bandeja (CU38): los mismos campos que devuelve REST, sin la clave interna ni el canal. */
@ObjectType('Notificacion')
export class NotificacionType implements NotificacionResponse {
  @Field(() => ID)
  codigoNotificacion!: string

  @Field(() => TipoNotificacion)
  tipo!: TipoNotificacion

  @Field()
  titulo!: string

  @Field()
  mensaje!: string

  @Field()
  leida!: boolean

  /** Fecha ISO-8601 en UTC. */
  @Field()
  creadaEn!: string
}

@ObjectType('PaginaNotificaciones')
export class PaginaNotificacionesType {
  @Field(() => [NotificacionType])
  items!: NotificacionType[]

  @Field(() => PageMetaType)
  meta!: PageMetaType
}
