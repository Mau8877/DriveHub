<?php

namespace App\Features\Manuales\Services;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Exceptions\ConflictException;
use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\NotFoundException;
use App\Features\Manuales\Models\Manual;
use App\Features\Manuales\Repositories\ManualRepository;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Support\Facades\Log;

/**
 * Lógica de los manuales de una escuela (CU32). Única capa que loguea y lanza excepciones de
 * negocio. Todo se hace dentro de la escuela que llega de parámetro (la del token).
 *
 * Estados: BORRADOR y ARCHIVADO se publican; solo PUBLICADO se archiva. Un manual archivado no se
 * edita: primero se vuelve a publicar.
 */
class ManualService
{
    private const MODULO = 'manuales';

    public function __construct(
        private readonly ManualRepository $manuales,
        private readonly ArchivoManualService $archivos,
    ) {}

    public function listar(
        string $escuelaCodigo,
        ?string $busqueda,
        ?EstadoPublicacion $estado,
        ?string $categoria,
        int $pagina,
        int $tamano,
    ): LengthAwarePaginator {
        return $this->manuales->listar($escuelaCodigo, $busqueda, $estado, $categoria, $pagina, $tamano);
    }

    public function obtener(string $escuelaCodigo, string $codigo): Manual
    {
        $manual = $this->manuales->buscar($escuelaCodigo, $codigo);
        if ($manual === null) {
            Log::warning('Manual no encontrado', [
                'module' => self::MODULO,
                'errorCode' => ErrorCodes::MANUAL_NO_ENCONTRADO,
                'manualCodigo' => $codigo,
            ]);
            throw new NotFoundException(ErrorCodes::MANUAL_NO_ENCONTRADO, 'El manual no existe');
        }

        return $manual;
    }

    /**
     * Crea el manual en BORRADOR: se publica aparte, cuando está listo.
     *
     * @param  array<string,mixed>  $datos
     */
    public function crear(string $escuelaCodigo, array $datos): Manual
    {
        $this->archivos->validarDescriptor($datos['archivo'], $escuelaCodigo);

        $manual = $this->manuales->crear([
            'escuela_codigo' => $escuelaCodigo,
            'titulo' => trim($datos['titulo']),
            'descripcion' => $this->limpiar($datos['descripcion'] ?? null),
            'categoria' => $datos['categoria'] ?? 'GENERAL',
            'tema' => $this->limpiar($datos['tema'] ?? null),
            'version' => 1,
            'archivo' => $datos['archivo'],
            'estado' => EstadoPublicacion::BORRADOR,
            'etiquetas' => $this->etiquetas($datos['etiquetas'] ?? []),
        ]);

        Log::info('Manual creado', [
            'module' => self::MODULO,
            'manualCodigo' => $manual->codigo_manual,
            'escuelaCodigo' => $escuelaCodigo,
        ]);

        return $manual;
    }

    /**
     * Edita solo lo que llega. Un archivo nuevo reemplaza al anterior y sube la versión.
     *
     * @param  array<string,mixed>  $datos
     */
    public function editar(string $escuelaCodigo, string $codigo, array $datos): Manual
    {
        $manual = $this->obtener($escuelaCodigo, $codigo);
        if ($manual->estado === EstadoPublicacion::ARCHIVADO) {
            $this->rechazarTransicion($codigo, 'Un manual archivado no se edita: publícalo de nuevo primero');
        }

        foreach (['titulo', 'categoria'] as $campo) {
            if (isset($datos[$campo])) {
                $manual->{$campo} = trim($datos[$campo]);
            }
        }
        foreach (['descripcion', 'tema'] as $campo) {
            if (array_key_exists($campo, $datos)) {
                $manual->{$campo} = $this->limpiar($datos[$campo]);
            }
        }
        if (array_key_exists('etiquetas', $datos)) {
            $manual->etiquetas = $this->etiquetas($datos['etiquetas'] ?? []);
        }
        if (isset($datos['archivo']) && ($datos['archivo']['url'] ?? null) !== ($manual->archivo['url'] ?? null)) {
            $this->archivos->validarDescriptor($datos['archivo'], $escuelaCodigo);
            $manual->archivo = $datos['archivo'];
            $manual->version = $manual->version + 1;
        }
        $this->manuales->guardar($manual);

        Log::info('Manual editado', ['module' => self::MODULO, 'manualCodigo' => $codigo, 'version' => $manual->version]);

        return $manual;
    }

    public function publicar(string $escuelaCodigo, string $codigo, string $usuarioCodigo): Manual
    {
        $manual = $this->obtener($escuelaCodigo, $codigo);
        if ($manual->estado === EstadoPublicacion::PUBLICADO) {
            $this->rechazarTransicion($codigo, 'El manual ya está publicado');
        }

        $manual->estado = EstadoPublicacion::PUBLICADO;
        $manual->publicado_por_codigo = $usuarioCodigo;
        $this->manuales->guardar($manual);

        Log::info('Manual publicado', ['module' => self::MODULO, 'manualCodigo' => $codigo]);

        return $manual;
    }

    public function archivar(string $escuelaCodigo, string $codigo): Manual
    {
        $manual = $this->obtener($escuelaCodigo, $codigo);
        if ($manual->estado !== EstadoPublicacion::PUBLICADO) {
            $this->rechazarTransicion($codigo, 'Solo un manual publicado se puede archivar');
        }

        $manual->estado = EstadoPublicacion::ARCHIVADO;
        $this->manuales->guardar($manual);

        Log::info('Manual archivado', ['module' => self::MODULO, 'manualCodigo' => $codigo]);

        return $manual;
    }

    private function rechazarTransicion(string $codigo, string $mensaje): never
    {
        Log::warning('Cambio de manual rechazado: estado no permitido', [
            'module' => self::MODULO,
            'errorCode' => ErrorCodes::TRANSICION_MANUAL_INVALIDA,
            'manualCodigo' => $codigo,
        ]);
        throw new ConflictException(ErrorCodes::TRANSICION_MANUAL_INVALIDA, $mensaje);
    }

    private function limpiar(?string $texto): ?string
    {
        $texto = $texto === null ? null : trim($texto);

        return $texto === '' ? null : $texto;
    }

    /**
     * @param  array<int,string>  $etiquetas
     * @return array<int,string>
     */
    private function etiquetas(array $etiquetas): array
    {
        return array_values(array_unique(array_filter(array_map(fn ($e) => trim((string) $e), $etiquetas))));
    }
}
