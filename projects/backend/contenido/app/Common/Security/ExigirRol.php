<?php

namespace App\Common\Security;

use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\ForbiddenException;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Exige que la persona tenga alguno de los roles indicados: `rol:ADMIN_ESCUELA`. Va después de
 * `jwt`. La decisión por rol vive acá, declarada en la ruta, y no repartida en los controllers.
 */
class ExigirRol
{
    public function handle(Request $request, Closure $next, string ...$roles): Response
    {
        /** @var UsuarioAutenticado|null $usuario */
        $usuario = $request->attributes->get('usuario');

        $permitido = $usuario !== null && array_filter($roles, fn (string $rol) => $usuario->tieneRol($rol)) !== [];
        if (! $permitido) {
            throw new ForbiddenException(ErrorCodes::SIN_PERMISO, 'No tienes permiso para realizar esta acción');
        }

        return $next($request);
    }
}
