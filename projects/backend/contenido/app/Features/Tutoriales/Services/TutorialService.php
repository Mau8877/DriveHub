<?php

namespace App\Features\Tutoriales\Services;

use App\Common\Enums\EstadoPublicacion;
use App\Common\Exceptions\ConflictException;
use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\NotFoundException;
use App\Features\Tutoriales\Enums\Audiencia;
use App\Features\Tutoriales\Enums\TipoRecurso;
use App\Features\Tutoriales\Models\Tutorial;
use App\Features\Tutoriales\Repositories\TutorialRepository;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Support\Facades\Log;

/**
 * Lógica de los tutoriales (CU33 y CU35). Única capa que loguea y lanza excepciones de negocio.
 *
 * * El instructor gestiona solo los suyos (escuela y autor salen del token). BORRADOR y ARCHIVADO se
 * * publican; solo PUBLICADO se archiva; un tutorial archivado no se edita.
 * * Los publicados los consulta quien está en su audiencia, dentro de su escuela.
 */
class TutorialService
{
    private const MODULO = 'tutoriales';

    public function __construct(
        private readonly TutorialRepository $tutoriales,
        private readonly RecursoTutorialService $recursos,
    ) {}

    public function listar(
        string $escuelaCodigo,
        string $autorCodigo,
        ?string $busqueda,
        ?EstadoPublicacion $estado,
        ?TipoRecurso $tipo,
        int $pagina,
        int $tamano,
    ): LengthAwarePaginator {
        return $this->tutoriales->listarDelAutor($escuelaCodigo, $autorCodigo, $busqueda, $estado, $tipo, $pagina, $tamano);
    }

    // * Un tutorial de otro instructor es, para quien lo pide, un tutorial que no existe
    public function obtener(string $escuelaCodigo, string $autorCodigo, string $codigo): Tutorial
    {
        $tutorial = $this->tutoriales->buscar($escuelaCodigo, $codigo);
        if ($tutorial === null || $tutorial->autor_codigo !== $autorCodigo) {
            $this->noEncontrado($codigo);
        }

        return $tutorial;
    }

    /**
     * Crea el tutorial en BORRADOR: se publica aparte, cuando está listo.
     *
     * @param  array<string,mixed>  $datos
     */
    public function crear(string $escuelaCodigo, string $autorCodigo, array $datos): Tutorial
    {
        $tipo = TipoRecurso::from($datos['tipo']);
        $recurso = $this->recursos->validar($tipo, $datos['recurso'], $escuelaCodigo);

        $tutorial = $this->tutoriales->crear([
            'escuela_codigo' => $escuelaCodigo,
            'autor_codigo' => $autorCodigo,
            'titulo' => trim($datos['titulo']),
            'descripcion' => $this->limpiar($datos['descripcion'] ?? null),
            'categoria' => $datos['categoria'] ?? 'GENERAL',
            'tipo' => $tipo,
            'recurso' => $recurso,
            'audiencia' => $this->audiencia($datos['audiencia']),
            'estado' => EstadoPublicacion::BORRADOR,
            'etiquetas' => $this->etiquetas($datos['etiquetas'] ?? []),
        ]);

        Log::info('Tutorial creado', [
            'module' => self::MODULO,
            'tutorialCodigo' => $tutorial->codigo_tutorial,
            'escuelaCodigo' => $escuelaCodigo,
        ]);

        return $tutorial;
    }

    /**
     * Edita solo lo que llega. Un tipo y un recurso nuevos reemplazan a los anteriores.
     *
     * @param  array<string,mixed>  $datos
     */
    public function editar(string $escuelaCodigo, string $autorCodigo, string $codigo, array $datos): Tutorial
    {
        $tutorial = $this->obtener($escuelaCodigo, $autorCodigo, $codigo);
        if ($tutorial->estado === EstadoPublicacion::ARCHIVADO) {
            $this->rechazarTransicion($codigo, 'Un tutorial archivado no se edita: publícalo de nuevo primero');
        }

        foreach (['titulo', 'categoria'] as $campo) {
            if (isset($datos[$campo])) {
                $tutorial->{$campo} = trim($datos[$campo]);
            }
        }
        if (array_key_exists('descripcion', $datos)) {
            $tutorial->descripcion = $this->limpiar($datos['descripcion']);
        }
        if (array_key_exists('etiquetas', $datos)) {
            $tutorial->etiquetas = $this->etiquetas($datos['etiquetas'] ?? []);
        }
        if (isset($datos['audiencia'])) {
            $tutorial->audiencia = $this->audiencia($datos['audiencia']);
        }
        if (isset($datos['tipo'], $datos['recurso'])) {
            $tipo = TipoRecurso::from($datos['tipo']);
            $tutorial->recurso = $this->recursos->validar($tipo, $datos['recurso'], $escuelaCodigo);
            $tutorial->tipo = $tipo;
        }
        $this->tutoriales->guardar($tutorial);

        Log::info('Tutorial editado', ['module' => self::MODULO, 'tutorialCodigo' => $codigo]);

        return $tutorial;
    }

    public function publicar(string $escuelaCodigo, string $autorCodigo, string $codigo): Tutorial
    {
        $tutorial = $this->obtener($escuelaCodigo, $autorCodigo, $codigo);
        if ($tutorial->estado === EstadoPublicacion::PUBLICADO) {
            $this->rechazarTransicion($codigo, 'El tutorial ya está publicado');
        }

        $tutorial->estado = EstadoPublicacion::PUBLICADO;
        $this->tutoriales->guardar($tutorial);

        Log::info('Tutorial publicado', ['module' => self::MODULO, 'tutorialCodigo' => $codigo]);

        return $tutorial;
    }

    public function archivar(string $escuelaCodigo, string $autorCodigo, string $codigo): Tutorial
    {
        $tutorial = $this->obtener($escuelaCodigo, $autorCodigo, $codigo);
        if ($tutorial->estado !== EstadoPublicacion::PUBLICADO) {
            $this->rechazarTransicion($codigo, 'Solo un tutorial publicado se puede archivar');
        }

        $tutorial->estado = EstadoPublicacion::ARCHIVADO;
        $this->tutoriales->guardar($tutorial);

        Log::info('Tutorial archivado', ['module' => self::MODULO, 'tutorialCodigo' => $codigo]);

        return $tutorial;
    }

    /**
     * Lo que ve quien consulta (CU35): los publicados de su escuela dirigidos a su audiencia.
     */
    public function listarPublicados(
        string $escuelaCodigo,
        Audiencia $audiencia,
        ?string $busqueda,
        ?string $categoria,
        ?TipoRecurso $tipo,
        int $pagina,
        int $tamano,
    ): LengthAwarePaginator {
        return $this->tutoriales->listarPublicados($escuelaCodigo, $audiencia, $busqueda, $categoria, $tipo, $pagina, $tamano);
    }

    // * Un tutorial no publicado, o dirigido a otra audiencia, es un tutorial que no existe
    public function obtenerPublicado(string $escuelaCodigo, Audiencia $audiencia, string $codigo): Tutorial
    {
        $tutorial = $this->tutoriales->buscar($escuelaCodigo, $codigo);
        if (
            $tutorial === null
            || $tutorial->estado !== EstadoPublicacion::PUBLICADO
            || ! in_array($audiencia->value, $tutorial->audiencia ?? [], true)
        ) {
            $this->noEncontrado($codigo);
        }

        return $tutorial;
    }

    private function noEncontrado(string $codigo): never
    {
        Log::warning('Tutorial no encontrado', [
            'module' => self::MODULO,
            'errorCode' => ErrorCodes::TUTORIAL_NO_ENCONTRADO,
            'tutorialCodigo' => $codigo,
        ]);
        throw new NotFoundException(ErrorCodes::TUTORIAL_NO_ENCONTRADO, 'El tutorial no existe');
    }

    private function rechazarTransicion(string $codigo, string $mensaje): never
    {
        Log::warning('Cambio de tutorial rechazado: estado no permitido', [
            'module' => self::MODULO,
            'errorCode' => ErrorCodes::TRANSICION_TUTORIAL_INVALIDA,
            'tutorialCodigo' => $codigo,
        ]);
        throw new ConflictException(ErrorCodes::TRANSICION_TUTORIAL_INVALIDA, $mensaje);
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

    /**
     * @param  array<int,string>  $audiencia
     * @return array<int,string>
     */
    private function audiencia(array $audiencia): array
    {
        return array_values(array_unique($audiencia));
    }
}
