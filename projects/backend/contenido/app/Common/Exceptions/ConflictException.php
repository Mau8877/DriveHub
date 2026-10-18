<?php

namespace App\Common\Exceptions;

/**
 * 409: Conflicto de estado (recurso duplicado, transición inválida).
 */
class ConflictException extends BusinessException
{
    public function __construct(string $errorCode, string $message)
    {
        parent::__construct($errorCode, 409, $message);
    }
}
