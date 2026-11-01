<?php

namespace App\Features\Tutoriales\Repositories;

use App\Common\Enums\EstadoPublicacion;
use App\Features\Tutoriales\Enums\Audiencia;
use App\Features\Tutoriales\Enums\TipoRecurso;
use App\Features\Tutoriales\Models\Tutorial;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Database\Eloquent\Builder;
use MongoDB\BSON\Regex;

/**
 * Acceso a la colección `tutoriales`, siempre dentro de una escuela: un tutorial de otra escuela
 * es, para quien consulta, un tutorial que no existe. Sin lógica de negocio.
 */
class TutorialRepository
{
    /**
     * Los tutoriales de un instructor, del más reciente al más antiguo.
     */
    public function listarDelAutor(
        string $escuelaCodigo,
        string $autorCodigo,
        ?string $busqueda,
        ?EstadoPublicacion $estado,
        ?TipoRecurso $tipo,
        int $pagina,
        int $tamano,
    ): LengthAwarePaginator {
        $consulta = Tutorial::query()->where('escuela_codigo', $escuelaCodigo)->where('autor_codigo', $autorCodigo);
        if ($estado !== null) {
            $consulta->where('estado', $estado->value);
        }

        return $this->filtrar($consulta, $busqueda, null, $tipo)->orderByDesc('created_at')->orderByDesc('id')
            ->paginate($tamano, ['*'], 'page', $pagina);
    }

    /**
     * Los tutoriales publicados de la escuela dirigidos a una audiencia.
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
        $consulta = Tutorial::query()
            ->where('escuela_codigo', $escuelaCodigo)
            ->where('estado', EstadoPublicacion::PUBLICADO->value)
            ->where('audiencia', $audiencia->value);

        return $this->filtrar($consulta, $busqueda, $categoria, $tipo)->orderByDesc('created_at')->orderByDesc('id')
            ->paginate($tamano, ['*'], 'page', $pagina);
    }

    public function buscar(string $escuelaCodigo, string $codigo): ?Tutorial
    {
        return Tutorial::query()
            ->where('escuela_codigo', $escuelaCodigo)
            ->where('codigo_tutorial', $codigo)
            ->first();
    }

    /**
     * @param  array<string,mixed>  $datos
     */
    public function crear(array $datos): Tutorial
    {
        return Tutorial::create($datos);
    }

    public function guardar(Tutorial $tutorial): void
    {
        $tutorial->save();
    }

    /**
     * `$busqueda` se busca en título, descripción y etiquetas sin distinguir mayúsculas y sin
     * interpretar comodines: la persona busca texto, no escribe patrones.
     */
    private function filtrar(Builder $consulta, ?string $busqueda, ?string $categoria, ?TipoRecurso $tipo): Builder
    {
        if ($categoria !== null && $categoria !== '') {
            $consulta->where('categoria', $categoria);
        }
        if ($tipo !== null) {
            $consulta->where('tipo', $tipo->value);
        }
        if ($busqueda !== null && trim($busqueda) !== '') {
            $patron = new Regex(preg_quote(trim($busqueda), '/'), 'i');
            $consulta->where(fn ($q) => $q->where('titulo', $patron)
                ->orWhere('descripcion', $patron)
                ->orWhere('etiquetas', $patron));
        }

        return $consulta;
    }
}
