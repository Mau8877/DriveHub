package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.RolModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioRolModel;

import lombok.RequiredArgsConstructor;

/**
 * Guarda usuarios y su asociación con roles (`usuario_rol`).
 *
 * * Traduce entre el `Usuario` de dominio y el `UsuarioModel` JPA: el dominio
 * * nunca ve un Model ni el id Long.
 */
@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final RolJpaRepository rolJpaRepository;
    private final UsuarioRolJpaRepository usuarioRolJpaRepository;

    @Override
    public boolean existePorEmail(String email) {
        return usuarioJpaRepository.existsByEmail(email);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioModel model = new UsuarioModel(
            usuario.getEmail(), usuario.getPasswordHash(), usuario.getNombres(), usuario.getApellidos(),
            usuario.getTelefono(), usuario.getFechaNacimiento(), usuario.getEscuelaCodigo(), usuario.getEstado());

        try {
            // * saveAndFlush: el UNIQUE del correo salta acá y no al final de la transacción
            model = usuarioJpaRepository.saveAndFlush(model);
        } catch (DataIntegrityViolationException e) {
            // * Carrera entre dos registros con el mismo correo: el segundo pierde
            throw new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo");
        }

        for (Rol rol : usuario.getRoles()) {
            RolModel rolModel = rolJpaRepository.findByNombre(rol.getNombre())
                .orElseThrow(() -> new IllegalStateException("El rol " + rol.getNombre() + " no existe en la base"));
            usuarioRolJpaRepository.save(new UsuarioRolModel(model, rolModel, Instant.now()));
        }

        return Usuario.builder()
            .codigo(model.getCodigo())
            .email(model.getEmail())
            .passwordHash(model.getPasswordHash())
            .nombres(model.getNombres())
            .apellidos(model.getApellidos())
            .telefono(model.getTelefono())
            .fechaNacimiento(model.getFechaNacimiento())
            .escuelaCodigo(model.getEscuelaCodigo())
            .estado(model.getEstado())
            .intentosFallidos(model.getIntentosFallidos())
            .roles(usuario.getRoles())
            .creadoEn(model.getCreatedAt())
            .actualizadoEn(model.getUpdatedAt())
            .build();
    }
}
