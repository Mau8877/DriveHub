<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\Schema;
use MongoDB\Laravel\Schema\Blueprint;

/**
 * CU32: cada manual pertenece a una escuela (tenant). El listado siempre filtra por
 * `escuela_codigo` y después por estado y categoría: ese es el índice.
 */
return new class extends Migration
{
    protected $connection = 'mongodb';

    public function up(): void
    {
        Schema::table('manuales', function (Blueprint $collection) {
            $collection->index(['escuela_codigo', 'estado', 'categoria'], 'idx_manuales_escuela_estado_categoria');
        });
    }

    public function down(): void
    {
        Schema::table('manuales', function (Blueprint $collection) {
            $collection->dropIndex('idx_manuales_escuela_estado_categoria');
        });
    }
};
