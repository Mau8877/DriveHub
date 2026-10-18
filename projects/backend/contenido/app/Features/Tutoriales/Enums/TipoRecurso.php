<?php

namespace App\Features\Tutoriales\Enums;

/**
 * Tipo de recurso de un tutorial (CU33).
 */
enum TipoRecurso: string
{
    case VIDEO = 'VIDEO';
    case PDF = 'PDF';
    case ENLACE = 'ENLACE';
}
