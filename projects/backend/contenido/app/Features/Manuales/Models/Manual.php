<?php

namespace App\Features\Manuales\Models;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Models\BaseModel;

/**
 * Manual de la escuela (CU32, CU34). Colección `manuales`.
 *
 * * Solo metadatos + ubicación del archivo: el PDF vive en el bucket
 * * drivehub-contenido. `version` sube cada vez que se reemplaza el archivo.
 *
 * @property string $codigo_manual Identificador público (UUID v7)
 * @property string $titulo
 * @property string|null $descripcion
 * @property string $categoria Sigla de categoría (Escuela categoria_licencia) o GENERAL
 * @property string|null $tema
 * @property int $version
 * @property array{url:string,mime:string,tamano_bytes:int,sha256:string} $archivo
 * @property EstadoPublicacion $estado
 * @property array<int,string> $etiquetas
 * @property string|null $publicado_por_codigo Persona de Identidad (usuario.codigo_usuario)
 */
class Manual extends BaseModel
{
    protected $table = 'manuales';

    protected $fillable = [
        'titulo',
        'descripcion',
        'categoria',
        'tema',
        'version',
        'archivo',
        'estado',
        'etiquetas',
        'publicado_por_codigo',
    ];

    // * Sin cast para arrays/subdocumentos (archivo, recurso, etiquetas...):
    // * MongoDB los guarda nativos. El cast 'array' de Eloquent los
    // * serializaría como string JSON.
    protected function casts(): array
    {
        return [
            'version' => 'integer',
            'estado' => EstadoPublicacion::class,
        ];
    }

    public function campoCodigo(): string
    {
        return 'codigo_manual';
    }
}
