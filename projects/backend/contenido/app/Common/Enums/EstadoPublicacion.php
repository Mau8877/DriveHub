<?php

namespace App\Common\Enums;

/**
 * Estado de publicación de un manual o tutorial (CU32, CU33). Lo comparten
 * los dos features, por eso vive en Common.
 */
enum EstadoPublicacion: string
{
    case BORRADOR = 'BORRADOR';
    case PUBLICADO = 'PUBLICADO';
    case ARCHIVADO = 'ARCHIVADO';
}
