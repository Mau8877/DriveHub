import { Field, ID, ObjectType, registerEnumType } from '@nestjs/graphql'

import type { DispositivoResponse } from '../dto/dispositivo.response.js'
import { Plataforma } from '../enums/plataforma.enum.js'

registerEnumType(Plataforma, { name: 'Plataforma', description: 'Dónde corre el cliente que recibe el push' })

/** Un dispositivo push de la persona (CU36). El token de Firebase nunca sale del servicio. */
@ObjectType('Dispositivo')
export class DispositivoType implements DispositivoResponse {
  @Field(() => ID)
  codigoDispositivoPush!: string

  @Field(() => Plataforma)
  plataforma!: Plataforma

  @Field()
  activo!: boolean
}
