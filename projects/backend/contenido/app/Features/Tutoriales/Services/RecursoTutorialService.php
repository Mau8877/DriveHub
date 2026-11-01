<?php

namespace App\Features\Tutoriales\Services;

use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\ValidationException;
use App\Features\Tutoriales\Enums\TipoRecurso;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

/**
 * El recurso de un tutorial (CU33): un PDF subido al bucket `drivehub-contenido` de MinIO o un
 * enlace de YouTube. El PDF se sube primero y el tutorial lo referencia, igual que en los manuales.
 *
 * * Cada escuela sube a su propia carpeta (`escuelas/{escuela}/tutoriales/`): un tutorial solo
 * * puede apuntar a archivos de la carpeta de su escuela.
 */
class RecursoTutorialService
{
    public const MIME_PDF = 'application/pdf';

    // * youtube.com/watch?v=ID, /shorts/ID, /embed/ID y youtu.be/ID, siempre por https
    private const PATRON_YOUTUBE = '~^https://(?:(?:www\.|m\.)?youtube\.com/(?:watch\?(?:[^#]*&)?v=|shorts/|embed/)|youtu\.be/)([A-Za-z0-9_-]{11})(?:[&?#/].*)?$~';

    /**
     * @return array{url:string,mime:string,tamano_bytes:int,sha256:string,nombre:string}
     */
    public function subirPdf(UploadedFile $archivo, string $escuelaCodigo): array
    {
        $clave = $this->carpeta($escuelaCodigo).Str::uuid7().'.pdf';
        Storage::disk('contenido')->put($clave, file_get_contents($archivo->getRealPath()), ['ContentType' => self::MIME_PDF]);

        Log::info('Archivo de tutorial subido', ['module' => 'tutoriales', 'escuelaCodigo' => $escuelaCodigo]);

        return [
            'url' => $this->urlPublica($clave),
            'mime' => self::MIME_PDF,
            'tamano_bytes' => (int) $archivo->getSize(),
            'sha256' => hash_file('sha256', $archivo->getRealPath()),
            'nombre' => $archivo->getClientOriginalName(),
        ];
    }

    /**
     * Comprueba el recurso según el tipo y devuelve lo que se guarda: un PDF de la carpeta de la
     * escuela con su descriptor completo, o un enlace de YouTube con solo su dirección.
     *
     * @param  array<string,mixed>  $recurso
     * @return array<string,mixed>
     */
    public function validar(TipoRecurso $tipo, array $recurso, string $escuelaCodigo): array
    {
        $url = trim((string) ($recurso['url'] ?? ''));

        $valido = match ($tipo) {
            TipoRecurso::PDF => $this->esPdfDeLaEscuela($recurso, $url, $escuelaCodigo),
            TipoRecurso::ENLACE => preg_match(self::PATRON_YOUTUBE, $url) === 1,
            TipoRecurso::VIDEO => false,
        };
        if (! $valido) {
            Log::warning('Tutorial rechazado: recurso inválido', [
                'module' => 'tutoriales',
                'errorCode' => ErrorCodes::RECURSO_TUTORIAL_INVALIDO,
                'tipo' => $tipo->value,
                'escuelaCodigo' => $escuelaCodigo,
            ]);
            throw new ValidationException(
                ErrorCodes::RECURSO_TUTORIAL_INVALIDO,
                $tipo === TipoRecurso::PDF
                    ? 'El PDF del tutorial no es válido: súbelo desde el formulario'
                    : 'El enlace debe ser de un video de YouTube',
            );
        }

        return $tipo === TipoRecurso::PDF
            ? array_intersect_key($recurso, array_flip(['url', 'mime', 'tamano_bytes', 'sha256', 'nombre']))
            : ['url' => $url];
    }

    /**
     * @param  array<string,mixed>  $recurso
     */
    private function esPdfDeLaEscuela(array $recurso, string $url, string $escuelaCodigo): bool
    {
        $prefijo = $this->urlPublica($this->carpeta($escuelaCodigo));

        return str_starts_with($url, $prefijo)
            && preg_match('/^[0-9a-f-]{36}\.pdf$/', substr($url, strlen($prefijo))) === 1
            && ($recurso['mime'] ?? null) === self::MIME_PDF;
    }

    private function carpeta(string $escuelaCodigo): string
    {
        return "escuelas/{$escuelaCodigo}/tutoriales/";
    }

    private function urlPublica(string $clave): string
    {
        $base = rtrim((string) config('filesystems.disks.contenido.public_url'), '/');

        return $base.'/'.config('filesystems.disks.contenido.bucket').'/'.$clave;
    }
}
