<?php

namespace App\Features\Manuales\Requests;

use App\Common\Enums\EstadoPublicacion;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Query de GET /api/v1/manuales (CU32): `page`, `pageSize`, `search`, `filter[estado]` y
 * `filter[categoria]`, el contrato de listados de ENDPOINTS_BACKEND.md.
 */
class ListarManualesRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    /**
     * @return array<string,mixed>
     */
    public function rules(): array
    {
        return [
            'page' => ['nullable', 'integer', 'min:1'],
            'pageSize' => ['nullable', 'integer', 'min:1', 'max:100'],
            'search' => ['nullable', 'string', 'max:100'],
            'filter' => ['nullable', 'array'],
            'filter.estado' => ['nullable', Rule::enum(EstadoPublicacion::class)],
            'filter.categoria' => ['nullable', 'string', 'max:10'],
        ];
    }

    /**
     * @return array<string,string>
     */
    public function messages(): array
    {
        return [
            'page.*' => 'La página debe ser un número desde 1',
            'pageSize.*' => 'El tamaño de página debe estar entre 1 y 100',
            'filter.estado' => 'El estado debe ser BORRADOR, PUBLICADO o ARCHIVADO',
        ];
    }
}
