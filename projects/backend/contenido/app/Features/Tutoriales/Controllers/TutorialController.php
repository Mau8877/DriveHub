<?php

namespace App\Features\Tutoriales\Controllers;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Responses\ApiResponse;
use App\Features\Tutoriales\Enums\TipoRecurso;
use App\Features\Tutoriales\Models\Tutorial;
use App\Features\Tutoriales\Requests\CrearTutorialRequest;
use App\Features\Tutoriales\Requests\EditarTutorialRequest;
use App\Features\Tutoriales\Requests\ListarTutorialesRequest;
use App\Features\Tutoriales\Requests\SubirArchivoTutorialRequest;
use App\Features\Tutoriales\Resources\TutorialResource;
use App\Features\Tutoriales\Services\RecursoTutorialService;
use App\Features\Tutoriales\Services\TutorialService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Gestión de los tutoriales del instructor (CU33): solo el Instructor (middleware `jwt` + `rol`, ver
 * routes/api.php) y solo los suyos. Controller delgado: valida con el FormRequest, llama a un método
 * del service y envuelve en el sobre estándar. La escuela y el autor salen del token.
 */
class TutorialController
{
    use IdentificaAlSolicitante;

    public function __construct(
        private readonly TutorialService $tutoriales,
        private readonly RecursoTutorialService $recursos,
    ) {}

    public function index(ListarTutorialesRequest $request): JsonResponse
    {
        $filtro = (array) $request->query('filter', []);

        return $this->lista($this->tutoriales->listar(
            $this->escuela($request),
            $this->usuario($request)->codigo,
            $request->query('search'),
            isset($filtro['estado']) ? EstadoPublicacion::from($filtro['estado']) : null,
            isset($filtro['tipo']) ? TipoRecurso::from($filtro['tipo']) : null,
            (int) $request->query('page', 1),
            (int) $request->query('pageSize', 20),
        ));
    }

    public function show(Request $request, string $codigo): JsonResponse
    {
        return $this->ok(
            $this->tutoriales->obtener($this->escuela($request), $this->usuario($request)->codigo, $codigo),
            'Tutorial obtenido correctamente',
        );
    }

    public function store(CrearTutorialRequest $request): JsonResponse
    {
        $tutorial = $this->tutoriales->crear(
            $this->escuela($request),
            $this->usuario($request)->codigo,
            $this->aInterno($request->validated()),
        );

        return response()->json(
            ApiResponse::success((new TutorialResource($tutorial))->resolve(), 'Tutorial creado correctamente'),
            201,
        );
    }

    public function update(EditarTutorialRequest $request, string $codigo): JsonResponse
    {
        return $this->ok(
            $this->tutoriales->editar(
                $this->escuela($request),
                $this->usuario($request)->codigo,
                $codigo,
                $this->aInterno($request->validated()),
            ),
            'Tutorial actualizado correctamente',
        );
    }

    public function publicar(Request $request, string $codigo): JsonResponse
    {
        return $this->ok(
            $this->tutoriales->publicar($this->escuela($request), $this->usuario($request)->codigo, $codigo),
            'Tutorial publicado correctamente',
        );
    }

    public function archivar(Request $request, string $codigo): JsonResponse
    {
        return $this->ok(
            $this->tutoriales->archivar($this->escuela($request), $this->usuario($request)->codigo, $codigo),
            'Tutorial archivado correctamente',
        );
    }

    /**
     * Sube el PDF a MinIO y devuelve el descriptor que el formulario manda después al crear o
     * editar el tutorial.
     */
    public function subirArchivo(SubirArchivoTutorialRequest $request): JsonResponse
    {
        $archivo = $this->recursos->subirPdf($request->file('archivo'), $this->escuela($request));

        $archivo['tamanoBytes'] = $archivo['tamano_bytes'];
        unset($archivo['tamano_bytes']);

        return response()->json(ApiResponse::success($archivo, 'Archivo subido correctamente'), 201);
    }

    private function ok(Tutorial $tutorial, string $mensaje): JsonResponse
    {
        return response()->json(ApiResponse::success((new TutorialResource($tutorial))->resolve(), $mensaje));
    }

    /**
     * El JSON va en camelCase y el subdocumento `recurso` se guarda en snake_case (diseño de la colección).
     *
     * @param  array<string,mixed>  $datos
     * @return array<string,mixed>
     */
    private function aInterno(array $datos): array
    {
        if (isset($datos['recurso']['tamanoBytes'])) {
            $datos['recurso']['tamano_bytes'] = $datos['recurso']['tamanoBytes'];
            unset($datos['recurso']['tamanoBytes']);
        }

        return $datos;
    }
}
