<?php

namespace App\Features\Manuales\Controllers;

use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\ForbiddenException;
use App\Common\Responses\ApiResponse;
use App\Common\Security\UsuarioAutenticado;
use App\Features\Manuales\Requests\ListarManualesRequest;
use App\Features\Manuales\Resources\ManualResource;
use App\Features\Manuales\Services\ManualService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Consulta de manuales del Estudiante (CU34): solo lectura y solo los publicados de su escuela
 * (middleware `jwt` + `rol:ESTUDIANTE`, ver routes/api.php). La escuela sale del token.
 */
class ManualConsultaController
{
    public function __construct(private readonly ManualService $manuales) {}

    public function index(ListarManualesRequest $request): JsonResponse
    {
        $filtro = (array) $request->query('filter', []);

        $resultado = $this->manuales->listarPublicados(
            $this->escuela($request),
            $request->query('search'),
            $filtro['categoria'] ?? null,
            (int) $request->query('page', 1),
            (int) $request->query('pageSize', 20),
        );

        return response()->json(ApiResponse::successList(
            ManualResource::collection($resultado->items())->resolve(),
            'Manuales obtenidos correctamente',
            [
                'page' => $resultado->currentPage(),
                'pageSize' => $resultado->perPage(),
                'totalItems' => $resultado->total(),
                'totalPages' => $resultado->lastPage(),
            ],
        ));
    }

    public function show(Request $request, string $codigo): JsonResponse
    {
        $manual = $this->manuales->obtenerPublicado($this->escuela($request), $codigo);

        return response()->json(ApiResponse::success((new ManualResource($manual))->resolve(), 'Manual obtenido correctamente'));
    }

    // * Un Estudiante siempre trae la escuela en el token (CU02); sin ella no hay tenant que filtrar
    private function escuela(Request $request): string
    {
        /** @var UsuarioAutenticado $usuario */
        $usuario = $request->attributes->get('usuario');
        if ($usuario->escuelaCodigo === null || $usuario->escuelaCodigo === '') {
            throw new ForbiddenException(ErrorCodes::TOKEN_SIN_ESCUELA, 'Tu cuenta no pertenece a ninguna escuela');
        }

        return $usuario->escuelaCodigo;
    }
}
