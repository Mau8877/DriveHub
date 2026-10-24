package com.drivehub.escuela.infrastructure.config;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.drivehub.escuela.common.util.UuidV7;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Transmision;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * Autos de ejemplo para desarrollo: la flota de `escuelaexample`.
 *
 * ! Solo con el perfil `dev`. Nunca se siembra en producción.
 * * Las fotos son la imagen de ejemplo `car` de la cuenta pública `demo` de Cloudinary, con un
 * * tono distinto por auto (`e_hue`): no hace falta cuenta propia para verlas. Se reemplazan
 * * subiendo la foto real desde la pantalla de autos.
 * * El auto de baja se crea primero: el listado va del más reciente al más antiguo y así queda al final.
 * * Se siembra por placa: si ya existe, no se toca; un auto borrado a mano se vuelve a crear.
 */
@Component
@Profile("dev")
@Order(2)
@RequiredArgsConstructor
public class VehiculoSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(VehiculoSeeder.class);
    private static final String FOTO = "https://res.cloudinary.com/demo/image/upload/c_fill,w_800,h_500,e_hue:%d/car.jpg";

    private record Ejemplo(String placa, String marca, String modelo, int anio, String color,
            Transmision transmision, int kilometraje, EstadoVehiculo estado, int tono) {
    }

    private static final List<Ejemplo> EJEMPLOS = List.of(
        new Ejemplo("SEED-008", "Renault", "Sandero", 2016, "Beige", Transmision.MANUAL, 102300, EstadoVehiculo.BAJA, 30),
        new Ejemplo("SEED-001", "Toyota", "Yaris", 2021, "Rojo", Transmision.MANUAL, 18500, EstadoVehiculo.DISPONIBLE, 0),
        new Ejemplo("SEED-002", "Suzuki", "Swift", 2020, "Azul", Transmision.MANUAL, 32100, EstadoVehiculo.DISPONIBLE, 200),
        new Ejemplo("SEED-003", "Nissan", "Versa", 2022, "Blanco", Transmision.AUTOMATICO, 9800, EstadoVehiculo.DISPONIBLE, 330),
        new Ejemplo("SEED-004", "Kia", "Rio", 2019, "Gris", Transmision.MANUAL, 54200, EstadoVehiculo.DISPONIBLE, 90),
        new Ejemplo("SEED-005", "Hyundai", "Accent", 2021, "Negro", Transmision.AUTOMATICO, 27300, EstadoVehiculo.MANTENIMIENTO, 270),
        new Ejemplo("SEED-006", "Chevrolet", "Spark", 2018, "Verde", Transmision.MANUAL, 71800, EstadoVehiculo.DISPONIBLE, 140),
        new Ejemplo("SEED-007", "Volkswagen", "Gol", 2017, "Plata", Transmision.MANUAL, 88400, EstadoVehiculo.MANTENIMIENTO, 40));

    private final VehiculoRepository vehiculoRepository;

    @Override
    public void run(ApplicationArguments args) {
        int creados = 0;
        for (Ejemplo e : EJEMPLOS) {
            if (vehiculoRepository.existePorPlaca(e.placa())) {
                continue;
            }
            vehiculoRepository.guardar(Vehiculo.builder()
                .codigo(UuidV7.generar())
                .escuelaCodigo(EscuelaSeeder.CODIGO_ESCUELA_EXAMPLE)
                .placa(e.placa())
                .marca(e.marca())
                .modelo(e.modelo())
                .anio((short) e.anio())
                .color(e.color())
                .transmision(e.transmision())
                .estado(e.estado())
                .kilometraje(e.kilometraje())
                .soatVence(LocalDate.of(Year.now().getValue() + 1, 3, 15))
                .inspeccionVence(LocalDate.of(Year.now().getValue() + 1, 6, 30))
                .imagenUrl(FOTO.formatted(e.tono()))
                .build());
            creados++;
        }
        if (creados > 0) {
            LOG.info("Autos de ejemplo creados para desarrollo ({})", creados);
        }
    }
}
