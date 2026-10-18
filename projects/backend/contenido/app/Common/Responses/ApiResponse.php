<?php

namespace App\Common\Responses;

/**
 * Sobre estándar de respuesta de toda la API (mismo contrato que ApiResponse
 * de los servicios Spring, ver .claude/rules/backend/RESPONSES_BACKEND.md).
 *
 * * `error` y `meta` no se serializan cuando no aplican.
 */
final class ApiResponse
{
    public static function success(mixed $data, string $message): array
    {
        return [
            'status' => 'success',
            'data' => $data,
            'message' => $message,
            'timestamp' => self::ahora(),
        ];
    }

    /**
     * @param  array{page:int,pageSize:int,totalItems:int,totalPages:int}  $meta
     */
    public static function successList(array $data, string $message, array $meta): array
    {
        return [
            'status' => 'success',
            'data' => $data,
            'message' => $message,
            'timestamp' => self::ahora(),
            'meta' => $meta,
        ];
    }

    public static function error(string $errorCode, string $message): array
    {
        return [
            'status' => 'failed',
            'data' => null,
            'message' => $message,
            'timestamp' => self::ahora(),
            'error' => $errorCode,
        ];
    }

    private static function ahora(): string
    {
        return now('UTC')->format('Y-m-d\TH:i:s.v\Z');
    }
}
