<?php

namespace App\Features\Tutoriales\Requests;

use App\Common\Enums\EstadoPublicacion;
use App\Features\Tutoriales\Enums\TipoRecurso;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Query de los listados de tutoriales (CU33 y CU35): `page`, `pageSize`, `search`,
 * `filter[estado]`, `filter[tipo]` y `filter[categoria]`, el contrato de ENDPOINTS_BACKEND.md.
 */
class ListarTutorialesRequest extends FormRequest
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
            'filter.tipo' => ['nullable', Rule::enum(TipoRecurso::class)],
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
            'filter.tipo' => 'El tipo debe ser PDF, ENLACE o VIDEO',
        ];
    }
}
