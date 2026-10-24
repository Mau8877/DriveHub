package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.RegistrarVehiculoInput;
import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.util.UuidV7;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU07: el Administrador de escuela registra un auto, que nace DISPONIBLE y ligado a su escuela.
 */
@Service
@RequiredArgsConstructor
public class RegistrarVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrarVehiculoUseCase.class);

    private final VehiculoRepository vehiculoRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public VehiculoOutput ejecutar(RegistrarVehiculoInput input) {
        String placa = ReglasVehiculo.limpiar(input.placa() == null ? null : input.placa().toUpperCase(Locale.ROOT));
        String marca = ReglasVehiculo.limpiar(input.marca());
        String modelo = ReglasVehiculo.limpiar(input.modelo());
        String color = ReglasVehiculo.limpiar(input.color());
        if (color != null && color.isEmpty()) {
            color = null;
        }

        if (placa == null || !ReglasVehiculo.PLACA.matcher(placa).matches()) {
            throw ReglasVehiculo.invalido("La placa debe tener entre 5 y 10 caracteres: letras, números y guiones");
        }
        ReglasVehiculo.texto(marca, "marca", 40);
        ReglasVehiculo.texto(modelo, "modelo", 40);
        ReglasVehiculo.colorOpcional(color);
        ReglasVehiculo.anio(input.anio());
        ReglasVehiculo.kilometraje(input.kilometraje());
        if (input.transmision() == null) {
            throw ReglasVehiculo.invalido("La transmisión es obligatoria");
        }

        if (vehiculoRepository.existePorPlaca(placa)) {
            throw placaEnUso();
        }

        UUID codigo = UuidV7.generar();
        Vehiculo guardado = vehiculoRepository.guardar(Vehiculo.builder()
            .codigo(codigo)
            .escuelaCodigo(input.escuelaCodigo())
            .placa(placa)
            .marca(marca)
            .modelo(modelo)
            .anio(input.anio())
            .color(color)
            .transmision(input.transmision())
            .estado(EstadoVehiculo.DISPONIBLE)
            .kilometraje(input.kilometraje() == null ? 0 : input.kilometraje())
            .soatVence(input.soatVence())
            .inspeccionVence(input.inspeccionVence())
            .build());

        LOG.info("Auto registrado", kv("module", ReglasVehiculo.MODULO), kv("vehiculoCodigo", codigo),
            kv("escuelaCodigo", input.escuelaCodigo()));
        return ReglasVehiculo.aSalida(guardado);
    }

    private ConflictException placaEnUso() {
        LOG.warn("Auto rechazado: placa en uso",
            kv("module", ReglasVehiculo.MODULO), kv("errorCode", EscuelaErrorCodes.PLACA_EN_USO));
        return new ConflictException(EscuelaErrorCodes.PLACA_EN_USO, "Ya existe un auto con esa placa");
    }
}
