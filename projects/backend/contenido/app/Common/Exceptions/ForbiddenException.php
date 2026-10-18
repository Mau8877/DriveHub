<?php

namespace App\Common\Exceptions;

/**
 * 403: Autenticado pero sin permiso sobre el recurso o la acción.
 */
class ForbiddenException extends BusinessException
{
    public function __construct(string $errorCode, string $message)
    {
        parent::__construct($errorCode, 403, $message);
    }
}
