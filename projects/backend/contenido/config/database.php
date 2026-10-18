<?php

return [

    // * Única base del servicio: MongoDB (base `contenido`). No hay conexión
    // * SQL: ningún microservicio lee la base de otro.
    'default' => env('DB_CONNECTION', 'mongodb'),

    'connections' => [

        'mongodb' => [
            'driver' => 'mongodb',
            // Replica set rs0 en docker-compose. Desde el host, directConnection=true
            // porque el miembro se anuncia con el hostname interno de Docker.
            'dsn' => env('MONGODB_URI', 'mongodb://localhost:27017/contenido?directConnection=true'),
            'database' => env('MONGODB_DATABASE', 'contenido'),
        ],

    ],

    'migrations' => [
        'table' => 'migrations',
        'update_date_on_publish' => true,
    ],

];
