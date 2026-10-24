<?php

namespace App\Common\Security;

use App\Common\Exceptions\ErrorCodes;
use App\Common\Exceptions\UnauthorizedException;
use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * Exige un access token válido y deja a la persona en `$request->attributes['usuario']`.
 * Alias `jwt` (ver bootstrap/app.php). Sin token o con uno inválido: 401 ERR_SYS_04.
 */
class AutenticarJwt
{
    public function __construct(private readonly VerificadorJwt $verificador) {}

    public function handle(Request $request, Closure $next): Response
    {
        $token = $request->bearerToken();
        $usuario = $token ? $this->verificador->verificar($token) : null;

        if ($usuario === null) {
            throw new UnauthorizedException(ErrorCodes::NO_AUTENTICADO, 'Tu sesión no es válida o venció');
        }

        $request->attributes->set('usuario', $usuario);

        return $next($request);
    }
}
