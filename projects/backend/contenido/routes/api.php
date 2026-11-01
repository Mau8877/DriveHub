<?php

use App\Features\Manuales\Controllers\ManualConsultaController;
use App\Features\Manuales\Controllers\ManualController;
use App\Features\Tutoriales\Controllers\TutorialConsultaController;
use App\Features\Tutoriales\Controllers\TutorialController;
use Illuminate\Support\Facades\Route;

// Rutas de negocio del servicio, montadas bajo /api/v1 (ver bootstrap/app.php).
// Recursos de este servicio: /manuales y /tutoriales. Cada feature registra
// acá sus rutas cuando tenga su primer caso de uso (CU32 a CU35).

// Consulta de manuales (CU34): solo el Estudiante, y solo los publicados de su escuela.
// * Antes del grupo del Administrador: "publicados" no es un codigo de manual
Route::middleware(['jwt', 'rol:ESTUDIANTE'])->prefix('manuales/publicados')->group(function () {
    Route::get('/', [ManualConsultaController::class, 'index']);
    Route::get('/{codigo}', [ManualConsultaController::class, 'show']);
});

// Gestión de manuales (CU32): solo el Administrador de escuela.
Route::middleware(['jwt', 'rol:ADMIN_ESCUELA'])->prefix('manuales')->group(function () {
    Route::get('/', [ManualController::class, 'index']);
    Route::post('/', [ManualController::class, 'store']);
    // * Antes de /{codigo}: "archivos" no es un codigo de manual
    Route::post('/archivos', [ManualController::class, 'subirArchivo']);
    Route::get('/{codigo}', [ManualController::class, 'show']);
    Route::patch('/{codigo}', [ManualController::class, 'update']);
    Route::post('/{codigo}/publicar', [ManualController::class, 'publicar']);
    Route::post('/{codigo}/archivar', [ManualController::class, 'archivar']);
});

// Consulta de tutoriales (CU35): Estudiante e Instructor, solo los publicados de su escuela y de su audiencia.
// * Antes del grupo del Instructor: "publicados" no es un codigo de tutorial
Route::middleware(['jwt', 'rol:ESTUDIANTE,INSTRUCTOR'])->prefix('tutoriales/publicados')->group(function () {
    Route::get('/', [TutorialConsultaController::class, 'index']);
    Route::get('/{codigo}', [TutorialConsultaController::class, 'show']);
});

// Gestión de tutoriales (CU33): solo el Instructor, y solo los suyos.
Route::middleware(['jwt', 'rol:INSTRUCTOR'])->prefix('tutoriales')->group(function () {
    Route::get('/', [TutorialController::class, 'index']);
    Route::post('/', [TutorialController::class, 'store']);
    // * Antes de /{codigo}: "archivos" no es un codigo de tutorial
    Route::post('/archivos', [TutorialController::class, 'subirArchivo']);
    Route::get('/{codigo}', [TutorialController::class, 'show']);
    Route::patch('/{codigo}', [TutorialController::class, 'update']);
    Route::post('/{codigo}/publicar', [TutorialController::class, 'publicar']);
    Route::post('/{codigo}/archivar', [TutorialController::class, 'archivar']);
});
