<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        // ! Datos de ejemplo solo en desarrollo: nunca se siembran en producción.
        if (app()->environment('local')) {
            $this->call(ManualSeeder::class);
        }
    }
}
