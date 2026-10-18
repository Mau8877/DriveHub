<?php

use Monolog\Formatter\JsonFormatter;
use Monolog\Handler\NullHandler;
use Monolog\Handler\StreamHandler;

return [

    // * Logs en JSON estructurado a stderr, una línea por evento (mismo
    // * criterio que logstash-logback-encoder en los servicios Spring, ver
    // * LOGGING_BACKEND.md). Campos contextuales: Log::info('...', ['module' => 'manuales']).
    'default' => env('LOG_CHANNEL', 'json'),

    'deprecations' => [
        'channel' => env('LOG_DEPRECATIONS_CHANNEL', 'null'),
        'trace' => false,
    ],

    'channels' => [

        'json' => [
            'driver' => 'monolog',
            'level' => env('LOG_LEVEL', 'info'),
            'handler' => StreamHandler::class,
            'handler_with' => [
                'stream' => 'php://stderr',
            ],
            'formatter' => JsonFormatter::class,
        ],

        'null' => [
            'driver' => 'monolog',
            'handler' => NullHandler::class,
        ],

    ],

];
