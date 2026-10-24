<?php

namespace App\Features\Manuales\Services;

use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\ValidationException;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

/**
 * Guarda el PDF de un manual en el bucket `drivehub-contenido` de MinIO y arma el descriptor que
 * después se guarda en el manual. El archivo se sube primero y el manual lo referencia: si la
 * persona abandona el formulario queda un archivo sin manual (se limpia aparte, ver el CU).
 *
 * * Cada escuela sube a su propia carpeta (`escuelas/{escuela}/manuales/`): un manual solo puede
 * * apuntar a archivos de la carpeta de su escuela.
 */
class ArchivoManualService
{
    public const MIME_PDF = 'application/pdf';

    /**
     * @return array{url:string,mime:string,tamano_bytes:int,sha256:string,nombre:string}
     */
    public function subir(UploadedFile $archivo, string $escuelaCodigo): array
    {
        $clave = $this->carpeta($escuelaCodigo).Str::uuid7().'.pdf';
        $disco = Storage::disk('contenido');
        $disco->put($clave, file_get_contents($archivo->getRealPath()), ['ContentType' => self::MIME_PDF]);

        Log::info('Archivo de manual subido', ['module' => 'manuales', 'escuelaCodigo' => $escuelaCodigo]);

        return [
            'url' => $this->urlPublica($clave),
            'mime' => self::MIME_PDF,
            'tamano_bytes' => (int) $archivo->getSize(),
            'sha256' => hash_file('sha256', $archivo->getRealPath()),
            'nombre' => $archivo->getClientOriginalName(),
        ];
    }

    /**
     * Comprueba que el descriptor apunta a un PDF de la carpeta de esa escuela.
     *
     * @param  array<string,mixed>  $archivo
     */
    public function validarDescriptor(array $archivo, string $escuelaCodigo): void
    {
        $prefijo = $this->urlPublica($this->carpeta($escuelaCodigo));
        $url = (string) ($archivo['url'] ?? '');

        $propio = str_starts_with($url, $prefijo)
            && preg_match('/^[0-9a-f-]{36}\.pdf$/', substr($url, strlen($prefijo))) === 1;
        if (! $propio || ($archivo['mime'] ?? null) !== self::MIME_PDF) {
            Log::warning('Manual rechazado: archivo inválido', [
                'module' => 'manuales',
                'errorCode' => ErrorCodes::ARCHIVO_INVALIDO,
                'escuelaCodigo' => $escuelaCodigo,
            ]);
            throw new ValidationException(ErrorCodes::ARCHIVO_INVALIDO, 'El archivo del manual no es válido: sube un PDF desde el formulario');
        }
    }

    private function carpeta(string $escuelaCodigo): string
    {
        return "escuelas/{$escuelaCodigo}/manuales/";
    }

    private function urlPublica(string $clave): string
    {
        $base = rtrim((string) config('filesystems.disks.contenido.public_url'), '/');

        return $base.'/'.config('filesystems.disks.contenido.bucket').'/'.$clave;
    }
}
