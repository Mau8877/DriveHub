<?php

namespace App\Common\Security;

/**
 * La persona autenticada, armada desde los claims del JWT (sin llamar a Identidad en cada
 * petición): `sub` es su codigo_usuario, `roles` sus roles y `escuela` el tenant de su cuenta.
 */
final readonly class UsuarioAutenticado
{
    /**
     * @param  array<int,string>  $roles
     */
    public function __construct(
        public string $codigo,
        public array $roles,
        public ?string $escuelaCodigo,
    ) {}

    public function tieneRol(string $rol): bool
    {
        return in_array($rol, $this->roles, true);
    }
}
