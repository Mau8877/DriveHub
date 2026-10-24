<?php

return [

    // * Claves públicas de Identidad (JWKS) para validar el JWT en este servicio (CU02): cada
    // * servicio es su propio resource server y no confía en que la petición venga del gateway.
    'identidad' => [
        'jwks_uri' => env('IDENTIDAD_URL', 'http://localhost:8081').'/.well-known/jwks.json',
        'emisor' => 'drivehub-identidad',
    ],

];
