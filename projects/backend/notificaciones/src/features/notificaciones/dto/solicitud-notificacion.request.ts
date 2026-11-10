import {
  IsEmail,
  IsEnum,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  MaxLength,
  Validate,
  ValidateIf,
  type ValidationArguments,
  ValidatorConstraint,
  type ValidatorConstraintInterface,
} from 'class-validator'

import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'

/** La escuela no puede venir junto con una persona: el aviso va a una bandeja o a la otra. */
@ValidatorConstraint({ name: 'sinPersona' })
class SinPersona implements ValidatorConstraintInterface {
  validate(_valor: unknown, args: ValidationArguments): boolean {
    return (args.object as SolicitudNotificacionRequest).usuarioCodigo === undefined
  }

  defaultMessage(): string {
    return 'El aviso va a una persona o a una escuela, no a las dos'
  }
}

/**
 * Pedido de notificación (CU36): lo mismo viaja por la ruta interna y por el
 * mensaje `notificacion.solicitada` de RabbitMQ.
 *
 * * Quien lo pide redacta el título y el mensaje, en español.
 * * eventoCodigo identifica el pedido: el mismo código se procesa una sola vez.
 * * El destino es una persona (`usuarioCodigo`) o, desde CU53, la bandeja de una escuela (`escuelaCodigo`),
 * * que leen sus Administradores. Exactamente uno de los dos.
 */
export class SolicitudNotificacionRequest {
  @IsUUID(undefined, { message: 'El código del pedido no es válido' })
  eventoCodigo!: string

  @ValidateIf((o: SolicitudNotificacionRequest) => o.escuelaCodigo === undefined)
  @IsUUID(undefined, { message: 'El código de la persona no es válido' })
  usuarioCodigo?: string

  @IsOptional()
  @IsUUID(undefined, { message: 'El código de la escuela no es válido' })
  @Validate(SinPersona)
  escuelaCodigo?: string

  @IsEnum(TipoNotificacion, { message: 'El tipo de notificación no es válido' })
  tipo!: TipoNotificacion

  @IsString({ message: 'El título es obligatorio' })
  @MaxLength(120, { message: 'El título no puede pasar de 120 caracteres' })
  titulo!: string

  @IsString({ message: 'El mensaje es obligatorio' })
  @MaxLength(1000, { message: 'El mensaje no puede pasar de 1000 caracteres' })
  mensaje!: string

  @IsOptional()
  @IsObject({ message: 'Los datos deben ser un objeto' })
  datos?: Record<string, unknown>

  @IsOptional()
  @IsEmail({}, { message: 'El correo no es válido' })
  email?: string
}
