<?php

namespace App\Common\Exceptions;

/**
 * 423: Recurso bloqueado temporalmente por una política de seguridad.
 */
class LockedException extends BusinessException
{
    public function __construct(string $errorCode, string $message)
    {
        parent::__construct($errorCode, 423, $message);
    }
}
