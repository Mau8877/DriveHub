<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\Schema;
use MongoDB\Laravel\Schema\Blueprint;

/**
 * Colecciones e índices iniciales del microservicio Contenido.
 * Fuente: docs/DriveHub/DriveHub_Diseno_Base_de_Datos.pdf (sección 5).
 *
 * * MongoDB no necesita declarar campos: la migración solo crea la colección
 * * y sus índices (el equivalente a los CREATE INDEX de Flyway).
 */
return new class extends Migration
{
    protected $connection = 'mongodb';

    public function up(): void
    {
        // Manuales de la escuela (CU32, CU34).
        Schema::create('manuales', function (Blueprint $collection) {
            $collection->unique('codigo_manual', 'uq_manuales_codigo');
            $collection->index(['estado', 'categoria'], 'idx_manuales_estado_categoria');
            $collection->index(
                ['titulo' => 'text', 'descripcion' => 'text', 'etiquetas' => 'text'],
                'idx_manuales_texto',
            );
        });

        // Tutoriales de los instructores (CU33, CU35).
        Schema::create('tutoriales', function (Blueprint $collection) {
            $collection->unique('codigo_tutorial', 'uq_tutoriales_codigo');
            $collection->index(['autor_codigo', 'estado'], 'idx_tutoriales_autor_estado');
            $collection->index(
                ['titulo' => 'text', 'descripcion' => 'text', 'etiquetas' => 'text'],
                'idx_tutoriales_texto',
            );
        });
    }

    public function down(): void
    {
        Schema::drop('tutoriales');
        Schema::drop('manuales');
    }
};
