<?php

namespace Database\Seeders;

use App\Common\Enums\EstadoPublicacion;
use App\Features\Manuales\Models\Manual;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

/**
 * Manuales de ejemplo para desarrollo: los de `escuelaexample`, en los tres estados y con un PDF
 * real de una página cada uno, subido al bucket drivehub-contenido de MinIO.
 *
 * ! Solo con APP_ENV=local (ver DatabaseSeeder). Nunca se siembra en producción.
 * * El codigo de la escuela es el fijo que siembran Escuela e Identidad (EscuelaSeeder). Se siembra
 * * por título: si ya existe, no se toca.
 */
class ManualSeeder extends Seeder
{
    private const ESCUELA = '0198f0a0-0000-7000-8000-000000000001';

    /**
     * @var array<int,array{0:string,1:string,2:string,3:string,4:EstadoPublicacion,5:array<int,string>}>
     */
    private function ejemplos(): array
    {
        return [
            ['Señales de tránsito', 'Todas las señales reglamentarias, preventivas e informativas.', 'GENERAL', 'Señalización', EstadoPublicacion::PUBLICADO, ['señales', 'tránsito']],
            ['Manejo defensivo', 'Técnicas para anticipar riesgos y evitar accidentes.', 'B', 'Seguridad vial', EstadoPublicacion::PUBLICADO, ['seguridad', 'práctica']],
            ['Mecánica básica del auto', 'Revisiones simples antes de salir: aceite, llantas, luces y frenos.', 'B', 'Mecánica', EstadoPublicacion::PUBLICADO, ['mecánica']],
            ['Conducción de motocicletas', 'Equipo de protección y maniobras básicas para la categoría A.', 'A', 'Motos', EstadoPublicacion::BORRADOR, ['motos']],
            ['Transporte de carga', 'Normas de carga y dimensiones para la categoría C.', 'C', 'Carga pesada', EstadoPublicacion::BORRADOR, []],
            ['Reglamento de tránsito 2019', 'Versión anterior del reglamento, ya reemplazada.', 'GENERAL', 'Normativa', EstadoPublicacion::ARCHIVADO, ['normativa']],
        ];
    }

    public function run(): void
    {
        $creados = 0;
        foreach ($this->ejemplos() as [$titulo, $descripcion, $categoria, $tema, $estado, $etiquetas]) {
            if (Manual::where('escuela_codigo', self::ESCUELA)->where('titulo', $titulo)->exists()) {
                continue;
            }

            $contenido = $this->pdf($titulo);
            $clave = 'escuelas/'.self::ESCUELA.'/manuales/'.Str::uuid7().'.pdf';
            Storage::disk('contenido')->put($clave, $contenido, ['ContentType' => 'application/pdf']);

            Manual::create([
                'escuela_codigo' => self::ESCUELA,
                'titulo' => $titulo,
                'descripcion' => $descripcion,
                'categoria' => $categoria,
                'tema' => $tema,
                'version' => 1,
                'archivo' => [
                    'url' => rtrim((string) config('filesystems.disks.contenido.public_url'), '/').'/'
                        .config('filesystems.disks.contenido.bucket').'/'.$clave,
                    'mime' => 'application/pdf',
                    'tamano_bytes' => strlen($contenido),
                    'sha256' => hash('sha256', $contenido),
                    'nombre' => Str::slug($titulo).'.pdf',
                ],
                'estado' => $estado,
                'etiquetas' => $etiquetas,
            ]);
            $creados++;
        }

        if ($creados > 0) {
            Log::info('Manuales de ejemplo creados para desarrollo', ['module' => 'manuales', 'cantidad' => $creados]);
        }
    }

    /**
     * PDF mínimo de una página con el título, con su tabla de referencias (xref) correcta.
     */
    private function pdf(string $titulo): string
    {
        $texto = str_replace(['\\', '(', ')'], ['\\\\', '\\(', '\\)'], mb_convert_encoding($titulo, 'ISO-8859-1', 'UTF-8'));
        $flujo = "BT /F1 22 Tf 50 150 Td ({$texto}) Tj ET\nBT /F1 11 Tf 50 120 Td (Manual de ejemplo de DriveHub) Tj ET";
        $objetos = [
            '<< /Type /Catalog /Pages 2 0 R >>',
            '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
            '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 420 240] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>',
            '<< /Length '.strlen($flujo)." >>\nstream\n{$flujo}\nendstream",
            '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>',
        ];

        $pdf = "%PDF-1.4\n";
        $posiciones = [];
        foreach ($objetos as $i => $objeto) {
            $posiciones[] = strlen($pdf);
            $pdf .= ($i + 1)." 0 obj\n{$objeto}\nendobj\n";
        }
        $xref = strlen($pdf);
        $pdf .= "xref\n0 ".(count($objetos) + 1)."\n0000000000 65535 f \n";
        foreach ($posiciones as $posicion) {
            $pdf .= sprintf("%010d 00000 n \n", $posicion);
        }

        return $pdf."trailer\n<< /Size ".(count($objetos) + 1)." /Root 1 0 R >>\nstartxref\n{$xref}\n%%EOF\n";
    }
}
