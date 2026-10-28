<?php

namespace App\Common\Exceptions;

/**
 * Catálogo de códigos de error del servicio Contenido (prefijo CON).
 *
 * * Cada código nuevo se documenta en docs/errors/contenido/ERRORES_CONTENIDO.md
 * * en el mismo cambio (ver GUIA_ERRORES_BACKEND.md).
 */
final class ErrorCodes
{
    public const MANUAL_NO_ENCONTRADO = 'ERR_CON_01';

    public const TRANSICION_MANUAL_INVALIDA = 'ERR_CON_02';

    public const ARCHIVO_INVALIDO = 'ERR_CON_03';

    public const TOKEN_SIN_ESCUELA = 'ERR_CON_04';

    public const TUTORIAL_NO_ENCONTRADO = 'ERR_CON_05';

    public const TRANSICION_TUTORIAL_INVALIDA = 'ERR_CON_06';

    public const RECURSO_TUTORIAL_INVALIDO = 'ERR_CON_07';

    // * Códigos transversales del sistema (ERR_SYS), iguales en todos los servicios
    public const VALIDACION = 'ERR_SYS_01';

    public const NO_AUTENTICADO = 'ERR_SYS_04';

    public const SIN_PERMISO = 'ERR_SYS_05';
}
