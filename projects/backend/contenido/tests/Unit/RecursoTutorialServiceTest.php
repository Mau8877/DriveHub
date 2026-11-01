<?php

namespace Tests\Unit;

use App\Common\Exceptions\BusinessException;
use App\Common\Exceptions\ErrorCodes;
use App\Features\Tutoriales\Enums\TipoRecurso;
use App\Features\Tutoriales\Services\RecursoTutorialService;
use Tests\TestCase;

/**
 * Qué recurso acepta un tutorial (CU33): un PDF de la carpeta de la escuela o un enlace de YouTube.
 */
class RecursoTutorialServiceTest extends TestCase
{
    private const ESCUELA = '0198f0a0-0000-7000-8000-000000000001';

    private const UUID = '0198f0a0-9999-7000-8000-000000000009';

    private RecursoTutorialService $servicio;

    protected function setUp(): void
    {
        parent::setUp();
        config(['filesystems.disks.contenido.public_url' => 'http://minio', 'filesystems.disks.contenido.bucket' => 'drivehub-contenido']);
        $this->servicio = new RecursoTutorialService;
    }

    private function pdf(string $carpeta = 'escuelas/'.self::ESCUELA.'/tutoriales/'): array
    {
        return [
            'url' => 'http://minio/drivehub-contenido/'.$carpeta.self::UUID.'.pdf',
            'mime' => 'application/pdf', 'tamano_bytes' => 10, 'sha256' => str_repeat('a', 64), 'nombre' => 'a.pdf', 'extra' => 'x',
        ];
    }

    private function invalido(TipoRecurso $tipo, array $recurso): void
    {
        try {
            $this->servicio->validar($tipo, $recurso, self::ESCUELA);
            $this->fail('Se esperaba ERR_CON_07');
        } catch (BusinessException $e) {
            $this->assertSame(ErrorCodes::RECURSO_TUTORIAL_INVALIDO, $e->errorCode);
        }
    }

    public function test_un_pdf_de_la_carpeta_de_la_escuela_es_valido_y_solo_guarda_el_descriptor(): void
    {
        $guardado = $this->servicio->validar(TipoRecurso::PDF, $this->pdf(), self::ESCUELA);

        $this->assertSame(['url', 'mime', 'tamano_bytes', 'sha256', 'nombre'], array_keys($guardado));
    }

    public function test_un_pdf_de_otra_escuela_de_manuales_o_que_no_es_pdf_es_invalido(): void
    {
        $this->invalido(TipoRecurso::PDF, $this->pdf('escuelas/otra/tutoriales/'));
        $this->invalido(TipoRecurso::PDF, $this->pdf('escuelas/'.self::ESCUELA.'/manuales/'));
        $this->invalido(TipoRecurso::PDF, ['mime' => 'text/plain'] + $this->pdf());
        $this->invalido(TipoRecurso::PDF, ['url' => 'https://youtu.be/dQw4w9WgXcQ']);
    }

    public function test_los_enlaces_de_youtube_son_validos_y_guardan_solo_la_direccion(): void
    {
        foreach ([
            'https://www.youtube.com/watch?v=dQw4w9WgXcQ',
            'https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ',
            'https://m.youtube.com/watch?v=dQw4w9WgXcQ&t=10s',
            'https://youtu.be/dQw4w9WgXcQ',
            'https://youtu.be/dQw4w9WgXcQ?si=abc',
            'https://www.youtube.com/shorts/dQw4w9WgXcQ',
            'https://www.youtube.com/embed/dQw4w9WgXcQ',
        ] as $url) {
            $this->assertSame(['url' => $url], $this->servicio->validar(TipoRecurso::ENLACE, ['url' => $url, 'mime' => 'x'], self::ESCUELA), $url);
        }
    }

    public function test_un_enlace_que_no_es_de_youtube_es_invalido(): void
    {
        foreach ([
            'http://www.youtube.com/watch?v=dQw4w9WgXcQ',
            'https://vimeo.com/123456789',
            'https://evil.com/watch?v=dQw4w9WgXcQ',
            'https://youtube.com.evil.com/watch?v=dQw4w9WgXcQ',
            'https://www.youtube.com/watch?v=corto',
            'https://www.youtube.com/',
            'javascript:alert(1)',
            '',
        ] as $url) {
            $this->invalido(TipoRecurso::ENLACE, ['url' => $url]);
        }
    }

    public function test_el_tipo_video_no_se_acepta(): void
    {
        $this->invalido(TipoRecurso::VIDEO, ['url' => 'https://youtu.be/dQw4w9WgXcQ']);
    }
}
