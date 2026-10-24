<?php

namespace App\Features\Manuales\Resources;

use App\Features\Manuales\Models\Manual;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * Un manual tal como lo ve el Administrador (CU32): también cada elemento del listado.
 * El JSON va en camelCase, igual que el resto de los servicios; el id interno nunca sale.
 *
 * @mixin Manual
 */
class ManualResource extends JsonResource
{
    /**
     * @return array<string,mixed>
     */
    public function toArray(Request $request): array
    {
        $archivo = $this->archivo ?? [];

        return [
            'codigo' => $this->codigo_manual,
            'titulo' => $this->titulo,
            'descripcion' => $this->descripcion,
            'categoria' => $this->categoria,
            'tema' => $this->tema,
            'version' => $this->version,
            'estado' => $this->estado->value,
            'etiquetas' => array_values($this->etiquetas ?? []),
            'archivo' => [
                'url' => $archivo['url'] ?? null,
                'nombre' => $archivo['nombre'] ?? null,
                'mime' => $archivo['mime'] ?? null,
                'tamanoBytes' => $archivo['tamano_bytes'] ?? null,
            ],
            'creadoEn' => $this->created_at?->toIso8601ZuluString(),
            'actualizadoEn' => $this->updated_at?->toIso8601ZuluString(),
        ];
    }
}
