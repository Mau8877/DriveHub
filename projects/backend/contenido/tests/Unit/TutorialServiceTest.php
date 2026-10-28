<?php

namespace Tests\Unit;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Exceptions\BusinessException;
use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\ValidationException;
use App\Features\Tutoriales\Enums\Audiencia;
use App\Features\Tutoriales\Enums\TipoRecurso;
use App\Features\Tutoriales\Models\Tutorial;
use App\Features\Tutoriales\Repositories\TutorialRepository;
use App\Features\Tutoriales\Services\RecursoTutorialService;
use App\Features\Tutoriales\Services\TutorialService;
use Illuminate\Pagination\LengthAwarePaginator;
use Mockery;
use Mockery\MockInterface;
use Tests\TestCase;

/**
 * Lógica de los tutoriales (CU33 y CU35) con el repositorio y el servicio de recursos simulados:
 * propiedad del instructor, estados, audiencia y escuela.
 */
class TutorialServiceTest extends TestCase
{
    private const ESCUELA = '0198f0a0-0000-7000-8000-000000000001';

    private const AUTOR = '0198f0a0-2222-7000-8000-000000000001';

    private const CODIGO = '0198f0a0-3333-7000-8000-000000000001';

    private MockInterface $repositorio;

    private MockInterface $recursos;

    private TutorialService $servicio;

    protected function setUp(): void
    {
        parent::setUp();
        $this->repositorio = Mockery::mock(TutorialRepository::class);
        $this->recursos = Mockery::mock(RecursoTutorialService::class);
        $this->servicio = new TutorialService($this->repositorio, $this->recursos);
    }

    private function tutorial(EstadoPublicacion $estado, string $autor = self::AUTOR, array $audiencia = ['ESTUDIANTE']): Tutorial
    {
        $tutorial = new Tutorial([
            'escuela_codigo' => self::ESCUELA,
            'autor_codigo' => $autor,
            'titulo' => 'Estacionar',
            'categoria' => 'B',
            'tipo' => TipoRecurso::ENLACE,
            'recurso' => ['url' => 'https://youtu.be/dQw4w9WgXcQ'],
            'audiencia' => $audiencia,
            'estado' => $estado,
            'etiquetas' => [],
        ]);
        $tutorial->codigo_tutorial = self::CODIGO;

        return $tutorial;
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

    public function test_crear_guarda_borrador_con_escuela_autor_audiencia_y_datos_limpios(): void
    {
        $recurso = ['url' => 'https://youtu.be/dQw4w9WgXcQ'];
        $this->recursos->shouldReceive('validar')->once()
            ->with(TipoRecurso::ENLACE, $recurso, self::ESCUELA)->andReturn($recurso);
        $this->repositorio->shouldReceive('crear')->once()->withArgs(function (array $d) use ($recurso) {
            return $d['escuela_codigo'] === self::ESCUELA
                && $d['autor_codigo'] === self::AUTOR
                && $d['titulo'] === 'Estacionar'
                && $d['descripcion'] === null
                && $d['categoria'] === 'GENERAL'
                && $d['tipo'] === TipoRecurso::ENLACE
                && $d['recurso'] === $recurso
                && $d['audiencia'] === ['ESTUDIANTE', 'INSTRUCTOR']
                && $d['estado'] === EstadoPublicacion::BORRADOR
                && $d['etiquetas'] === ['vial'];
        })->andReturn($this->tutorial(EstadoPublicacion::BORRADOR));

        $creado = $this->servicio->crear(self::ESCUELA, self::AUTOR, [
            'titulo' => '  Estacionar ',
            'descripcion' => '  ',
            'tipo' => 'ENLACE',
            'recurso' => $recurso,
            'audiencia' => ['ESTUDIANTE', 'INSTRUCTOR', 'ESTUDIANTE'],
            'etiquetas' => [' vial ', 'vial', ''],
        ]);

        $this->assertSame(EstadoPublicacion::BORRADOR, $creado->estado);
    }

    public function test_crear_con_recurso_invalido_no_toca_el_repositorio(): void
    {
        $this->recursos->shouldReceive('validar')->andThrow(new ValidationException(ErrorCodes::RECURSO_TUTORIAL_INVALIDO, 'x'));
        $this->repositorio->shouldNotReceive('crear');

        $this->esperaCodigo(
            fn () => $this->servicio->crear(self::ESCUELA, self::AUTOR, [
                'titulo' => 'Estacionar', 'tipo' => 'ENLACE', 'recurso' => ['url' => 'x'], 'audiencia' => ['ESTUDIANTE'],
            ]),
            ErrorCodes::RECURSO_TUTORIAL_INVALIDO,
        );
    }

    public function test_obtener_de_otro_instructor_o_inexistente_lanza_err_con_05(): void
    {
        $this->repositorio->shouldReceive('buscar')->with(self::ESCUELA, self::CODIGO)->once()
            ->andReturn($this->tutorial(EstadoPublicacion::BORRADOR, 'otro-instructor'));
        $this->esperaCodigo(fn () => $this->servicio->obtener(self::ESCUELA, self::AUTOR, self::CODIGO), ErrorCodes::TUTORIAL_NO_ENCONTRADO);

        $this->repositorio->shouldReceive('buscar')->with(self::ESCUELA, 'nada')->once()->andReturn(null);
        $this->esperaCodigo(fn () => $this->servicio->obtener(self::ESCUELA, self::AUTOR, 'nada'), ErrorCodes::TUTORIAL_NO_ENCONTRADO);
    }

    public function test_editar_cambia_solo_lo_enviado_y_un_recurso_nuevo_se_valida(): void
    {
        $tutorial = $this->tutorial(EstadoPublicacion::BORRADOR);
        $this->repositorio->shouldReceive('buscar')->andReturn($tutorial);
        $this->recursos->shouldReceive('validar')->once()->andReturn(['url' => 'https://youtu.be/AAAAAAAAAAA']);
        $this->repositorio->shouldReceive('guardar')->once()->with($tutorial);

        $editado = $this->servicio->editar(self::ESCUELA, self::AUTOR, self::CODIGO, [
            'titulo' => ' Nuevo ',
            'audiencia' => ['INSTRUCTOR'],
            'tipo' => 'ENLACE',
            'recurso' => ['url' => 'https://youtu.be/AAAAAAAAAAA'],
        ]);

        $this->assertSame('Nuevo', $editado->titulo);
        $this->assertSame('B', $editado->categoria);
        $this->assertSame(['INSTRUCTOR'], $editado->audiencia);
        $this->assertSame('https://youtu.be/AAAAAAAAAAA', $editado->recurso['url']);
    }

    public function test_editar_un_tutorial_archivado_lanza_err_con_06(): void
    {
        $this->repositorio->shouldReceive('buscar')->andReturn($this->tutorial(EstadoPublicacion::ARCHIVADO));
        $this->repositorio->shouldNotReceive('guardar');

        $this->esperaCodigo(
            fn () => $this->servicio->editar(self::ESCUELA, self::AUTOR, self::CODIGO, ['titulo' => 'Otro']),
            ErrorCodes::TRANSICION_TUTORIAL_INVALIDA,
        );
    }

    public function test_publicar_desde_borrador_o_archivado_y_no_si_ya_esta_publicado(): void
    {
        foreach ([EstadoPublicacion::BORRADOR, EstadoPublicacion::ARCHIVADO] as $estado) {
            $repositorio = Mockery::mock(TutorialRepository::class);
            $repositorio->shouldReceive('buscar')->andReturn($this->tutorial($estado));
            $repositorio->shouldReceive('guardar')->once();

            $publicado = (new TutorialService($repositorio, $this->recursos))->publicar(self::ESCUELA, self::AUTOR, self::CODIGO);
            $this->assertSame(EstadoPublicacion::PUBLICADO, $publicado->estado);
        }

        $this->repositorio->shouldReceive('buscar')->andReturn($this->tutorial(EstadoPublicacion::PUBLICADO));
        $this->repositorio->shouldNotReceive('guardar');
        $this->esperaCodigo(
            fn () => $this->servicio->publicar(self::ESCUELA, self::AUTOR, self::CODIGO),
            ErrorCodes::TRANSICION_TUTORIAL_INVALIDA,
        );
    }

    public function test_archivar_solo_un_tutorial_publicado(): void
    {
        $this->repositorio->shouldReceive('buscar')->andReturn($this->tutorial(EstadoPublicacion::PUBLICADO));
        $this->repositorio->shouldReceive('guardar')->once();
        $this->assertSame(EstadoPublicacion::ARCHIVADO, $this->servicio->archivar(self::ESCUELA, self::AUTOR, self::CODIGO)->estado);

        foreach ([EstadoPublicacion::BORRADOR, EstadoPublicacion::ARCHIVADO] as $estado) {
            $repositorio = Mockery::mock(TutorialRepository::class);
            $repositorio->shouldReceive('buscar')->andReturn($this->tutorial($estado));
            $repositorio->shouldNotReceive('guardar');

            $this->esperaCodigo(
                fn () => (new TutorialService($repositorio, $this->recursos))->archivar(self::ESCUELA, self::AUTOR, self::CODIGO),
                ErrorCodes::TRANSICION_TUTORIAL_INVALIDA,
            );
        }
    }

    public function test_listar_publicados_pasa_la_escuela_y_la_audiencia(): void
    {
        $pagina = new LengthAwarePaginator([], 0, 20, 1);
        $this->repositorio->shouldReceive('listarPublicados')->once()
            ->with(self::ESCUELA, Audiencia::INSTRUCTOR, 'señ', 'B', TipoRecurso::PDF, 1, 20)->andReturn($pagina);

        $this->assertSame($pagina, $this->servicio->listarPublicados(self::ESCUELA, Audiencia::INSTRUCTOR, 'señ', 'B', TipoRecurso::PDF, 1, 20));
    }

    public function test_obtener_publicado_exige_publicado_y_la_audiencia(): void
    {
        $this->repositorio->shouldReceive('buscar')->andReturn($this->tutorial(EstadoPublicacion::PUBLICADO, audiencia: ['ESTUDIANTE']));
        $this->assertSame(self::CODIGO, $this->servicio->obtenerPublicado(self::ESCUELA, Audiencia::ESTUDIANTE, self::CODIGO)->codigo_tutorial);

        $this->esperaCodigo(
            fn () => $this->servicio->obtenerPublicado(self::ESCUELA, Audiencia::INSTRUCTOR, self::CODIGO),
            ErrorCodes::TUTORIAL_NO_ENCONTRADO,
        );

        foreach ([EstadoPublicacion::BORRADOR, EstadoPublicacion::ARCHIVADO] as $estado) {
            $repositorio = Mockery::mock(TutorialRepository::class);
            $repositorio->shouldReceive('buscar')->andReturn($this->tutorial($estado));
            $this->esperaCodigo(
                fn () => (new TutorialService($repositorio, $this->recursos))->obtenerPublicado(self::ESCUELA, Audiencia::ESTUDIANTE, self::CODIGO),
                ErrorCodes::TUTORIAL_NO_ENCONTRADO,
            );
        }
    }

    protected function tearDown(): void
    {
        Mockery::close();
        parent::tearDown();
    }
}
