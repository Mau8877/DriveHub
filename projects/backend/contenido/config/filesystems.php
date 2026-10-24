<?php

return [

    'default' => 'contenido',

    'disks' => [

        // * Bucket drivehub-contenido de MinIO (S3 compatible): PDF de manuales y archivos de tutoriales.
        // * `endpoint` es la dirección INTERNA de Docker (la usa este servicio para subir);
        // * `public_url` es la que ve el navegador para abrir el archivo: el bucket es de lectura
        // * pública (ver infrastructure/minio/create-buckets.sh).
        'contenido' => [
            'driver' => 's3',
            'key' => env('MINIO_ROOT_USER'),
            'secret' => env('MINIO_ROOT_PASSWORD'),
            'region' => 'us-east-1',
            'bucket' => env('MINIO_BUCKET', 'drivehub-contenido'),
            'endpoint' => env('MINIO_URL', 'http://localhost:9000'),
            'use_path_style_endpoint' => true,
            'throw' => true,
            'public_url' => env('MINIO_PUBLIC_URL', 'http://localhost:9000'),
        ],

    ],

];
