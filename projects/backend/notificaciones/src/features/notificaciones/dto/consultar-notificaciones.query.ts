import { Transform, Type } from 'class-transformer'
import { IsBoolean, IsEnum, IsInt, IsOptional, Max, Min, ValidateNested } from 'class-validator'

import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'

class FiltrosNotificacionesQuery {
  // El query llega como texto: solo 'true' y 'false' son válidos (cualquier otra cosa falla la validación)
  @IsOptional()
  @Transform(({ value }) => (value === 'true' ? true : value === 'false' ? false : value))
  @IsBoolean({ message: 'El filtro de leída debe ser true o false' })
  leida?: boolean

  @IsOptional()
  @IsEnum(TipoNotificacion, { message: 'El tipo de aviso no es válido' })
  tipo?: TipoNotificacion
}

/** Query del listado de la bandeja (CU38): `page`, `pageSize` y `filter[leida]` / `filter[tipo]`. */
export class ConsultarNotificacionesQuery {
  @IsOptional()
  @Type(() => Number)
  @IsInt({ message: 'La página debe ser un número entero' })
  @Min(1, { message: 'La página debe ser 1 o mayor' })
  page: number = 1

  @IsOptional()
  @Type(() => Number)
  @IsInt({ message: 'El tamaño de página debe ser un número entero' })
  @Min(1, { message: 'El tamaño de página debe ser de 1 a 100' })
  @Max(100, { message: 'El tamaño de página debe ser de 1 a 100' })
  pageSize: number = 20

  @IsOptional()
  @ValidateNested()
  @Type(() => FiltrosNotificacionesQuery)
  filter?: FiltrosNotificacionesQuery
}
