<?php

namespace App\Common\Exceptions;

/**
 * 401: No autenticado: falta el token, es inválido o expiró.
 */
class UnauthorizedException extends BusinessException
{
    public function __construct(string $errorCode, string $message)
    {
        parent::__construct($errorCode, 401, $message);
    }
}
