import { ArgsType, Field, Int } from '@nestjs/graphql'
import { IsBoolean, IsEnum, IsInt, IsOptional, Max, Min } from 'class-validator'

import { TipoNotificacion } from '../enums/tipo-notificacion.enum.js'

/**
 * Argumentos de la consulta `notificaciones` (CU38): la misma paginación y los mismos filtros que el listado de REST
 * (`page`, `pageSize`, `filter[leida]`, `filter[tipo]`), con las mismas reglas y mensajes.
 */
@ArgsType()
export class ConsultarNotificacionesArgs {
  @Field(() => Int, { defaultValue: 1 })
  @IsInt({ message: 'La página debe ser un número entero' })
  @Min(1, { message: 'La página debe ser 1 o mayor' })
  page: number = 1

  @Field(() => Int, { defaultValue: 20 })
  @IsInt({ message: 'El tamaño de página debe ser un número entero' })
  @Min(1, { message: 'El tamaño de página debe ser de 1 a 100' })
  @Max(100, { message: 'El tamaño de página debe ser de 1 a 100' })
  pageSize: number = 20

  @Field(() => Boolean, { nullable: true })
  @IsOptional()
  @IsBoolean({ message: 'El filtro de leída debe ser true o false' })
  leida?: boolean | null

  @Field(() => TipoNotificacion, { nullable: true })
  @IsOptional()
  @IsEnum(TipoNotificacion, { message: 'El tipo de aviso no es válido' })
  tipo?: TipoNotificacion | null
}
