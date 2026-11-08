import { Field, InputType } from '@nestjs/graphql'
import { Type } from 'class-transformer'
import { IsBoolean, IsDefined, IsEmail, IsOptional, Matches, ValidateNested } from 'class-validator'

/** Hora del día, de 00:00 a 23:59. Las mismas reglas y mensajes que `PreferenciasRequest` de REST. */
const HORA = /^([01]\d|2[0-3]):[0-5]\d$/
const MENSAJE_HORA = 'La hora debe tener el formato HH:mm, de 00:00 a 23:59'

@InputType('CanalesAvisoInput')
export class CanalesAvisoInput {
  @Field()
  @IsBoolean({ message: 'El canal de correo debe ser verdadero o falso' })
  email!: boolean

  @Field()
  @IsBoolean({ message: 'El canal push debe ser verdadero o falso' })
  push!: boolean

  @Field()
  @IsBoolean({ message: 'El canal dentro de la app debe ser verdadero o falso' })
  inApp!: boolean
}

@InputType('TiposAvisoInput')
export class TiposAvisoInput {
  @Field()
  @IsBoolean({ message: 'El tipo RESERVA debe ser verdadero o falso' })
  RESERVA!: boolean

  @Field()
  @IsBoolean({ message: 'El tipo PAGO debe ser verdadero o falso' })
  PAGO!: boolean

  @Field()
  @IsBoolean({ message: 'El tipo EXAMEN debe ser verdadero o falso' })
  EXAMEN!: boolean

  @Field()
  @IsBoolean({ message: 'El tipo RECORDATORIO debe ser verdadero o falso' })
  RECORDATORIO!: boolean
}

/**
 * Configuración completa de la persona (CU37): como en el `PUT` de REST, lo que se omite (franja de silencio, correo)
 * se interpreta como «vaciar», no como «no tocar». Por eso la mutación se llama `guardarPreferencias` y no
 * `actualizar...`.
 *
 * ! Las reglas de validación de un campo deben ser iguales a las de `PreferenciasRequest` (las comprueba un test).
 */
@InputType('PreferenciasInput')
export class PreferenciasInput {
  @Field(() => CanalesAvisoInput)
  @IsDefined({ message: 'Los canales son obligatorios' })
  @ValidateNested()
  @Type(() => CanalesAvisoInput)
  canales!: CanalesAvisoInput

  @Field(() => TiposAvisoInput)
  @IsDefined({ message: 'Los tipos de aviso son obligatorios' })
  @ValidateNested()
  @Type(() => TiposAvisoInput)
  tipos!: TiposAvisoInput

  @Field(() => String, { nullable: true })
  @IsOptional()
  @Matches(HORA, { message: MENSAJE_HORA })
  silencioDesde?: string | null

  @Field(() => String, { nullable: true })
  @IsOptional()
  @Matches(HORA, { message: MENSAJE_HORA })
  silencioHasta?: string | null

  @Field(() => String, { nullable: true })
  @IsOptional()
  @IsEmail({}, { message: 'El correo no es válido' })
  email?: string | null
}
