<?php

namespace App\Features\Tutoriales\Controllers;

use App\Common\Responses\ApiResponse;
use App\Features\Tutoriales\Enums\Audiencia;
use App\Features\Tutoriales\Enums\TipoRecurso;
use App\Features\Tutoriales\Requests\ListarTutorialesRequest;
use App\Features\Tutoriales\Resources\TutorialResource;
use App\Features\Tutoriales\Services\TutorialService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Consulta de tutoriales (CU35): el Estudiante y el Instructor, solo lectura y solo los publicados
 * de su escuela dirigidos a su audiencia (middleware `jwt` + `rol`, ver routes/api.php).
 */
class TutorialConsultaController
{
    use IdentificaAlSolicitante;

    public function __construct(private readonly TutorialService $tutoriales) {}

    public function index(ListarTutorialesRequest $request): JsonResponse
    {
        $filtro = (array) $request->query('filter', []);

        return $this->lista($this->tutoriales->listarPublicados(
            $this->escuela($request),
            $this->audiencia($request),
            $request->query('search'),
            $filtro['categoria'] ?? null,
            isset($filtro['tipo']) ? TipoRecurso::from($filtro['tipo']) : null,
            (int) $request->query('page', 1),
            (int) $request->query('pageSize', 20),
        ));
    }

    public function show(Request $request, string $codigo): JsonResponse
    {
        $tutorial = $this->tutoriales->obtenerPublicado($this->escuela($request), $this->audiencia($request), $codigo);

        return response()->json(ApiResponse::success((new TutorialResource($tutorial))->resolve(), 'Tutorial obtenido correctamente'));
    }

    // * Quien es Instructor ve lo dirigido a instructores; el resto (Estudiante), lo dirigido a estudiantes
    private function audiencia(Request $request): Audiencia
    {
        return $this->usuario($request)->tieneRol('INSTRUCTOR') ? Audiencia::INSTRUCTOR : Audiencia::ESTUDIANTE;
    }
}
