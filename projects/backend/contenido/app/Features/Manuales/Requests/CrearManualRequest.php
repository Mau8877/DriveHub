<?php

namespace App\Features\Manuales\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * Cuerpo de POST /api/v1/manuales (CU32). La escuela NO viaja en el cuerpo: sale del token.
 * `archivo` es el descriptor que devolvió la subida (POST /manuales/archivos).
 */
class CrearManualRequest extends FormRequest
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
            'tema' => ['nullable', 'string', 'max:80'],
            'etiquetas' => ['nullable', 'array', 'max:10'],
            'etiquetas.*' => ['string', 'max:30'],
            'archivo' => ['required', 'array'],
            'archivo.url' => ['required', 'string', 'max:500'],
            'archivo.mime' => ['required', 'string', 'max:100'],
            'archivo.tamanoBytes' => ['required', 'integer', 'min:1'],
            'archivo.sha256' => ['required', 'string', 'regex:/^[a-f0-9]{64}$/'],
            'archivo.nombre' => ['nullable', 'string', 'max:200'],
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
            'tema.max' => 'El tema no puede superar 80 caracteres',
            'etiquetas.max' => 'Puedes poner hasta 10 etiquetas',
            'etiquetas.*.max' => 'Cada etiqueta puede tener hasta 30 caracteres',
            'archivo.required' => 'Sube el PDF del manual',
            'archivo.*' => 'El archivo del manual no es válido',
        ];
    }
}
