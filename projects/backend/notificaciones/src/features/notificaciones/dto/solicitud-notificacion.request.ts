import { IsEmail, IsEnum, IsObject, IsOptional, IsString, IsUUID, MaxLength } from 'class-validator'

import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'

/**
 * Pedido de notificación (CU36): lo mismo viaja por la ruta interna y por el
 * mensaje `notificacion.solicitada` de RabbitMQ.
 *
 * * Quien lo pide redacta el título y el mensaje, en español.
 * * eventoCodigo identifica el pedido: el mismo código se procesa una sola vez.
 */
export class SolicitudNotificacionRequest {
  @IsUUID(undefined, { message: 'El código del pedido no es válido' })
  eventoCodigo!: string

  @IsUUID(undefined, { message: 'El código de la persona no es válido' })
  usuarioCodigo!: string

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
