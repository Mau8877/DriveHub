<?php

namespace Tests\Unit;

use App\Common\Responses\ApiResponse;
use Tests\TestCase;

class ApiResponseTest extends TestCase
{
    public function test_success_no_incluye_error_ni_meta(): void
    {
        $respuesta = ApiResponse::success(['titulo' => 'Señales'], 'Manual obtenido correctamente');

        $this->assertSame('success', $respuesta['status']);
        $this->assertSame(['titulo' => 'Señales'], $respuesta['data']);
        $this->assertArrayNotHasKey('error', $respuesta);
        $this->assertArrayNotHasKey('meta', $respuesta);
        $this->assertMatchesRegularExpression('/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/', $respuesta['timestamp']);
    }

    public function test_error_lleva_codigo_y_data_null(): void
    {
        $respuesta = ApiResponse::error('ERR_CON_01', 'El manual no existe');

        $this->assertSame('failed', $respuesta['status']);
        $this->assertNull($respuesta['data']);
        $this->assertSame('ERR_CON_01', $respuesta['error']);
    }
}
