<?php

namespace Tests\Feature;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\NotFoundException;
use App\Features\Tutoriales\Enums\Audiencia;
use App\Features\Tutoriales\Enums\TipoRecurso;
use App\Features\Tutoriales\Models\Tutorial;
use App\Features\Tutoriales\Services\RecursoTutorialService;
use App\Features\Tutoriales\Services\TutorialService;
use Firebase\JWT\JWT;
use Illuminate\Http\UploadedFile;
use Illuminate\Pagination\LengthAwarePaginator;
use Illuminate\Support\Facades\Http;
use Mockery;
use Tests\TestCase;

/**
 * Contrato HTTP de los tutoriales (CU33 y CU35): autenticación con un JWT real (par de claves
 * generado en el test, JWKS simulado), rol, forma del sobre y validación. El service va simulado:
 * no hace falta MongoDB ni MinIO.
 */
class TutorialesApiTest extends TestCase
{
    private const ESCUELA = '0198f0a0-0000-7000-8000-000000000001';

    private const AUTOR = 'usuario-1';

    private const CODIGO = '0198f0a0-3333-7000-8000-000000000001';

    private $clavePrivada;

    protected function setUp(): void
    {
        parent::setUp();
        $this->clavePrivada = openssl_pkey_new(['private_key_bits' => 2048, 'private_key_type' => OPENSSL_KEYTYPE_RSA]);
        $detalle = openssl_pkey_get_details($this->clavePrivada)['rsa'];
        $b64 = fn (string $b) => rtrim(strtr(base64_encode($b), '+/', '-_'), '=');
        Http::fake(['*/.well-known/jwks.json' => Http::response(['keys' => [[
            'kty' => 'RSA', 'alg' => 'RS256', 'use' => 'sig', 'kid' => 'k1', 'n' => $b64($detalle['n']), 'e' => $b64($detalle['e']),
        ]]])]);
    }

    /**
     * @param  array<int,string>  $roles
     */
    private function sesion(array $roles = ['INSTRUCTOR'], ?string $escuela = self::ESCUELA): array
    {
        $claims = array_merge([
            'iss' => 'drivehub-identidad', 'sub' => self::AUTOR, 'roles' => $roles, 'iat' => time(), 'exp' => time() + 900,
        ], $escuela !== null ? ['escuela' => $escuela] : []);

        return ['Authorization' => 'Bearer '.JWT::encode($claims, $this->clavePrivada, 'RS256', 'k1')];
    }

    private function tutorial(EstadoPublicacion $estado = EstadoPublicacion::BORRADOR): Tutorial
    {
        $tutorial = new Tutorial([
            'escuela_codigo' => self::ESCUELA, 'autor_codigo' => self::AUTOR, 'titulo' => 'Estacionar', 'categoria' => 'B',
            'tipo' => TipoRecurso::PDF,
            'recurso' => ['url' => 'http://x/a.pdf', 'mime' => 'application/pdf', 'tamano_bytes' => 10, 'sha256' => str_repeat('a', 64), 'nombre' => 'a.pdf'],
            'audiencia' => ['ESTUDIANTE'], 'estado' => $estado, 'etiquetas' => ['vial'],
        ]);
        $tutorial->codigo_tutorial = self::CODIGO;

        return $tutorial;
    }

    private function cuerpo(): array
    {
        return [
            'titulo' => 'Cómo estacionar', 'categoria' => 'B', 'audiencia' => ['ESTUDIANTE', 'INSTRUCTOR'],
            'tipo' => 'PDF', 'etiquetas' => ['vial'],
            'recurso' => ['url' => 'http://x/a.pdf', 'mime' => 'application/pdf', 'tamanoBytes' => 10, 'sha256' => str_repeat('a', 64)],
        ];
    }

    public function test_sin_token_responde_401_y_con_otro_rol_403_en_la_gestion(): void
    {
        $this->getJson('/api/v1/tutoriales')->assertStatus(401)->assertJson(['error' => 'ERR_SYS_04']);

        foreach (['ESTUDIANTE', 'ADMIN_ESCUELA', 'SUPER_ADMIN'] as $rol) {
            $h = $this->sesion([$rol]);
            $this->getJson('/api/v1/tutoriales', $h)->assertStatus(403)->assertJson(['error' => 'ERR_SYS_05']);
            $this->postJson('/api/v1/tutoriales', $this->cuerpo(), $h)->assertStatus(403);
            $this->postJson('/api/v1/tutoriales/'.self::CODIGO.'/publicar', [], $h)->assertStatus(403);
        }
    }

    public function test_un_token_sin_escuela_responde_403_err_con_04(): void
    {
        $this->mock(TutorialService::class)->shouldNotReceive('listar');

        $this->getJson('/api/v1/tutoriales', $this->sesion(escuela: null))
            ->assertStatus(403)->assertJson(['error' => ErrorCodes::TOKEN_SIN_ESCUELA]);
    }

    public function test_listar_pasa_escuela_autor_y_filtros_y_devuelve_array_con_meta(): void
    {
        $this->mock(TutorialService::class)->shouldReceive('listar')->once()
            ->with(self::ESCUELA, self::AUTOR, 'est', EstadoPublicacion::PUBLICADO, TipoRecurso::PDF, 2, 10)
            ->andReturn(new LengthAwarePaginator([$this->tutorial()], 11, 10, 2));

        $this->getJson('/api/v1/tutoriales?page=2&pageSize=10&search=est&filter[estado]=PUBLICADO&filter[tipo]=PDF', $this->sesion())
            ->assertOk()
            ->assertJsonPath('data.0.codigo', self::CODIGO)
            ->assertJsonPath('data.0.recurso.tamanoBytes', 10)
            ->assertJsonPath('data.0.audiencia', ['ESTUDIANTE'])
            ->assertJsonPath('meta', ['page' => 2, 'pageSize' => 10, 'totalItems' => 11, 'totalPages' => 2])
            ->assertJsonMissingPath('data.0.id')
            ->assertJsonMissingPath('data.0.autor_codigo');
    }

    public function test_listar_con_parametros_invalidos_responde_400_err_sys_01(): void
    {
        $this->getJson('/api/v1/tutoriales?filter[tipo]=ROTO', $this->sesion())->assertStatus(400)->assertJson(['error' => 'ERR_SYS_01']);
        $this->getJson('/api/v1/tutoriales?pageSize=500', $this->sesion())->assertStatus(400);
    }

    public function test_crear_responde_201_y_escuela_y_autor_no_vienen_del_cuerpo(): void
    {
        $this->mock(TutorialService::class)->shouldReceive('crear')->once()
            ->withArgs(fn (string $escuela, string $autor, array $d) => $escuela === self::ESCUELA && $autor === self::AUTOR
                && $d['titulo'] === 'Cómo estacionar' && ! isset($d['escuela_codigo']) && ! isset($d['autor_codigo'])
                && $d['recurso']['tamano_bytes'] === 10 && ! isset($d['recurso']['tamanoBytes']))
            ->andReturn($this->tutorial());

        $this->postJson('/api/v1/tutoriales', $this->cuerpo() + ['escuela_codigo' => 'otra', 'autor_codigo' => 'otro'], $this->sesion())
            ->assertCreated()->assertJsonPath('status', 'success')->assertJsonPath('data.estado', 'BORRADOR');
    }

    public function test_crear_con_datos_invalidos_responde_400_sin_llamar_al_service(): void
    {
        $this->mock(TutorialService::class)->shouldNotReceive('crear');
        $h = $this->sesion();

        foreach ([
            ['titulo' => 'ab'],
            ['audiencia' => []],
            ['audiencia' => ['ADMIN']],
            ['tipo' => 'VIDEO'],
            ['categoria' => 'abc'],
            ['recurso' => ['mime' => 'application/pdf']],
        ] as $cambio) {
            $this->postJson('/api/v1/tutoriales', $cambio + $this->cuerpo(), $h)->assertStatus(400)->assertJson(['error' => 'ERR_SYS_01']);
        }
    }

    public function test_obtener_inexistente_responde_404_err_con_05(): void
    {
        $this->mock(TutorialService::class)->shouldReceive('obtener')->once()->with(self::ESCUELA, self::AUTOR, 'x')
            ->andThrow(new NotFoundException(ErrorCodes::TUTORIAL_NO_ENCONTRADO, 'El tutorial no existe'));

        $this->getJson('/api/v1/tutoriales/x', $this->sesion())->assertStatus(404)->assertJson(['error' => 'ERR_CON_05']);
    }

    public function test_editar_publicar_y_archivar_responden_el_tutorial(): void
    {
        $servicio = $this->mock(TutorialService::class);
        $servicio->shouldReceive('editar')->once()
            ->withArgs(fn (string $e, string $a, string $c, array $d) => $d === ['titulo' => 'Nuevo título'])
            ->andReturn($this->tutorial());
        $servicio->shouldReceive('publicar')->once()->with(self::ESCUELA, self::AUTOR, self::CODIGO)->andReturn($this->tutorial(EstadoPublicacion::PUBLICADO));
        $servicio->shouldReceive('archivar')->once()->with(self::ESCUELA, self::AUTOR, self::CODIGO)->andReturn($this->tutorial(EstadoPublicacion::ARCHIVADO));
        $h = $this->sesion();

        $this->patchJson('/api/v1/tutoriales/'.self::CODIGO, ['titulo' => 'Nuevo título', 'escuela_codigo' => 'x'], $h)->assertOk();
        $this->postJson('/api/v1/tutoriales/'.self::CODIGO.'/publicar', [], $h)->assertOk()->assertJsonPath('data.estado', 'PUBLICADO');
        $this->postJson('/api/v1/tutoriales/'.self::CODIGO.'/archivar', [], $h)->assertOk()->assertJsonPath('data.estado', 'ARCHIVADO');
    }

    public function test_editar_el_tipo_sin_recurso_o_el_recurso_sin_tipo_responde_400(): void
    {
        $h = $this->sesion();

        $this->patchJson('/api/v1/tutoriales/'.self::CODIGO, ['tipo' => 'ENLACE'], $h)->assertStatus(400);
        $this->patchJson('/api/v1/tutoriales/'.self::CODIGO, ['recurso' => ['url' => 'https://youtu.be/dQw4w9WgXcQ']], $h)->assertStatus(400);
    }

    public function test_subir_archivo_devuelve_el_descriptor_y_rechaza_lo_que_no_es_pdf(): void
    {
        $descriptor = ['url' => 'http://x/a.pdf', 'mime' => 'application/pdf', 'tamano_bytes' => 4, 'sha256' => str_repeat('a', 64), 'nombre' => 't.pdf'];
        $this->mock(RecursoTutorialService::class)->shouldReceive('subirPdf')->once()
            ->withArgs(fn ($archivo, string $escuela) => $escuela === self::ESCUELA)->andReturn($descriptor);
        $h = $this->sesion() + ['Accept' => 'application/json'];

        $pdf = UploadedFile::fake()->createWithContent('t.pdf', "%PDF-1.4\n%%EOF\n");
        $this->post('/api/v1/tutoriales/archivos', ['archivo' => $pdf], $h)
            ->assertCreated()->assertJsonPath('data.tamanoBytes', 4)->assertJsonMissingPath('data.tamano_bytes');

        $texto = UploadedFile::fake()->createWithContent('t.txt', 'hola');
        $this->post('/api/v1/tutoriales/archivos', ['archivo' => $texto], $h)
            ->assertStatus(400)->assertJson(['error' => 'ERR_SYS_01', 'message' => 'El tutorial debe ser un archivo PDF']);
    }

    public function test_la_consulta_la_hacen_estudiante_e_instructor_cada_uno_con_su_audiencia(): void
    {
        $servicio = $this->mock(TutorialService::class);
        $servicio->shouldReceive('listarPublicados')->once()
            ->with(self::ESCUELA, Audiencia::ESTUDIANTE, 'est', 'B', TipoRecurso::ENLACE, 1, 20)
            ->andReturn(new LengthAwarePaginator([$this->tutorial(EstadoPublicacion::PUBLICADO)], 1, 20, 1));
        $servicio->shouldReceive('listarPublicados')->once()
            ->with(self::ESCUELA, Audiencia::INSTRUCTOR, null, null, null, 1, 20)
            ->andReturn(new LengthAwarePaginator([], 0, 20, 1));
        $servicio->shouldReceive('obtenerPublicado')->once()->with(self::ESCUELA, Audiencia::ESTUDIANTE, self::CODIGO)
            ->andReturn($this->tutorial(EstadoPublicacion::PUBLICADO));

        $this->getJson('/api/v1/tutoriales/publicados?search=est&filter[categoria]=B&filter[tipo]=ENLACE', $this->sesion(['ESTUDIANTE']))
            ->assertOk()->assertJsonPath('data.0.estado', 'PUBLICADO')->assertJsonPath('meta.totalItems', 1);
        $this->getJson('/api/v1/tutoriales/publicados', $this->sesion(['INSTRUCTOR']))->assertOk()->assertJsonPath('data', []);
        $this->getJson('/api/v1/tutoriales/publicados/'.self::CODIGO, $this->sesion(['ESTUDIANTE']))
            ->assertOk()->assertJsonPath('data.codigo', self::CODIGO);
    }

    public function test_la_consulta_rechaza_otros_roles_y_un_tutorial_ajeno_es_404(): void
    {
        foreach (['ADMIN_ESCUELA', 'SUPER_ADMIN'] as $rol) {
            $this->getJson('/api/v1/tutoriales/publicados', $this->sesion([$rol]))->assertStatus(403)->assertJson(['error' => 'ERR_SYS_05']);
        }
        $this->getJson('/api/v1/tutoriales/publicados')->assertStatus(401);

        $this->mock(TutorialService::class)->shouldReceive('obtenerPublicado')->once()
            ->andThrow(new NotFoundException(ErrorCodes::TUTORIAL_NO_ENCONTRADO, 'El tutorial no existe'));
        $this->getJson('/api/v1/tutoriales/publicados/x', $this->sesion(['ESTUDIANTE']))->assertStatus(404)->assertJson(['error' => 'ERR_CON_05']);
    }

    public function test_la_consulta_con_token_sin_escuela_responde_403_err_con_04(): void
    {
        $this->mock(TutorialService::class)->shouldNotReceive('listarPublicados');

        $this->getJson('/api/v1/tutoriales/publicados', $this->sesion(['ESTUDIANTE'], null))
            ->assertStatus(403)->assertJson(['error' => ErrorCodes::TOKEN_SIN_ESCUELA]);
    }

    protected function tearDown(): void
    {
        Mockery::close();
        parent::tearDown();
    }
}
