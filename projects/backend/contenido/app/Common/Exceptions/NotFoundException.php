<?php

namespace App\Common\Exceptions;

/**
 * 404: El recurso solicitado no existe.
 */
class NotFoundException extends BusinessException
{
    public function __construct(string $errorCode, string $message)
    {
        parent::__construct($errorCode, 404, $message);
    }
}
