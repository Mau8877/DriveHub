<?php

namespace App\Common\Models;

use App\Common\Support\Secuencia;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;
use MongoDB\Laravel\Eloquent\Model;

/**
 * Base de todos los Models (documentos de MongoDB) del servicio.
 *
 * * `id` (el _id del documento) es un entero secuencial, no un ObjectId:
 * * mismo criterio que la PK interna Long del resto de los servicios. Se
 * * asigna al crear, desde la colección `contadores` (ver Secuencia). Nunca
 * * sale del servicio: está en $hidden.
 * * El identificador público es el campo codigo_{coleccion} (UUID v7) que
 * * declara cada Model en campoCodigo(); se genera al crear si viene vacío.
 * * Soft delete con deleted_at: Eloquent filtra los borrados en toda query.
 */
abstract class BaseModel extends Model
{
    use SoftDeletes;

    protected $connection = 'mongodb';

    protected $keyType = 'int';

    public $incrementing = false;

    protected $hidden = ['id', '_id'];

    /**
     * Nombre del campo con el codigo público (ej. "codigo_manual").
     */
    abstract public function campoCodigo(): string;

    protected static function booted(): void
    {
        static::creating(function (BaseModel $modelo) {
            if ($modelo->getKey() === null) {
                $modelo->setAttribute($modelo->getKeyName(), Secuencia::siguiente($modelo->getTable()));
            }

            $campo = $modelo->campoCodigo();
            if (empty($modelo->getAttribute($campo))) {
                $modelo->setAttribute($campo, (string) Str::uuid7());
            }
        });
    }
}
