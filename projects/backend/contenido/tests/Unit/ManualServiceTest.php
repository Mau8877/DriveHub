<?php

namespace Tests\Unit;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Exceptions\BusinessException;
use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\ValidationException;
use App\Features\Manuales\Models\Manual;
use App\Features\Manuales\Repositories\ManualRepository;
use App\Features\Manuales\Services\ArchivoManualService;
use App\Features\Manuales\Services\ManualService;
use Illuminate\Pagination\LengthAwarePaginator;
use Mockery;
use Mockery\MockInterface;
use Tests\TestCase;

/**
 * Lógica de los manuales (CU32) con el repositorio y el almacén de archivos simulados: no hace
 * falta MongoDB ni MinIO.
 */
class ManualServiceTest extends TestCase
{
    private const ESCUELA = '0198f0a0-0000-7000-8000-000000000001';

    private const CODIGO = '0198f0a0-1111-7000-8000-000000000001';

    private MockInterface $repositorio;

    private MockInterface $archivos;

    private ManualService $servicio;

    protected function setUp(): void
    {
        parent::setUp();
        $this->repositorio = Mockery::mock(ManualRepository::class);
        $this->archivos = Mockery::mock(ArchivoManualService::class);
        $this->servicio = new ManualService($this->repositorio, $this->archivos);
    }

    private function descriptor(string $nombre = 'a'): array
    {
        return ['url' => "http://x/{$nombre}.pdf", 'mime' => 'application/pdf', 'tamano_bytes' => 10, 'sha256' => str_repeat('a', 64)];
    }

    private function manual(EstadoPublicacion $estado, int $version = 1): Manual
    {
        $manual = new Manual([
            'escuela_codigo' => self::ESCUELA,
            'titulo' => 'Señales',
            'categoria' => 'GENERAL',
            'version' => $version,
            'archivo' => $this->descriptor(),
            'estado' => $estado,
            'etiquetas' => [],
        ]);
        $manual->codigo_manual = self::CODIGO;

        return $manual;
    }

    private function esperaCodigo(callable $accion, string $codigo): void
    {
        try {
            $accion();
            $this->fail('Se esperaba una BusinessException');
        } catch (BusinessException $e) {
            $this->assertSame($codigo, $e->errorCode);
        }
    }

    private function sinGuardar(): void
    {
        $this->repositorio->shouldNotReceive('guardar');
    }

    public function test_crear_guarda_borrador_version_1_con_la_escuela_y_datos_limpios(): void
    {
        $this->archivos->shouldReceive('validarDescriptor')->once();
        $this->repositorio->shouldReceive('crear')->once()->withArgs(function (array $d) {
            return $d['escuela_codigo'] === self::ESCUELA
                && $d['titulo'] === 'Señales'
                && $d['descripcion'] === null
                && $d['categoria'] === 'GENERAL'
                && $d['version'] === 1
                && $d['estado'] === EstadoPublicacion::BORRADOR
                && $d['etiquetas'] === ['vial', 'a'];
        })->andReturn($this->manual(EstadoPublicacion::BORRADOR));

        $this->servicio->crear(self::ESCUELA, [
            'titulo' => '  Señales ',
            'descripcion' => '   ',
            'etiquetas' => [' vial ', 'vial', '', 'a'],
            'archivo' => $this->descriptor(),
        ]);
    }

    public function test_crear_con_archivo_de_otra_escuela_no_toca_el_repositorio(): void
    {
        $this->archivos->shouldReceive('validarDescriptor')->andThrow(
            new ValidationException(ErrorCodes::ARCHIVO_INVALIDO, 'x'),
        );
        $this->repositorio->shouldNotReceive('crear');

        $this->esperaCodigo(
            fn () => $this->servicio->crear(self::ESCUELA, ['titulo' => 'Señales', 'archivo' => $this->descriptor()]),
            ErrorCodes::ARCHIVO_INVALIDO,
        );
    }

    public function test_obtener_con_manual_inexistente_o_de_otra_escuela_lanza_err_con_01(): void
    {
        $this->repositorio->shouldReceive('buscar')->with(self::ESCUELA, self::CODIGO)->andReturn(null);

        $this->esperaCodigo(fn () => $this->servicio->obtener(self::ESCUELA, self::CODIGO), ErrorCodes::MANUAL_NO_ENCONTRADO);
    }

    public function test_listar_publicados_pide_solo_el_estado_publicado_de_la_escuela(): void
    {
        $pagina = new LengthAwarePaginator([], 0, 20, 1);
        $this->repositorio->shouldReceive('listar')->once()
            ->with(self::ESCUELA, 'señ', EstadoPublicacion::PUBLICADO, 'B', 1, 20)->andReturn($pagina);

        $this->assertSame($pagina, $this->servicio->listarPublicados(self::ESCUELA, 'señ', 'B', 1, 20));
    }

    public function test_obtener_publicado_devuelve_el_manual_publicado(): void
    {
        $this->repositorio->shouldReceive('buscar')->with(self::ESCUELA, self::CODIGO)->andReturn($this->manual(EstadoPublicacion::PUBLICADO));

        $this->assertSame(self::CODIGO, $this->servicio->obtenerPublicado(self::ESCUELA, self::CODIGO)->codigo_manual);
    }

    public function test_obtener_publicado_con_borrador_o_archivado_lanza_err_con_01(): void
    {
        foreach ([EstadoPublicacion::BORRADOR, EstadoPublicacion::ARCHIVADO] as $estado) {
            $repositorio = Mockery::mock(ManualRepository::class);
            $repositorio->shouldReceive('buscar')->andReturn($this->manual($estado));

            $this->esperaCodigo(
                fn () => (new ManualService($repositorio, $this->archivos))->obtenerPublicado(self::ESCUELA, self::CODIGO),
                ErrorCodes::MANUAL_NO_ENCONTRADO,
            );
        }
    }

    public function test_editar_cambia_solo_lo_enviado_y_con_archivo_nuevo_sube_la_version(): void
    {
        $manual = $this->manual(EstadoPublicacion::PUBLICADO, version: 2);
        $this->repositorio->shouldReceive('buscar')->andReturn($manual);
        $this->archivos->shouldReceive('validarDescriptor')->once();
        $this->repositorio->shouldReceive('guardar')->once();

        $editado = $this->servicio->editar(self::ESCUELA, self::CODIGO, [
            'tema' => 'Tránsito',
            'archivo' => $this->descriptor('b'),
        ]);

        $this->assertSame('Tránsito', $editado->tema);
        $this->assertSame('Señales', $editado->titulo);
        $this->assertSame(3, $editado->version);
        $this->assertSame('http://x/b.pdf', $editado->archivo['url']);
        $this->assertSame(EstadoPublicacion::PUBLICADO, $editado->estado);
    }

    public function test_editar_con_el_mismo_archivo_no_sube_la_version(): void
    {
        $manual = $this->manual(EstadoPublicacion::BORRADOR);
        $this->repositorio->shouldReceive('buscar')->andReturn($manual);
        $this->archivos->shouldNotReceive('validarDescriptor');
        $this->repositorio->shouldReceive('guardar')->once();

        $editado = $this->servicio->editar(self::ESCUELA, self::CODIGO, ['archivo' => $this->descriptor()]);

        $this->assertSame(1, $editado->version);
    }

    public function test_editar_un_manual_archivado_lanza_err_con_02(): void
    {
        $this->repositorio->shouldReceive('buscar')->andReturn($this->manual(EstadoPublicacion::ARCHIVADO));
        $this->sinGuardar();

        $this->esperaCodigo(
            fn () => $this->servicio->editar(self::ESCUELA, self::CODIGO, ['titulo' => 'Otro']),
            ErrorCodes::TRANSICION_MANUAL_INVALIDA,
        );
    }

    public function test_publicar_desde_borrador_o_archivado_deja_publicado_con_quien_lo_hizo(): void
    {
        foreach ([EstadoPublicacion::BORRADOR, EstadoPublicacion::ARCHIVADO] as $estado) {
            $repositorio = Mockery::mock(ManualRepository::class);
            $repositorio->shouldReceive('buscar')->andReturn($this->manual($estado));
            $repositorio->shouldReceive('guardar')->once();

            $manual = (new ManualService($repositorio, $this->archivos))->publicar(self::ESCUELA, self::CODIGO, 'usuario-1');

            $this->assertSame(EstadoPublicacion::PUBLICADO, $manual->estado);
            $this->assertSame('usuario-1', $manual->publicado_por_codigo);
        }
    }

    public function test_publicar_un_manual_ya_publicado_lanza_err_con_02(): void
    {
        $this->repositorio->shouldReceive('buscar')->andReturn($this->manual(EstadoPublicacion::PUBLICADO));
        $this->sinGuardar();

        $this->esperaCodigo(
            fn () => $this->servicio->publicar(self::ESCUELA, self::CODIGO, 'usuario-1'),
            ErrorCodes::TRANSICION_MANUAL_INVALIDA,
        );
    }

    public function test_archivar_solo_un_manual_publicado(): void
    {
        $this->repositorio->shouldReceive('buscar')->andReturn($this->manual(EstadoPublicacion::PUBLICADO));
        $this->repositorio->shouldReceive('guardar')->once();

        $this->assertSame(EstadoPublicacion::ARCHIVADO, $this->servicio->archivar(self::ESCUELA, self::CODIGO)->estado);

        foreach ([EstadoPublicacion::BORRADOR, EstadoPublicacion::ARCHIVADO] as $estado) {
            $repositorio = Mockery::mock(ManualRepository::class);
            $repositorio->shouldReceive('buscar')->andReturn($this->manual($estado));
            $repositorio->shouldNotReceive('guardar');

            $this->esperaCodigo(
                fn () => (new ManualService($repositorio, $this->archivos))->archivar(self::ESCUELA, self::CODIGO),
                ErrorCodes::TRANSICION_MANUAL_INVALIDA,
            );
        }
    }
}
