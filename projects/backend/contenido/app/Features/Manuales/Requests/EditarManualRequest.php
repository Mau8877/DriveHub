<?php

namespace App\Features\Manuales\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * Cuerpo de PATCH /api/v1/manuales/{codigo} (CU32). Todo es opcional: lo que no se envía no se
 * toca. Un `archivo` distinto del actual reemplaza el PDF y sube la versión.
 */
class EditarManualRequest extends FormRequest
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
            'tema' => ['sometimes', 'nullable', 'string', 'max:80'],
            'etiquetas' => ['sometimes', 'nullable', 'array', 'max:10'],
            'etiquetas.*' => ['string', 'max:30'],
            'archivo' => ['sometimes', 'array'],
            'archivo.url' => ['required_with:archivo', 'string', 'max:500'],
            'archivo.mime' => ['required_with:archivo', 'string', 'max:100'],
            'archivo.tamanoBytes' => ['required_with:archivo', 'integer', 'min:1'],
            'archivo.sha256' => ['required_with:archivo', 'string', 'regex:/^[a-f0-9]{64}$/'],
            'archivo.nombre' => ['nullable', 'string', 'max:200'],
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
            'tema.max' => 'El tema no puede superar 80 caracteres',
            'etiquetas.max' => 'Puedes poner hasta 10 etiquetas',
            'etiquetas.*.max' => 'Cada etiqueta puede tener hasta 30 caracteres',
            'archivo.*' => 'El archivo del manual no es válido',
        ];
    }
}
