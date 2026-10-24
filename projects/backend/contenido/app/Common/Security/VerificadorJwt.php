<?php

namespace App\Common\Security;

use Firebase\JWT\JWK;
use Firebase\JWT\JWT;
use Illuminate\Support\Facades\Http;
use Throwable;

/**
 * Valida el access token (RS256) con las claves públicas de Identidad (JWKS) y comprueba el
 * emisor. Devuelve null ante cualquier problema (firma, vencido, otro emisor, JWKS caído): el
 * motivo no se le cuenta al cliente.
 *
 * ! Las claves se piden en cada petición: la caché del servicio es `array` (API sin estado) y
 * ! `artisan serve` no conserva memoria entre peticiones. Es una llamada interna corta.
 * ! ponytail: si pesa, cachear el JWKS en Redis o en un archivo con TTL.
 */
class VerificadorJwt
{
    public function verificar(string $token): ?UsuarioAutenticado
    {
        try {
            $jwks = Http::timeout(3)->get(config('services.identidad.jwks_uri'))->throw()->json();
            $claims = JWT::decode($token, JWK::parseKeySet($jwks, 'RS256'));
        } catch (Throwable) {
            return null;
        }

        if (($claims->iss ?? null) !== config('services.identidad.emisor') || empty($claims->sub)) {
            return null;
        }

        return new UsuarioAutenticado(
            codigo: (string) $claims->sub,
            roles: array_values((array) ($claims->roles ?? [])),
            escuelaCodigo: isset($claims->escuela) ? (string) $claims->escuela : null,
        );
    }
}
