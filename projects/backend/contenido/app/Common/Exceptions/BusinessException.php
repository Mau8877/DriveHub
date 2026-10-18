<?php

namespace App\Common\Exceptions;

use RuntimeException;

/**
 * Base de todas las excepciones de negocio del servicio: siempre llevan un
 * código catalogado (ERR_CON_NN) y el HTTP que les corresponde. Misma
 * jerarquía que los servicios Spring (ver EXCEPCIONES_BACKEND.md).
 *
 * ! No se lanzan RuntimeException genéricas ni abort() desde código de
 * ! negocio: el handler de bootstrap/app.php solo arma el sobre con código
 * ! para estas.
 */
abstract class BusinessException extends RuntimeException
{
    public function __construct(
        public readonly string $errorCode,
        public readonly int $httpStatus,
        string $message,
    ) {
        parent::__construct($message);
    }
}
