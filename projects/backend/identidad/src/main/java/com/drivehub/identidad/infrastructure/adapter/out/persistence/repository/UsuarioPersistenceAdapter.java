package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.common.exception.ConflictException;
import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.domain.model.Perfil;
import com.drivehub.identidad.domain.model.Rol;
import com.drivehub.identidad.domain.model.RolNombres;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.out.FiltroEstudiantes;
import com.drivehub.identidad.domain.port.out.PaginaUsuarios;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.PerfilModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.RolModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioModel;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.UsuarioRolModel;

import lombok.RequiredArgsConstructor;

/**
 * Guarda y lee usuarios con su perfil (`perfil`) y sus roles (`usuario_rol`).
 *
 * * Traduce entre el `Usuario` de dominio y el `UsuarioModel` JPA: el dominio
 * * nunca ve un Model ni el id Long.
 */
@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final PerfilJpaRepository perfilJpaRepository;
    private final RolJpaRepository rolJpaRepository;
    private final UsuarioRolJpaRepository usuarioRolJpaRepository;

    @Override
    public boolean existePorEmail(String email) {
        return usuarioJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existePorCi(String ci) {
        return perfilJpaRepository.existsByCi(ci);
    }

    @Override
    public boolean existePorCiDeOtro(String ci, UUID usuarioCodigo) {
        return perfilJpaRepository.existsByCiAndUsuarioCodigoNot(ci, usuarioCodigo);
    }

    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        UsuarioModel model = new UsuarioModel(
            usuario.getEmail(), usuario.getPasswordHash(), usuario.getEscuelaCodigo(), usuario.getEstado());
        model.setCorreoVerificado(usuario.isCorreoVerificado());
        if (usuario.getCodigo() != null) {
            model.asignarCodigo(usuario.getCodigo());
        }

        try {
            // * saveAndFlush: el UNIQUE del correo salta acá y no al final de la transacción
            model = usuarioJpaRepository.saveAndFlush(model);
        } catch (DataIntegrityViolationException e) {
            // * Carrera entre dos registros con el mismo correo: el segundo pierde
            throw new ConflictException(IdentidadErrorCodes.CORREO_EN_USO, "Ya existe una cuenta con ese correo");
        }

        Perfil p = usuario.getPerfil();
        PerfilModel perfilModel = new PerfilModel(model, p.getNombres(), p.getApellidos(), p.getTelefono(),
            p.getFotoUrl(), p.getFechaNacimiento(), p.getCi(), p.getSexo(), p.getDireccion(), p.getLatitud(),
            p.getLongitud(), p.getCategoriaSigla());
        try {
            perfilModel = perfilJpaRepository.saveAndFlush(perfilModel);
        } catch (DataIntegrityViolationException e) {
            // * Carrera entre dos registros con el mismo CI: el segundo pierde y se deshace la cuenta
            throw new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI");
        }

        for (Rol rol : usuario.getRoles()) {
            RolModel rolModel = rolJpaRepository.findByNombre(rol.getNombre())
                .orElseThrow(() -> new IllegalStateException("El rol " + rol.getNombre() + " no existe en la base"));
            usuarioRolJpaRepository.save(new UsuarioRolModel(model, rolModel, Instant.now()));
        }

        return aDominio(model, perfilModel, usuario.getRoles());
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioJpaRepository.findByEmail(email).map(m -> aDominio(m, m.getPerfil(), rolesDe(m)));
    }

    @Override
    public Optional<Usuario> buscarPorCodigo(UUID codigo) {
        return usuarioJpaRepository.findByCodigo(codigo).map(m -> aDominio(m, m.getPerfil(), rolesDe(m)));
    }

    @Override
    public boolean existeAdministradorDeEscuela(UUID escuelaCodigo) {
        return usuarioRolJpaRepository.existeUsuarioConRolEnEscuela(escuelaCodigo, RolNombres.ADMIN_ESCUELA);
    }

    @Override
    public Optional<Usuario> buscarAdministradorDeEscuela(UUID escuelaCodigo) {
        return usuarioRolJpaRepository.findUsuariosConRolEnEscuela(escuelaCodigo, RolNombres.ADMIN_ESCUELA).stream()
            .findFirst().map(m -> aDominio(m, m.getPerfil(), rolesDe(m)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaUsuarios listarEstudiantesDeEscuela(
            UUID escuelaCodigo, FiltroEstudiantes filtro, int pagina, int tamano) {
        Page<UsuarioModel> resultado = usuarioJpaRepository.findAll(
            CuentasDeEscuelaSpecification.deEscuela(escuelaCodigo, RolNombres.ESTUDIANTE, filtro),
            PageRequest.of(pagina - 1, tamano, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new PaginaUsuarios(
            resultado.getContent().stream().map(m -> aDominio(m, m.getPerfil(), Set.of())).toList(),
            resultado.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarEstudianteDeEscuela(UUID escuelaCodigo, UUID codigo) {
        return usuarioJpaRepository
            .findOne(CuentasDeEscuelaSpecification
                .deEscuela(escuelaCodigo, RolNombres.ESTUDIANTE, new FiltroEstudiantes(null, null))
                .and(CuentasDeEscuelaSpecification.conCodigo(codigo)))
            .map(m -> aDominio(m, m.getPerfil(), Set.of()));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaUsuarios listarInstructoresDeEscuela(UUID escuelaCodigo, String busqueda, int pagina, int tamano) {
        Page<UsuarioModel> resultado = usuarioJpaRepository.findAll(
            CuentasDeEscuelaSpecification.deEscuela(escuelaCodigo, RolNombres.INSTRUCTOR,
                new FiltroEstudiantes(busqueda, null)),
            PageRequest.of(pagina - 1, tamano, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new PaginaUsuarios(
            resultado.getContent().stream().map(m -> aDominio(m, m.getPerfil(), Set.of())).toList(),
            resultado.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> buscarInstructoresPorCodigos(UUID escuelaCodigo, Collection<UUID> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return List.of();
        }
        return usuarioJpaRepository
            .findAll(CuentasDeEscuelaSpecification
                .deEscuela(escuelaCodigo, RolNombres.INSTRUCTOR, new FiltroEstudiantes(null, null))
                .and(CuentasDeEscuelaSpecification.conCodigos(codigos)))
            .stream().map(m -> aDominio(m, m.getPerfil(), Set.of())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> buscarInstructorDeEscuela(UUID escuelaCodigo, UUID codigo) {
        return usuarioJpaRepository
            .findOne(CuentasDeEscuelaSpecification
                .deEscuela(escuelaCodigo, RolNombres.INSTRUCTOR, new FiltroEstudiantes(null, null))
                .and(CuentasDeEscuelaSpecification.conCodigo(codigo)))
            .map(m -> aDominio(m, m.getPerfil(), Set.of()));
    }

    @Override
    @Transactional
    public void eliminarDefinitivamente(UUID usuarioCodigo) {
        usuarioJpaRepository.borrarRolesDe(usuarioCodigo);
        usuarioJpaRepository.borrarSesionesDe(usuarioCodigo);
        usuarioJpaRepository.borrarEnlacesDeCorreoDe(usuarioCodigo);
        usuarioJpaRepository.borrarPerfilDe(usuarioCodigo);
        usuarioJpaRepository.borrarUsuario(usuarioCodigo);
    }

    @Override
    @Transactional
    public Usuario actualizar(Usuario usuario) {
        UsuarioModel model = usuarioJpaRepository.findByCodigo(usuario.getCodigo())
            .orElseThrow(() -> new IllegalStateException("No existe el usuario a actualizar"));
        model.setEmail(usuario.getEmail());
        model.setPasswordHash(usuario.getPasswordHash());
        model.setEstado(usuario.getEstado());
        model.setIntentosFallidos(usuario.getIntentosFallidos());
        model.setBloqueadoHasta(usuario.getBloqueadoHasta());
        model.setUltimoLogin(usuario.getUltimoLogin());
        model.setCorreoVerificado(usuario.isCorreoVerificado());

        PerfilModel perfilModel = model.getPerfil();
        Perfil p = usuario.getPerfil();
        perfilModel.setNombres(p.getNombres());
        perfilModel.setApellidos(p.getApellidos());
        perfilModel.setTelefono(p.getTelefono());
        perfilModel.setFechaNacimiento(p.getFechaNacimiento());
        perfilModel.setFotoUrl(p.getFotoUrl());
        perfilModel.setCi(p.getCi());
        perfilModel.setSexo(p.getSexo());
        perfilModel.setDireccion(p.getDireccion());
        perfilModel.setLatitud(p.getLatitud());
        perfilModel.setLongitud(p.getLongitud());
        perfilModel.setCategoriaSigla(p.getCategoriaSigla());

        UsuarioModel guardado = usuarioJpaRepository.save(model);
        try {
            usuarioJpaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(IdentidadErrorCodes.CI_EN_USO, "Ya existe una cuenta con ese CI");
        }
        return aDominio(guardado, guardado.getPerfil(), usuario.getRoles());
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

    private Usuario aDominio(UsuarioModel model, PerfilModel perfil, Set<Rol> roles) {
        return Usuario.builder()
            .codigo(model.getCodigo())
            .email(model.getEmail())
            .passwordHash(model.getPasswordHash())
            .perfil(aPerfil(perfil))
            .escuelaCodigo(model.getEscuelaCodigo())
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

    private Perfil aPerfil(PerfilModel m) {
        if (m == null) {
            throw new IllegalStateException("La cuenta no tiene perfil");
        }
        return Perfil.builder()
            .nombres(m.getNombres())
            .apellidos(m.getApellidos())
            .telefono(m.getTelefono())
            .fotoUrl(m.getFotoUrl())
            .fechaNacimiento(m.getFechaNacimiento())
            .ci(m.getCi())
            .sexo(m.getSexo())
            .direccion(m.getDireccion())
            .latitud(m.getLatitud())
            .longitud(m.getLongitud())
            .categoriaSigla(m.getCategoriaSigla())
            .build();
    }
}
