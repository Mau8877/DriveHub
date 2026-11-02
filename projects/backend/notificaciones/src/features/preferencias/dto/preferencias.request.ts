import { Type } from 'class-transformer'
import { IsBoolean, IsDefined, IsEmail, IsOptional, Matches, ValidateNested } from 'class-validator'

/** Hora del día, de 00:00 a 23:59. */
const HORA = /^([01]\d|2[0-3]):[0-5]\d$/
const MENSAJE_HORA = 'La hora debe tener el formato HH:mm, de 00:00 a 23:59'

class CanalesRequest {
  @IsBoolean({ message: 'El canal de correo debe ser verdadero o falso' })
  email!: boolean

  @IsBoolean({ message: 'El canal push debe ser verdadero o falso' })
  push!: boolean

  @IsBoolean({ message: 'El canal dentro de la app debe ser verdadero o falso' })
  inApp!: boolean
}

class TiposRequest {
  @IsBoolean({ message: 'El tipo RESERVA debe ser verdadero o falso' })
  RESERVA!: boolean

  @IsBoolean({ message: 'El tipo PAGO debe ser verdadero o falso' })
  PAGO!: boolean

  @IsBoolean({ message: 'El tipo EXAMEN debe ser verdadero o falso' })
  EXAMEN!: boolean

  @IsBoolean({ message: 'El tipo RECORDATORIO debe ser verdadero o falso' })
  RECORDATORIO!: boolean
}

/**
 * Configuración completa de la persona (CU37, `PUT`): lo que se omite (franja de silencio,
 * correo) se interpreta como «vaciar», no como «no tocar».
 */
export class PreferenciasRequest {
  @IsDefined({ message: 'Los canales son obligatorios' })
  @ValidateNested()
  @Type(() => CanalesRequest)
  canales!: CanalesRequest

  @IsDefined({ message: 'Los tipos de aviso son obligatorios' })
  @ValidateNested()
  @Type(() => TiposRequest)
  tipos!: TiposRequest

  @IsOptional()
  @Matches(HORA, { message: MENSAJE_HORA })
  silencioDesde?: string | null

  @IsOptional()
  @Matches(HORA, { message: MENSAJE_HORA })
  silencioHasta?: string | null

  @IsOptional()
  @IsEmail({}, { message: 'El correo no es válido' })
  email?: string | null
}
