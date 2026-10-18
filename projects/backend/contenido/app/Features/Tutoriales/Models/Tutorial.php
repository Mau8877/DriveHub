<?php

namespace App\Features\Tutoriales\Models;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Models\BaseModel;
use App\Features\Tutoriales\Enums\TipoRecurso;

/**
 * Tutorial de un instructor (CU33, CU35). Colección `tutoriales`.
 *
 * * Solo metadatos + ubicación del recurso: los videos/PDF viven en el
 * * bucket drivehub-contenido (o es un enlace externo si tipo = ENLACE).
 *
 * @property string $codigo_tutorial Identificador público (UUID v7)
 * @property string $titulo
 * @property string|null $descripcion
 * @property string $autor_codigo Instructor: persona de Identidad (usuario.codigo_usuario)
 * @property string $categoria Sigla de categoría (Escuela categoria_licencia)
 * @property TipoRecurso $tipo
 * @property array{url:string,duracion_seg:int|null} $recurso
 * @property array<int,string> $audiencia Valores de Audiencia (ESTUDIANTE y/o INSTRUCTOR)
 * @property EstadoPublicacion $estado
 * @property array<int,string> $etiquetas
 */
class Tutorial extends BaseModel
{
    protected $table = 'tutoriales';

    protected $fillable = [
        'titulo',
        'descripcion',
        'autor_codigo',
        'categoria',
        'tipo',
        'recurso',
        'audiencia',
        'estado',
        'etiquetas',
    ];

    // * Sin cast para arrays/subdocumentos (archivo, recurso, etiquetas...):
    // * MongoDB los guarda nativos. El cast 'array' de Eloquent los
    // * serializaría como string JSON.
    protected function casts(): array
    {
        return [
            'tipo' => TipoRecurso::class,
            'estado' => EstadoPublicacion::class,
        ];
    }

    public function campoCodigo(): string
    {
        return 'codigo_tutorial';
    }
}
