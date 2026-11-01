<?php

namespace App\Features\Tutoriales\Requests;

use App\Features\Tutoriales\Enums\Audiencia;
use App\Features\Tutoriales\Enums\TipoRecurso;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Cuerpo de PATCH /api/v1/tutoriales/{codigo} (CU33): solo los campos que cambian. El tipo y el
 * recurso viajan juntos: uno sin el otro no tiene sentido.
 */
class EditarTutorialRequest extends FormRequest
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
            'titulo' => ['sometimes', 'string', 'min:3', 'max:150'],
            'descripcion' => ['sometimes', 'nullable', 'string', 'max:1000'],
            'categoria' => ['sometimes', 'string', 'regex:/^(GENERAL|[A-Z]{1,3})$/'],
            'etiquetas' => ['sometimes', 'nullable', 'array', 'max:10'],
            'etiquetas.*' => ['string', 'max:30'],
            'audiencia' => ['sometimes', 'array', 'min:1'],
            'audiencia.*' => ['distinct', Rule::enum(Audiencia::class)],
            'tipo' => ['required_with:recurso', Rule::in([TipoRecurso::PDF->value, TipoRecurso::ENLACE->value])],
            'recurso' => ['required_with:tipo', 'array'],
            'recurso.url' => ['required_with:recurso', 'string', 'max:500'],
            'recurso.mime' => ['nullable', 'string', 'max:100'],
            'recurso.tamanoBytes' => ['nullable', 'integer', 'min:1'],
            'recurso.sha256' => ['nullable', 'string', 'regex:/^[a-f0-9]{64}$/'],
            'recurso.nombre' => ['nullable', 'string', 'max:200'],
        ];
    }

    /**
     * @return array<string,string>
     */
    public function messages(): array
    {
        return [
            'titulo.min' => 'El título debe tener entre 3 y 150 caracteres',
            'titulo.max' => 'El título debe tener entre 3 y 150 caracteres',
            'descripcion.max' => 'La descripción no puede superar 1000 caracteres',
            'categoria.regex' => 'La categoría debe ser GENERAL o una sigla de hasta 3 letras mayúsculas',
            'etiquetas.max' => 'Puedes poner hasta 10 etiquetas',
            'etiquetas.*.max' => 'Cada etiqueta puede tener hasta 30 caracteres',
            'audiencia.*' => 'La audiencia debe ser ESTUDIANTE o INSTRUCTOR',
            'tipo.*' => 'El tipo debe ser PDF o ENLACE',
            'recurso.*' => 'El recurso del tutorial no es válido',
        ];
    }
}
