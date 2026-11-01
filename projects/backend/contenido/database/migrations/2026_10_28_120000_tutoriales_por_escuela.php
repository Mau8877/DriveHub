<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\Schema;
use MongoDB\Laravel\Schema\Blueprint;

/**
 * CU33 y CU35: cada tutorial pertenece a una escuela (tenant). El instructor lista los suyos por
 * escuela, autor y estado; el estudiante y el instructor consultan los publicados de su escuela
 * por audiencia.
 */
return new class extends Migration
{
    protected $connection = 'mongodb';

    public function up(): void
    {
        Schema::table('tutoriales', function (Blueprint $collection) {
            $collection->index(['escuela_codigo', 'autor_codigo', 'estado'], 'idx_tutoriales_escuela_autor_estado');
            $collection->index(['escuela_codigo', 'estado', 'audiencia'], 'idx_tutoriales_escuela_estado_audiencia');
        });
    }

    public function down(): void
    {
        Schema::table('tutoriales', function (Blueprint $collection) {
            $collection->dropIndex('idx_tutoriales_escuela_autor_estado');
            $collection->dropIndex('idx_tutoriales_escuela_estado_audiencia');
        });
    }
};
