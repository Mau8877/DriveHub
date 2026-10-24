<?php

namespace App\Features\Manuales\Controllers;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\ForbiddenException;
use App\Common\Responses\ApiResponse;
use App\Common\Security\UsuarioAutenticado;
use App\Features\Manuales\Models\Manual;
use App\Features\Manuales\Requests\CrearManualRequest;
use App\Features\Manuales\Requests\EditarManualRequest;
use App\Features\Manuales\Requests\ListarManualesRequest;
use App\Features\Manuales\Requests\SubirArchivoManualRequest;
use App\Features\Manuales\Resources\ManualResource;
use App\Features\Manuales\Services\ArchivoManualService;
use App\Features\Manuales\Services\ManualService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Gestión de manuales (CU32): solo el Administrador de escuela (middleware `jwt` + `rol`, ver
 * routes/api.php). Controller delgado: valida con el FormRequest, llama a un método del service y
 * envuelve en el sobre estándar. La escuela sale del token, nunca del cuerpo ni de la URL.
 */
class ManualController
{
    public function __construct(
        private readonly ManualService $manuales,
        private readonly ArchivoManualService $archivos,
    ) {}

    public function index(ListarManualesRequest $request): JsonResponse
    {
        $pagina = (int) $request->query('page', 1);
        $tamano = (int) $request->query('pageSize', 20);
        $filtro = (array) $request->query('filter', []);
        $estado = isset($filtro['estado']) ? EstadoPublicacion::from($filtro['estado']) : null;

        $resultado = $this->manuales->listar(
            $this->escuela($request), $request->query('search'), $estado, $filtro['categoria'] ?? null, $pagina, $tamano,
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
        return $this->ok($this->manuales->obtener($this->escuela($request), $codigo), 'Manual obtenido correctamente');
    }

    public function store(CrearManualRequest $request): JsonResponse
    {
        $manual = $this->manuales->crear($this->escuela($request), $this->aInterno($request->validated()));

        return response()->json(
            ApiResponse::success((new ManualResource($manual))->resolve(), 'Manual creado correctamente'),
            201,
        );
    }

    public function update(EditarManualRequest $request, string $codigo): JsonResponse
    {
        return $this->ok(
            $this->manuales->editar($this->escuela($request), $codigo, $this->aInterno($request->validated())),
            'Manual actualizado correctamente',
        );
    }

    public function publicar(Request $request, string $codigo): JsonResponse
    {
        return $this->ok(
            $this->manuales->publicar($this->escuela($request), $codigo, $this->usuario($request)->codigo),
            'Manual publicado correctamente',
        );
    }

    public function archivar(Request $request, string $codigo): JsonResponse
    {
        return $this->ok($this->manuales->archivar($this->escuela($request), $codigo), 'Manual archivado correctamente');
    }

    /**
     * Sube el PDF a MinIO y devuelve el descriptor que el formulario manda después al crear o
     * editar el manual.
     */
    public function subirArchivo(SubirArchivoManualRequest $request): JsonResponse
    {
        $archivo = $this->archivos->subir($request->file('archivo'), $this->escuela($request));

        $archivo['tamanoBytes'] = $archivo['tamano_bytes'];
        unset($archivo['tamano_bytes']);

        return response()->json(ApiResponse::success($archivo, 'Archivo subido correctamente'), 201);
    }

    private function ok(Manual $manual, string $mensaje): JsonResponse
    {
        return response()->json(ApiResponse::success((new ManualResource($manual))->resolve(), $mensaje));
    }

    /**
     * El JSON va en camelCase y el subdocumento `archivo` se guarda en snake_case (diseño de la colección).
     *
     * @param  array<string,mixed>  $datos
     * @return array<string,mixed>
     */
    private function aInterno(array $datos): array
    {
        if (isset($datos['archivo']['tamanoBytes'])) {
            $datos['archivo']['tamano_bytes'] = $datos['archivo']['tamanoBytes'];
            unset($datos['archivo']['tamanoBytes']);
        }

        return $datos;
    }

    private function usuario(Request $request): UsuarioAutenticado
    {
        return $request->attributes->get('usuario');
    }

    // * Un Administrador siempre trae la escuela en el token (CU02); sin ella no hay tenant que filtrar
    private function escuela(Request $request): string
    {
        $escuela = $this->usuario($request)->escuelaCodigo;
        if ($escuela === null || $escuela === '') {
            throw new ForbiddenException(ErrorCodes::TOKEN_SIN_ESCUELA, 'Tu cuenta no pertenece a ninguna escuela');
        }

        return $escuela;
    }
}
