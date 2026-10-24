package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.application.dto.EditarVehiculoInput;
import com.drivehub.escuela.application.dto.VehiculoOutput;
import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.common.exception.NotFoundException;
import com.drivehub.escuela.domain.entity.EstadoVehiculo;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.VehiculoRepository;

import lombok.RequiredArgsConstructor;

/**
 * CU07: el Administrador de escuela corrige los datos de un auto que no está de baja.
 *
 * * Un campo nulo no se toca. La placa, la transmisión y el estado no tienen parámetro: no se
 * * editan por acá (el estado cambia con CambiarEstadoVehiculoUseCase).
 */
@Service
@RequiredArgsConstructor
public class EditarVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EditarVehiculoUseCase.class);

    private final VehiculoRepository vehiculoRepository;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    @Transactional
    public VehiculoOutput ejecutar(EditarVehiculoInput input) {
        String marca = ReglasVehiculo.limpiar(input.marca());
        String modelo = ReglasVehiculo.limpiar(input.modelo());
        String color = ReglasVehiculo.limpiar(input.color());
        if (marca != null) {
            ReglasVehiculo.texto(marca, "marca", 40);
        }
        if (modelo != null) {
            ReglasVehiculo.texto(modelo, "modelo", 40);
        }
        ReglasVehiculo.colorOpcional(color);
        ReglasVehiculo.anio(input.anio());
        ReglasVehiculo.kilometraje(input.kilometraje());

        Vehiculo actual = vehiculoRepository.buscarPorCodigo(input.escuelaCodigo(), input.codigo()).orElseThrow(() -> {
            LOG.warn("Edición rechazada: el auto no existe", kv("module", ReglasVehiculo.MODULO),
                kv("errorCode", EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO), kv("vehiculoCodigo", input.codigo()));
            return new NotFoundException(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO, "El auto no existe");
        });
        if (actual.getEstado() == EstadoVehiculo.BAJA) {
            LOG.warn("Edición rechazada: el auto está de baja", kv("module", ReglasVehiculo.MODULO),
                kv("errorCode", EscuelaErrorCodes.TRANSICION_VEHICULO_INVALIDA), kv("vehiculoCodigo", input.codigo()));
            throw new ConflictException(EscuelaErrorCodes.TRANSICION_VEHICULO_INVALIDA,
                "El auto está dado de baja y ya no se puede editar");
        }
        if (input.kilometraje() != null && input.kilometraje() < actual.getKilometraje()) {
            throw ReglasVehiculo.invalido(
                "El kilometraje no puede ser menor al actual (" + actual.getKilometraje() + ")");
        }

        Vehiculo editado = actual.toBuilder()
            .marca(marca != null ? marca : actual.getMarca())
            .modelo(modelo != null ? modelo : actual.getModelo())
            .anio(input.anio() != null ? input.anio() : actual.getAnio())
            .color(color != null && !color.isEmpty() ? color : actual.getColor())
            .kilometraje(input.kilometraje() != null ? input.kilometraje() : actual.getKilometraje())
            .soatVence(input.soatVence() != null ? input.soatVence() : actual.getSoatVence())
            .inspeccionVence(input.inspeccionVence() != null ? input.inspeccionVence() : actual.getInspeccionVence())
            .build();
        Vehiculo guardado = vehiculoRepository.actualizar(editado).orElseThrow(() ->
            new NotFoundException(EscuelaErrorCodes.VEHICULO_NO_ENCONTRADO, "El auto no existe"));

        LOG.info("Auto editado", kv("module", ReglasVehiculo.MODULO), kv("vehiculoCodigo", input.codigo()));
        return ReglasVehiculo.aSalida(guardado);
    }
}
