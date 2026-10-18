<?php

namespace App\Health;

use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\DB;
use Throwable;

/**
 * Health check en GET /health, mismo formato que el Actuator de los
 * servicios Spring ({status, components}). Endpoint de infraestructura: va
 * fuera de /api/v1 y sin el sobre ApiResponse.
 */
class HealthController
{
    public function __invoke(): JsonResponse
    {
        try {
            DB::connection('mongodb')->getDatabase()->command(['ping' => 1]);
            $mongodb = 'UP';
        } catch (Throwable) {
            $mongodb = 'DOWN';
        }

        return response()->json(
            ['status' => $mongodb, 'components' => ['mongodb' => ['status' => $mongodb]]],
            $mongodb === 'UP' ? 200 : 503,
        );
    }
}
