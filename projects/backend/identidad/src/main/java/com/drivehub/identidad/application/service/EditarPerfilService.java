package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

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

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.EditarPerfilComando;
import com.drivehub.identidad.domain.port.in.EditarPerfilUseCase;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;

/**
 * CU06 (F1): editar los datos personales de la cuenta propia.
 *
 * * Solo cambian los campos presentes. Las reglas son las del registro: nombres y apellidos
 * * no vacíos y la edad mínima (ERR_IDE_02, el mismo código del registro).
 * ! En los registros solo van los NOMBRES de los campos modificados, nunca sus valores.
 */
@Service
public class EditarPerfilService implements EditarPerfilUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(EditarPerfilService.class);
    private static final String MODULO = "perfil";

    private final UsuarioRepositoryPort usuarioRepository;
    private final PerfilArmador perfilArmador;
    private final Clock reloj;
    private final int edadMinima;

    public EditarPerfilService(
            UsuarioRepositoryPort usuarioRepository,
            PerfilArmador perfilArmador,
            Clock reloj,
            @Value("${app.registro.edad-minima:18}") int edadMinima) {
        this.usuarioRepository = usuarioRepository;
        this.perfilArmador = perfilArmador;
        this.reloj = reloj;
        this.edadMinima = edadMinima;
    }

    @Override
    @Transactional
    public PerfilUsuario editar(EditarPerfilComando comando) {
        Usuario usuario = usuarioRepository.buscarPorCodigo(comando.usuarioCodigo())
            .orElseThrow(() -> new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó"));

        Usuario.UsuarioBuilder cambios = usuario.toBuilder();
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

        Usuario guardado = campos.isEmpty() ? usuario : usuarioRepository.actualizar(cambios.build());
        LOG.info("Perfil editado", kv("module", MODULO), kv("usuarioCodigo", usuario.getCodigo()),
            kv("campos", campos));
        return perfilArmador.armar(guardado);
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
