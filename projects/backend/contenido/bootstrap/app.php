<?php

use App\Common\Exceptions\BusinessException;
use App\Common\Responses\ApiResponse;
use App\Common\Security\AutenticarJwt;
use App\Common\Security\ExigirRol;
use App\Health\HealthController;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Support\Facades\Route;
use Illuminate\Validation\ValidationException as LaravelValidationException;
use Symfony\Component\HttpKernel\Exception\HttpExceptionInterface;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        // Rutas de negocio bajo /api/v1, igual que el resto de los servicios.
        api: __DIR__.'/../routes/api.php',
        apiPrefix: 'api/v1',
        commands: __DIR__.'/../routes/console.php',
        then: function () {
            // /health fuera de /api/v1: es infraestructura, no API de negocio.
            Route::get('/health', HealthController::class);
        },
    )
    ->withMiddleware(function (Middleware $middleware): void {
        // * Autenticación y rol declarados en las rutas (routes/api.php)
        $middleware->alias([
            'jwt' => AutenticarJwt::class,
            'rol' => ExigirRol::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        // * Traduce toda excepción al sobre estándar con su código catalogado
        // * (equivalente al GlobalExceptionHandler de los servicios Spring).
        // * BusinessException no se reporta al log acá: ya la logueó el
        // * service que la lanzó (un evento se loguea una sola vez). El resto
        // * de las excepciones Laravel las sigue reportando al log como ERROR.
        $exceptions->dontReport(BusinessException::class);

        $exceptions->render(fn (BusinessException $e) => response()->json(
            ApiResponse::error($e->errorCode, $e->getMessage()),
            $e->httpStatus,
        ));

        $exceptions->render(fn (LaravelValidationException $e) => response()->json(
            ApiResponse::error('ERR_SYS_01', $e->validator->errors()->first() ?: 'Los datos enviados no son válidos'),
            400,
        ));

        $exceptions->render(fn (NotFoundHttpException $e) => response()->json(
            ApiResponse::error('ERR_SYS_03', 'El recurso solicitado no existe'),
            404,
        ));

        // Otros errores HTTP del framework (ej. 405) conservan su status.
        $exceptions->render(fn (Throwable $e) => response()->json(
            ApiResponse::error('ERR_SYS_00', 'Ocurrió un error inesperado'),
            $e instanceof HttpExceptionInterface ? $e->getStatusCode() : 500,
        ));
    })->create();
