<?php

namespace Tests\Feature;

use App\Common\Exceptions\ConflictException;
use Illuminate\Support\Facades\Route;
use Tests\TestCase;

/**
 * Contrato del sobre de error (status, data, error) para cada tipo de
 * excepción. Las rutas de prueba se registran en el propio test: no hace
 * falta MongoDB para correrlo.
 */
class ManejoDeErroresTest extends TestCase
{
    protected function setUp(): void
    {
        parent::setUp();

        Route::get('/api/v1/prueba-negocio', fn () => throw new ConflictException('ERR_CON_01', 'Ya existe'));
        Route::post('/api/v1/prueba-validacion', fn () => request()->validate(['titulo' => 'required']));
        Route::get('/api/v1/prueba-inesperada', fn () => throw new \RuntimeException('conexión rechazada a mongo'));
    }

    public function test_business_exception_responde_su_codigo_y_su_http(): void
    {
        $this->getJson('/api/v1/prueba-negocio')
            ->assertStatus(409)
            ->assertJson(['status' => 'failed', 'data' => null, 'error' => 'ERR_CON_01', 'message' => 'Ya existe']);
    }

    public function test_validacion_fallida_responde_err_sys_01(): void
    {
        $this->postJson('/api/v1/prueba-validacion', [])
            ->assertStatus(400)
            ->assertJson(['status' => 'failed', 'error' => 'ERR_SYS_01']);
    }

    public function test_ruta_inexistente_responde_404_err_sys_03(): void
    {
        $this->getJson('/api/v1/no-existe')
            ->assertStatus(404)
            ->assertJson(['status' => 'failed', 'error' => 'ERR_SYS_03']);
    }

    public function test_error_no_controlado_oculta_el_detalle_detras_de_err_sys_00(): void
    {
        $this->getJson('/api/v1/prueba-inesperada')
            ->assertStatus(500)
            ->assertJson(['error' => 'ERR_SYS_00', 'message' => 'Ocurrió un error inesperado']);
    }
}
