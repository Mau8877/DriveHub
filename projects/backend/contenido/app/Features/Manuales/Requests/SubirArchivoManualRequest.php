<?php

namespace App\Features\Manuales\Requests;

use Illuminate\Foundation\Http\FormRequest;

/**
 * Cuerpo (multipart) de POST /api/v1/manuales/archivos (CU32): el PDF del manual, de hasta 20 MB.
 */
class SubirArchivoManualRequest extends FormRequest
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
            'archivo.required' => 'Elige el PDF del manual',
            'archivo.file' => 'El archivo no se pudo leer',
            'archivo.mimetypes' => 'El manual debe ser un archivo PDF',
            'archivo.max' => 'El PDF no puede pesar más de 20 MB',
            'archivo.uploaded' => 'No se pudo subir el archivo: revisa que no pese más de 20 MB',
        ];
    }
}
