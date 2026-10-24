<?php

namespace App\Features\Manuales\Repositories;

use App\Common\Enums\EstadoPublicacion;
use App\Features\Manuales\Models\Manual;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use MongoDB\BSON\Regex;

/**
 * Acceso a la colección `manuales`, siempre dentro de una escuela: un manual de otra escuela es,
 * para quien consulta, un manual que no existe. Sin lógica de negocio.
 */
class ManualRepository
{
    /**
     * Listado de la escuela, del más reciente al más antiguo.
     *
     * `$busqueda` se busca en título, descripción y etiquetas sin distinguir mayúsculas y sin
     * interpretar comodines: la persona busca texto, no escribe patrones.
     */
    public function listar(
        string $escuelaCodigo,
        ?string $busqueda,
        ?EstadoPublicacion $estado,
        ?string $categoria,
        int $pagina,
        int $tamano,
    ): LengthAwarePaginator {
        $consulta = Manual::query()->where('escuela_codigo', $escuelaCodigo);

        if ($estado !== null) {
            $consulta->where('estado', $estado->value);
        }
        if ($categoria !== null && $categoria !== '') {
            $consulta->where('categoria', $categoria);
        }
        if ($busqueda !== null && trim($busqueda) !== '') {
            $patron = new Regex(preg_quote(trim($busqueda), '/'), 'i');
            $consulta->where(fn ($q) => $q->where('titulo', $patron)
                ->orWhere('descripcion', $patron)
                ->orWhere('etiquetas', $patron));
        }

        return $consulta->orderByDesc('created_at')->orderByDesc('id')->paginate($tamano, ['*'], 'page', $pagina);
    }

    public function buscar(string $escuelaCodigo, string $codigo): ?Manual
    {
        return Manual::query()
            ->where('escuela_codigo', $escuelaCodigo)
            ->where('codigo_manual', $codigo)
            ->first();
    }

    /**
     * @param  array<string,mixed>  $datos
     */
    public function crear(array $datos): Manual
    {
        return Manual::create($datos);
    }

    public function guardar(Manual $manual): void
    {
        $manual->save();
    }
}
