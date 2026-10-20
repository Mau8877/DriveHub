package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
 * Guarda y lee usuarios con sus roles (`usuario_rol`).
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
        model.setCorreoVerificado(usuario.isCorreoVerificado());

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

        return aDominio(model, usuario.getRoles());
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioJpaRepository.findByEmail(email).map(m -> aDominio(m, rolesDe(m)));
    }

    @Override
    public Optional<Usuario> buscarPorCodigo(UUID codigo) {
        return usuarioJpaRepository.findByCodigo(codigo).map(m -> aDominio(m, rolesDe(m)));
    }

    @Override
    public Usuario actualizar(Usuario usuario) {
        UsuarioModel model = usuarioJpaRepository.findByCodigo(usuario.getCodigo())
            .orElseThrow(() -> new IllegalStateException("No existe el usuario a actualizar"));
        model.setPasswordHash(usuario.getPasswordHash());
        model.setEstado(usuario.getEstado());
        model.setIntentosFallidos(usuario.getIntentosFallidos());
        model.setBloqueadoHasta(usuario.getBloqueadoHasta());
        model.setUltimoLogin(usuario.getUltimoLogin());
        model.setCorreoVerificado(usuario.isCorreoVerificado());
        return aDominio(usuarioJpaRepository.save(model), usuario.getRoles());
    }

    private Set<Rol> rolesDe(UsuarioModel model) {
        Set<Rol> roles = new HashSet<>();
        for (UsuarioRolModel asociacion : usuarioRolJpaRepository.findConRolPorUsuarioId(model.getId())) {
            RolModel rol = asociacion.getRol();
            roles.add(Rol.builder().codigo(rol.getCodigo()).nombre(rol.getNombre())
                .descripcion(rol.getDescripcion()).permisos(Set.of()).build());
        }
        return roles;
    }

    private Usuario aDominio(UsuarioModel model, Set<Rol> roles) {
        return Usuario.builder()
            .codigo(model.getCodigo())
            .email(model.getEmail())
            .passwordHash(model.getPasswordHash())
            .nombres(model.getNombres())
            .apellidos(model.getApellidos())
            .telefono(model.getTelefono())
            .fechaNacimiento(model.getFechaNacimiento())
            .escuelaCodigo(model.getEscuelaCodigo())
            .fotoUrl(model.getFotoUrl())
            .estado(model.getEstado())
            .intentosFallidos(model.getIntentosFallidos())
            .ultimoLogin(model.getUltimoLogin())
            .bloqueadoHasta(model.getBloqueadoHasta())
            .correoVerificado(model.isCorreoVerificado())
            .roles(roles)
            .creadoEn(model.getCreatedAt())
            .actualizadoEn(model.getUpdatedAt())
            .build();
    }
}
