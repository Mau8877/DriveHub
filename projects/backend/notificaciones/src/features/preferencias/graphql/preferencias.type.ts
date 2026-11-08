import { Field, ObjectType } from '@nestjs/graphql'

import type { PreferenciasResponse } from '../dto/preferencias.response.js'

@ObjectType('CanalesAviso')
export class CanalesAvisoType {
  @Field()
  email!: boolean

  @Field()
  push!: boolean

  @Field()
  inApp!: boolean
}

/** Un interruptor por tipo de aviso. Los nombres son los mismos que usa REST. */
@ObjectType('TiposAviso')
export class TiposAvisoType {
  @Field()
  RESERVA!: boolean

  @Field()
  PAGO!: boolean

  @Field()
  EXAMEN!: boolean

  @Field()
  RECORDATORIO!: boolean
}

/** La configuración de avisos de la persona (CU37). */
@ObjectType('Preferencias')
export class PreferenciasType implements PreferenciasResponse {
  @Field(() => CanalesAvisoType)
  canales!: CanalesAvisoType

  @Field(() => TiposAvisoType)
  tipos!: TiposAvisoType

  @Field(() => String, { nullable: true })
  silencioDesde!: string | null

  @Field(() => String, { nullable: true })
  silencioHasta!: string | null

  @Field(() => String, { nullable: true })
  email!: string | null

  /** `null` si la persona nunca guardó: se muestran los valores por defecto. */
  @Field(() => String, { nullable: true })
  actualizadoEn!: string | null
}
