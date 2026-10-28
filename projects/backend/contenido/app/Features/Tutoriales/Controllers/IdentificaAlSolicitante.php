<?php

namespace App\Features\Tutoriales\Controllers;

use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\ForbiddenException;
use App\Common\Responses\ApiResponse;
use App\Common\Security\UsuarioAutenticado;
use App\Features\Tutoriales\Resources\TutorialResource;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Lo que comparten los controllers de tutoriales: quién pide (sale del token, puesto por el
 * middleware `jwt`) y la forma de un listado paginado.
 */
trait IdentificaAlSolicitante
{
    private function usuario(Request $request): UsuarioAutenticado
    {
        return $request->attributes->get('usuario');
    }

    // * Instructor y Estudiante siempre traen la escuela en el token (CU02); sin ella no hay tenant que filtrar
    private function escuela(Request $request): string
    {
        $escuela = $this->usuario($request)->escuelaCodigo;
        if ($escuela === null || $escuela === '') {
            throw new ForbiddenException(ErrorCodes::TOKEN_SIN_ESCUELA, 'Tu cuenta no pertenece a ninguna escuela');
        }

        return $escuela;
    }

    private function lista(LengthAwarePaginator $resultado): JsonResponse
    {
        return response()->json(ApiResponse::successList(
            TutorialResource::collection($resultado->items())->resolve(),
            'Tutoriales obtenidos correctamente',
            [
                'page' => $resultado->currentPage(),
                'pageSize' => $resultado->perPage(),
                'totalItems' => $resultado->total(),
                'totalPages' => $resultado->lastPage(),
            ],
        ));
    }
}
