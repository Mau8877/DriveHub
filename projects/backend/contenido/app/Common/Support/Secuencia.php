<?php

namespace App\Common\Support;

use Illuminate\Support\Facades\DB;
use MongoDB\Operation\FindOneAndUpdate;

/**
 * Secuencias enteras por colección, para el _id de los documentos.
 *
 * * MongoDB no tiene autoincremental. Se usa una colección `contadores` con
 * * un documento por colección ({ _id: "manuales", secuencia: 42 }) y un
 * * findOneAndUpdate atómico con $inc: dos inserts concurrentes nunca
 * * reciben el mismo número. Mismo mecanismo que SecuenciaService en Académico.
 */
final class Secuencia
{
    public const COLECCION_CONTADORES = 'contadores';

    public static function siguiente(string $coleccion): int
    {
        $contador = DB::connection('mongodb')
            ->getDatabase()
            ->selectCollection(self::COLECCION_CONTADORES)
            ->findOneAndUpdate(
                ['_id' => $coleccion],
                ['$inc' => ['secuencia' => 1]],
                ['upsert' => true, 'returnDocument' => FindOneAndUpdate::RETURN_DOCUMENT_AFTER],
            );

        return (int) $contador['secuencia'];
    }
}
