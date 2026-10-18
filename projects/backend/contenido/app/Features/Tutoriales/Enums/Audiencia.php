<?php

namespace App\Features\Tutoriales\Enums;

/**
 * A quién va dirigido un tutorial (CU33, CU35). Un tutorial puede tener ambas.
 */
enum Audiencia: string
{
    case ESTUDIANTE = 'ESTUDIANTE';
    case INSTRUCTOR = 'INSTRUCTOR';
}
