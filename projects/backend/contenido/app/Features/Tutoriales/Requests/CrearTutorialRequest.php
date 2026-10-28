<?php

namespace App\Features\Tutoriales\Requests;

use App\Features\Tutoriales\Enums\Audiencia;
use App\Features\Tutoriales\Enums\TipoRecurso;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

/**
 * Cuerpo de POST /api/v1/tutoriales (CU33). La escuela y el autor NO viajan en el cuerpo: salen del
 * token. `recurso` es el descriptor que devolvió la subida del PDF, o `{ url }` si es un enlace.
 * Que el recurso corresponda al tipo lo comprueba el service (ERR_CON_07).
 */
class CrearTutorialRequest extends FormRequest
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
            'titulo' => ['required', 'string', 'min:3', 'max:150'],
            'descripcion' => ['nullable', 'string', 'max:1000'],
            'categoria' => ['nullable', 'string', 'regex:/^(GENERAL|[A-Z]{1,3})$/'],
            'etiquetas' => ['nullable', 'array', 'max:10'],
            'etiquetas.*' => ['string', 'max:30'],
            'audiencia' => ['required', 'array', 'min:1'],
            'audiencia.*' => ['distinct', Rule::enum(Audiencia::class)],
            'tipo' => ['required', Rule::in([TipoRecurso::PDF->value, TipoRecurso::ENLACE->value])],
            'recurso' => ['required', 'array'],
            'recurso.url' => ['required', 'string', 'max:500'],
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
            'titulo.required' => 'El título es obligatorio',
            'titulo.min' => 'El título debe tener entre 3 y 150 caracteres',
            'titulo.max' => 'El título debe tener entre 3 y 150 caracteres',
            'descripcion.max' => 'La descripción no puede superar 1000 caracteres',
            'categoria.regex' => 'La categoría debe ser GENERAL o una sigla de hasta 3 letras mayúsculas',
            'etiquetas.max' => 'Puedes poner hasta 10 etiquetas',
            'etiquetas.*.max' => 'Cada etiqueta puede tener hasta 30 caracteres',
            'audiencia.required' => 'Elige a quién va dirigido el tutorial',
            'audiencia.min' => 'Elige a quién va dirigido el tutorial',
            'audiencia.*' => 'La audiencia debe ser ESTUDIANTE o INSTRUCTOR',
            'tipo.*' => 'El tipo debe ser PDF o ENLACE',
            'recurso.*' => 'El recurso del tutorial no es válido',
        ];
    }
}
