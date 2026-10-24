package com.drivehub.escuela.application.usecase;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.drivehub.escuela.domain.gateway.FirmaSubidaImagen;
import com.drivehub.escuela.domain.gateway.ImagenesGateway;

import lombok.RequiredArgsConstructor;

/**
 * CU07: el Administrador de escuela pide permiso para subir la foto de un auto a Cloudinary.
 *
 * * Cada escuela sube a su propia carpeta (`drivehub/{escuela}/vehiculos`), con lo que sus
 * * archivos quedan separados de los de las demás.
 */
@Service
@RequiredArgsConstructor
public class FirmarSubidaImagenVehiculoUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(FirmarSubidaImagenVehiculoUseCase.class);

    private final ImagenesGateway imagenesGateway;

    @PreAuthorize("hasRole('ADMIN_ESCUELA')")
    public FirmaSubidaImagen ejecutar(UUID escuelaCodigo) {
        FirmaSubidaImagen firma = imagenesGateway.firmarSubida("drivehub/" + escuelaCodigo + "/vehiculos");
        LOG.info("Subida de imagen firmada", kv("module", ReglasVehiculo.MODULO), kv("escuelaCodigo", escuelaCodigo));
        return firma;
    }
}
