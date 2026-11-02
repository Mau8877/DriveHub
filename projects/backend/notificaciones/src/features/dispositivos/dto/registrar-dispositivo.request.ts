import { IsEnum, IsNotEmpty, IsString, MaxLength } from 'class-validator'

import { Plataforma } from '../enums/plataforma.enum.js'

/** Lo que manda la app al registrar el token de Firebase de su dispositivo (CU36). */
export class RegistrarDispositivoRequest {
  @IsString({ message: 'El token del dispositivo es obligatorio' })
  @IsNotEmpty({ message: 'El token del dispositivo es obligatorio' })
  @MaxLength(4096, { message: 'El token del dispositivo es demasiado largo' })
  tokenFcm!: string

  @IsEnum(Plataforma, { message: 'La plataforma debe ser ANDROID, IOS o WEB' })
  plataforma!: Plataforma
}
