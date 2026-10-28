<?php

namespace App\Features\Tutoriales\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * Cuerpo (multipart) de POST /api/v1/tutoriales/archivos (CU33): el PDF del tutorial, de hasta 20 MB.
 */
class SubirArchivoTutorialRequest extends FormRequest
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
            'archivo' => ['required', 'file', 'mimetypes:application/pdf', 'max:20480'],
        ];
    }

    /**
     * @return array<string,string>
     */
    public function messages(): array
    {
        return [
            'archivo.required' => 'Elige el PDF del tutorial',
            'archivo.file' => 'El archivo no se pudo leer',
            'archivo.mimetypes' => 'El tutorial debe ser un archivo PDF',
            'archivo.max' => 'El PDF no puede pesar más de 20 MB',
            'archivo.uploaded' => 'No se pudo subir el archivo: revisa que no pese más de 20 MB',
        ];
    }
}
