<?php

namespace App\Common\Exceptions;

/**
 * 400: El payload no cumple una regla de negocio (más allá de la validación de formato).
 */
class ValidationException extends BusinessException
{
    public function __construct(string $errorCode, string $message)
    {
        parent::__construct($errorCode, 400, $message);
    }
}
