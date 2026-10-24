<?php

namespace Tests\Feature;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\NotFoundException;
use App\Features\Manuales\Models\Manual;
use App\Features\Manuales\Services\ArchivoManualService;
use App\Features\Manuales\Services\ManualService;
use Firebase\JWT\JWT;
use Illuminate\Http\UploadedFile;
use Illuminate\Pagination\LengthAwarePaginator;
use Illuminate\Support\Facades\Http;
use Mockery;
use Tests\TestCase;

/**
 * Contrato HTTP de los manuales (CU32): autenticación con un JWT real (par de claves generado en
 * el test, JWKS simulado), rol, forma del sobre y validación. El service va simulado: no hace
 * falta MongoDB ni MinIO.
 */
class ManualesApiTest extends TestCase
{
    private const ESCUELA = '0198f0a0-0000-7000-8000-000000000001';

    private const CODIGO = '0198f0a0-1111-7000-8000-000000000001';

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
     * @param  array<string,mixed>  $extra
     */
    private function token(array $roles = ['ADMIN_ESCUELA'], ?string $escuela = self::ESCUELA, array $extra = []): string
    {
        $claims = array_merge([
            'iss' => 'drivehub-identidad', 'sub' => 'usuario-1', 'roles' => $roles, 'iat' => time(), 'exp' => time() + 900,
        ], $escuela !== null ? ['escuela' => $escuela] : [], $extra);

        return JWT::encode($claims, $this->clavePrivada, 'RS256', 'k1');
    }

    /**
     * @return array<string,string>
     */
    private function sesion(string ...$roles): array
    {
        return ['Authorization' => 'Bearer '.$this->token($roles ?: ['ADMIN_ESCUELA'])];
    }

    private function manual(EstadoPublicacion $estado = EstadoPublicacion::BORRADOR): Manual
    {
        $manual = new Manual([
            'escuela_codigo' => self::ESCUELA, 'titulo' => 'Señales', 'categoria' => 'GENERAL', 'version' => 1,
            'archivo' => ['url' => 'http://x/a.pdf', 'mime' => 'application/pdf', 'tamano_bytes' => 10, 'sha256' => str_repeat('a', 64), 'nombre' => 'a.pdf'],
            'estado' => $estado, 'etiquetas' => ['vial'],
        ]);
        $manual->codigo_manual = self::CODIGO;

        return $manual;
    }

    private function cuerpo(): array
    {
        return [
            'titulo' => 'Señales de tránsito', 'categoria' => 'B', 'etiquetas' => ['vial'],
            'archivo' => ['url' => 'http://x/a.pdf', 'mime' => 'application/pdf', 'tamanoBytes' => 10, 'sha256' => str_repeat('a', 64)],
        ];
    }

    public function test_sin_token_o_con_token_invalido_responde_401_err_sys_04(): void
    {
        $this->getJson('/api/v1/manuales')->assertStatus(401)->assertJson(['status' => 'failed', 'error' => 'ERR_SYS_04']);
        $this->getJson('/api/v1/manuales', ['Authorization' => 'Bearer basura'])->assertStatus(401)->assertJson(['error' => 'ERR_SYS_04']);
        $vencido = $this->token(extra: ['exp' => time() - 60]);
        $this->getJson('/api/v1/manuales', ['Authorization' => "Bearer {$vencido}"])->assertStatus(401);
        $otroEmisor = $this->token(extra: ['iss' => 'otro']);
        $this->getJson('/api/v1/manuales', ['Authorization' => "Bearer {$otroEmisor}"])->assertStatus(401);
    }

    public function test_con_otro_rol_responde_403_err_sys_05_en_cada_endpoint(): void
    {
        foreach (['SUPER_ADMIN', 'INSTRUCTOR', 'ESTUDIANTE'] as $rol) {
            $h = $this->sesion($rol);
            $this->getJson('/api/v1/manuales', $h)->assertStatus(403)->assertJson(['error' => 'ERR_SYS_05']);
            $this->postJson('/api/v1/manuales', $this->cuerpo(), $h)->assertStatus(403);
            $this->postJson('/api/v1/manuales/'.self::CODIGO.'/publicar', [], $h)->assertStatus(403);
        }
    }

    public function test_un_token_sin_escuela_responde_403_err_con_04(): void
    {
        $this->mock(ManualService::class)->shouldNotReceive('listar');
        $sinEscuela = $this->token(escuela: null);

        $this->getJson('/api/v1/manuales', ['Authorization' => "Bearer {$sinEscuela}"])
            ->assertStatus(403)->assertJson(['error' => ErrorCodes::TOKEN_SIN_ESCUELA]);
    }

    public function test_listar_devuelve_array_con_meta_y_pasa_los_filtros_con_la_escuela_del_token(): void
    {
        $this->mock(ManualService::class)->shouldReceive('listar')
            ->once()->with(self::ESCUELA, 'señ', EstadoPublicacion::PUBLICADO, 'B', 2, 10)
            ->andReturn(new LengthAwarePaginator([$this->manual()], 11, 10, 2));

        $this->getJson('/api/v1/manuales?page=2&pageSize=10&search=señ&filter[estado]=PUBLICADO&filter[categoria]=B', $this->sesion())
            ->assertOk()
            ->assertJsonPath('status', 'success')
            ->assertJsonPath('data.0.codigo', self::CODIGO)
            ->assertJsonPath('data.0.archivo.tamanoBytes', 10)
            ->assertJsonPath('meta', ['page' => 2, 'pageSize' => 10, 'totalItems' => 11, 'totalPages' => 2])
            ->assertJsonMissingPath('data.0.id');
    }

    public function test_listar_con_parametros_invalidos_responde_400_err_sys_01(): void
    {
        $this->getJson('/api/v1/manuales?filter[estado]=ROTO', $this->sesion())->assertStatus(400)->assertJson(['error' => 'ERR_SYS_01']);
        $this->getJson('/api/v1/manuales?pageSize=500', $this->sesion())->assertStatus(400);
        $this->getJson('/api/v1/manuales?page=0', $this->sesion())->assertStatus(400);
    }

    public function test_crear_responde_201_y_la_escuela_no_viene_del_cuerpo(): void
    {
        $this->mock(ManualService::class)->shouldReceive('crear')->once()
            ->withArgs(fn (string $escuela, array $d) => $escuela === self::ESCUELA && $d['titulo'] === 'Señales de tránsito' && ! isset($d['escuela_codigo']) && $d['archivo']['tamano_bytes'] === 10 && ! isset($d['archivo']['tamanoBytes']))
            ->andReturn($this->manual());

        $this->postJson('/api/v1/manuales', $this->cuerpo() + ['escuela_codigo' => 'otra'], $this->sesion())
            ->assertCreated()
            ->assertJsonPath('data.estado', 'BORRADOR')
            ->assertJsonPath('data.version', 1)
            ->assertJsonMissingPath('error');
    }

    public function test_crear_con_datos_invalidos_responde_400_err_sys_01_sin_llamar_al_service(): void
    {
        $this->mock(ManualService::class)->shouldNotReceive('crear');
        $h = $this->sesion();
        $malos = [
            ['titulo' => 'ab'],
            ['titulo' => str_repeat('a', 151)],
            ['categoria' => 'general'],
            ['categoria' => 'ABCD'],
            ['etiquetas' => array_fill(0, 11, 'x')],
            ['archivo' => null],
            ['archivo' => ['url' => 'http://x/a.pdf', 'mime' => 'application/pdf', 'tamanoBytes' => 0, 'sha256' => str_repeat('a', 64)]],
            ['archivo' => ['url' => 'http://x/a.pdf', 'mime' => 'application/pdf', 'tamanoBytes' => 5, 'sha256' => 'zzz']],
        ];
        foreach ($malos as $cambio) {
            $this->postJson('/api/v1/manuales', array_merge($this->cuerpo(), $cambio), $h)
                ->assertStatus(400)->assertJson(['status' => 'failed', 'error' => 'ERR_SYS_01']);
        }
    }

    public function test_obtener_inexistente_responde_404_err_con_01(): void
    {
        $this->mock(ManualService::class)->shouldReceive('obtener')->with(self::ESCUELA, self::CODIGO)
            ->andThrow(new NotFoundException(ErrorCodes::MANUAL_NO_ENCONTRADO, 'El manual no existe'));

        $this->getJson('/api/v1/manuales/'.self::CODIGO, $this->sesion())
            ->assertStatus(404)->assertJson(['error' => 'ERR_CON_01', 'message' => 'El manual no existe']);
    }

    public function test_editar_pasa_solo_lo_validado(): void
    {
        $this->mock(ManualService::class)->shouldReceive('editar')->once()
            ->with(self::ESCUELA, self::CODIGO, ['tema' => 'Tránsito'])->andReturn($this->manual());

        $this->patchJson('/api/v1/manuales/'.self::CODIGO, ['tema' => 'Tránsito', 'estado' => 'PUBLICADO'], $this->sesion())
            ->assertOk()->assertJsonPath('data.codigo', self::CODIGO);
    }

    public function test_publicar_y_archivar_responden_el_manual_con_su_estado(): void
    {
        $servicio = $this->mock(ManualService::class);
        $servicio->shouldReceive('publicar')->once()->with(self::ESCUELA, self::CODIGO, 'usuario-1')
            ->andReturn($this->manual(EstadoPublicacion::PUBLICADO));
        $servicio->shouldReceive('archivar')->once()->with(self::ESCUELA, self::CODIGO)
            ->andReturn($this->manual(EstadoPublicacion::ARCHIVADO));

        $this->postJson('/api/v1/manuales/'.self::CODIGO.'/publicar', [], $this->sesion())
            ->assertOk()->assertJsonPath('data.estado', 'PUBLICADO');
        $this->postJson('/api/v1/manuales/'.self::CODIGO.'/archivar', [], $this->sesion())
            ->assertOk()->assertJsonPath('data.estado', 'ARCHIVADO');
    }

    public function test_subir_archivo_devuelve_el_descriptor_y_rechaza_lo_que_no_es_pdf(): void
    {
        $descriptor = ['url' => 'http://x/a.pdf', 'mime' => 'application/pdf', 'tamano_bytes' => 4, 'sha256' => str_repeat('a', 64), 'nombre' => 'm.pdf'];
        $this->mock(ArchivoManualService::class)->shouldReceive('subir')->once()
            ->withArgs(fn ($archivo, string $escuela) => $escuela === self::ESCUELA)->andReturn($descriptor);

        $pdf = UploadedFile::fake()->createWithContent('m.pdf', "%PDF-1.4\n%%EOF\n");
        $this->post('/api/v1/manuales/archivos', ['archivo' => $pdf], $this->sesion() + ['Accept' => 'application/json'])
            ->assertCreated()->assertJsonPath('data.url', 'http://x/a.pdf')->assertJsonPath('data.tamanoBytes', 4)->assertJsonMissingPath('data.tamano_bytes');

        $texto = UploadedFile::fake()->createWithContent('m.txt', 'hola');
        $this->post('/api/v1/manuales/archivos', ['archivo' => $texto], $this->sesion() + ['Accept' => 'application/json'])
            ->assertStatus(400)->assertJson(['error' => 'ERR_SYS_01', 'message' => 'El manual debe ser un archivo PDF']);
        $this->post('/api/v1/manuales/archivos', [], $this->sesion() + ['Accept' => 'application/json'])
            ->assertStatus(400)->assertJson(['message' => 'Elige el PDF del manual']);
    }

    protected function tearDown(): void
    {
        Mockery::close();
        parent::tearDown();
    }
}
