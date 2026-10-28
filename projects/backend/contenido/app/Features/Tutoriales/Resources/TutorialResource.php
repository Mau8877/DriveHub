<?php

namespace App\Features\Tutoriales\Resources;

use App\Features\Tutoriales\Models\Tutorial;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * Un tutorial tal como lo ven el instructor (CU33) y quien lo consulta (CU35). El JSON va en
 * camelCase, igual que el resto de los servicios; el id interno nunca sale.
 *
 * @mixin Tutorial
 */
class TutorialResource extends JsonResource
{
    /**
     * @return array<string,mixed>
     */
    public function toArray(Request $request): array
    {
        $recurso = $this->recurso ?? [];

        return [
            'codigo' => $this->codigo_tutorial,
            'titulo' => $this->titulo,
            'descripcion' => $this->descripcion,
            'categoria' => $this->categoria,
            'tipo' => $this->tipo->value,
            'estado' => $this->estado->value,
            'audiencia' => array_values($this->audiencia ?? []),
            'etiquetas' => array_values($this->etiquetas ?? []),
            'recurso' => [
                'url' => $recurso['url'] ?? null,
                'nombre' => $recurso['nombre'] ?? null,
                'mime' => $recurso['mime'] ?? null,
                'tamanoBytes' => $recurso['tamano_bytes'] ?? null,
            ],
            'creadoEn' => $this->created_at?->toIso8601ZuluString(),
            'actualizadoEn' => $this->updated_at?->toIso8601ZuluString(),
        ];
    }
}
