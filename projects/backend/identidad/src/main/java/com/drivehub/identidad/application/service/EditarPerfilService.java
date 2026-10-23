package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Ci;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EditarPerfilComando;
import com.drivehub.identidad.domain.port.in.EditarPerfilUseCase;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.GeocodificadorInversoPort;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU06 (F1) + CU08a (F2): editar los datos de la persona de la cuenta propia.
 *
 * * Solo cambian los campos presentes. Las reglas son las del registro: nombres y apellidos
 * * no vacíos, la edad mínima (ERR_IDE_02) y el CI único en la plataforma (ERR_IDE_19).
 * * Si llegan coordenadas distintas a las guardadas, el servidor recalcula la dirección; si son
 * * iguales no vuelve a consultar el mapa. La categoría no se toca nunca desde acá.
 * ! En los registros solo van los NOMBRES de los campos modificados, nunca sus valores.
 */
@Service
public class EditarPerfilService implements EditarPerfilUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EditarPerfilService.class);
    private static final String MODULO = "perfil";

    private final UsuarioRepositoryPort usuarioRepository;
    private final PerfilArmador perfilArmador;
    private final GeocodificadorInversoPort geocodificador;
    private final Clock reloj;
    private final int edadMinima;

    public EditarPerfilService(
            UsuarioRepositoryPort usuarioRepository,
            PerfilArmador perfilArmador,
            GeocodificadorInversoPort geocodificador,
            Clock reloj,
            @Value("${app.registro.edad-minima:18}") int edadMinima) {
        this.usuarioRepository = usuarioRepository;
        this.perfilArmador = perfilArmador;
        this.geocodificador = geocodificador;
        this.reloj = reloj;
        this.edadMinima = edadMinima;
    }

    @Override
    @Transactional
    public PerfilUsuario editar(EditarPerfilComando comando) {
        Usuario usuario = usuarioRepository.buscarPorCodigo(comando.usuarioCodigo())
            .orElseThrow(() -> new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó"));

        Perfil.PerfilBuilder cambios = usuario.getPerfil().toBuilder();
        List<String> campos = new ArrayList<>();

        comando.nombres().ifPresent(valor -> {
            cambios.nombres(obligatorio(valor, "Los nombres"));
            campos.add("nombres");
        });
        comando.apellidos().ifPresent(valor -> {
            cambios.apellidos(obligatorio(valor, "Los apellidos"));
            campos.add("apellidos");
        });
        comando.telefono().ifPresent(valor -> {
            cambios.telefono(valor.isBlank() ? null : valor.trim());
            campos.add("telefono");
        });
        comando.fechaNacimiento().ifPresent(valor -> {
            comprobarEdad(valor);
            cambios.fechaNacimiento(valor);
            campos.add("fechaNacimiento");
        });
        comando.ci().ifPresent(valor -> {
            String ci = Ci.normalizar(valor);
            if (usuarioRepository.existePorCiDeOtro(ci, usuario.getCodigo())) {
                LOG.warn("Edicion de perfil rechazada: CI en uso",
                    kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.CI_EN_USO));
                throw new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI");
            }
            cambios.ci(ci);
            campos.add("ci");
        });
        comando.sexo().ifPresent(valor -> {
            cambios.sexo(valor);
            campos.add("sexo");
        });
        if (comando.latitud().isPresent() != comando.longitud().isPresent()) {
            throw new ValidationException("ERR_SYS_01", "La latitud y la longitud van juntas");
        }
        if (comando.latitud().isPresent()) {
            cambiarUbicacion(usuario.getPerfil(), cambios, comando.latitud().get(), comando.longitud().get(), campos);
        }

        Usuario guardado = campos.isEmpty()
            ? usuario
            : usuarioRepository.actualizar(usuario.toBuilder().perfil(cambios.build()).build());
        LOG.info("Perfil editado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()),
            kv("campos", campos));
        return perfilArmador.armar(guardado);
    }

    /** Recalcula la dirección solo si el punto cambió; si no se puede calcular, queda en blanco. */
    private void cambiarUbicacion(Perfil actual, Perfil.PerfilBuilder cambios, BigDecimal latitud,
            BigDecimal longitud, List<String> campos) {
        if (iguales(actual.getLatitud(), latitud) && iguales(actual.getLongitud(), longitud)) {
            return;
        }
        cambios.latitud(latitud).longitud(longitud)
            .direccion(geocodificador.direccionDe(latitud, longitud).orElse(null));
        campos.add("ubicacion");
    }

    private boolean iguales(BigDecimal a, BigDecimal b) {
        return a != null && a.compareTo(b) == 0;
    }

    private String obligatorio(String valor, String campo) {
        if (valor.isBlank()) {
            throw new ValidationException("ERR_SYS_01", campo + " no pueden estar vacíos");
        }
        return valor.trim();
    }

    private void comprobarEdad(LocalDate fechaNacimiento) {
        if (Period.between(fechaNacimiento, LocalDate.now(reloj)).getYears() < edadMinima) {
            LOG.warn("Edicion de perfil rechazada: menor de edad",
                kv("module", MODULO), kv("errorCode", IdentidadErrorCodes.MENOR_DE_EDAD));
            throw new ValidationException(IdentidadErrorCodes.MENOR_DE_EDAD,
                "Debes tener al menos " + edadMinima + " años");
        }
    }
}
