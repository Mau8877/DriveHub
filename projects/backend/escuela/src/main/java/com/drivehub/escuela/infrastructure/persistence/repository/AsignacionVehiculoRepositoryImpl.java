package com.drivehub.escuela.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.escuela.common.exception.ConflictException;
import com.drivehub.escuela.common.exception.EscuelaErrorCodes;
import com.drivehub.escuela.domain.entity.AsignacionVehiculo;
import com.drivehub.escuela.domain.entity.Vehiculo;
import com.drivehub.escuela.domain.repository.AsignacionDeAuto;
import com.drivehub.escuela.domain.repository.AsignacionHistorial;
import com.drivehub.escuela.domain.repository.AsignacionVehiculoRepository;
import com.drivehub.escuela.domain.repository.FiltroAsignaciones;
import com.drivehub.escuela.domain.repository.Pagina;
import com.drivehub.escuela.infrastructure.persistence.model.AsignacionVehiculoModel;
import com.drivehub.escuela.infrastructure.persistence.model.InstructorModel;
import com.drivehub.escuela.infrastructure.persistence.model.VehiculoModel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;

/**
 * Asignaciones de autos a instructores (CU10) con el filtro de escuela siempre explícito, a través
 * del auto.
 */
@Repository
@RequiredArgsConstructor
public class AsignacionVehiculoRepositoryImpl implements AsignacionVehiculoRepository {

    private static final String DESDE = " from VehiculoModel v"
        + " left join AsignacionVehiculoModel a on a.vehiculo = v and a.fechaFin is null"
        + " left join a.instructor i";

    private final AsignacionVehiculoJpaRepository jpaRepository;
    private final VehiculoJpaRepository vehiculoJpaRepository;
    private final InstructorJpaRepository instructorJpaRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<AsignacionVehiculo> buscarVigente(UUID escuelaCodigo, UUID vehiculoCodigo) {
        return jpaRepository.vigente(escuelaCodigo, vehiculoCodigo).map(this::aDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> ultimaFechaFin(UUID escuelaCodigo, UUID vehiculoCodigo) {
        return Optional.ofNullable(jpaRepository.ultimaFechaFin(escuelaCodigo, vehiculoCodigo));
    }

    @Override
    @Transactional
    public AsignacionVehiculo abrir(UUID escuelaCodigo, AsignacionVehiculo nueva) {
        VehiculoModel vehiculo = vehiculoJpaRepository
            .findByEscuelaCodigoAndCodigo(escuelaCodigo, nueva.getVehiculoCodigo())
            .orElseThrow(() -> new IllegalStateException("El auto de la asignación no existe en la escuela"));
        InstructorModel instructor = instructorJpaRepository
            .findByCodigoAndEscuelaCodigo(nueva.getInstructorCodigo(), escuelaCodigo)
            .orElseThrow(() -> new IllegalStateException("El instructor de la asignación no existe en la escuela"));
        try {
            // * saveAndFlush: el índice único salta acá y no al confirmar la transacción
            return aDominio(jpaRepository.saveAndFlush(
                new AsignacionVehiculoModel(nueva.getCodigo(), vehiculo, instructor, nueva.getFechaInicio())));
        } catch (DataIntegrityViolationException e) {
            // * Carrera entre dos asignaciones del mismo auto: la segunda pierde
            throw new ConflictException(EscuelaErrorCodes.VEHICULO_YA_ASIGNADO,
                "El auto ya tiene un instructor asignado");
        }
    }

    @Override
    @Transactional
    public Optional<AsignacionVehiculo> cerrar(UUID escuelaCodigo, UUID vehiculoCodigo, LocalDate fechaFin) {
        return jpaRepository.vigente(escuelaCodigo, vehiculoCodigo).map(model -> {
            model.setFechaFin(fechaFin);
            return aDominio(jpaRepository.saveAndFlush(model));
        });
    }

    @Override
    @Transactional
    public AsignacionVehiculo cerrarYAbrir(UUID escuelaCodigo, UUID vehiculoCodigo, LocalDate fecha,
            AsignacionVehiculo nueva) {
        // * Primero se cierra y se envía a la base: así el índice único ve una sola vigente a la vez
        cerrar(escuelaCodigo, vehiculoCodigo, fecha);
        return abrir(escuelaCodigo, nueva);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<AsignacionDeAuto> listar(UUID escuelaCodigo, FiltroAsignaciones filtro, int pagina, int tamano) {
        String patron = patronDeBusqueda(filtro.busqueda());
        Collection<UUID> usuarios = filtro.instructorUsuarioCodigos();
        boolean hayNombres = patron != null && usuarios != null && !usuarios.isEmpty();

        // * JPQL armado con solo los parámetros que existen: un parámetro nulo no tiene tipo para PostgreSQL
        StringBuilder donde = new StringBuilder(" where v.escuelaCodigo = :escuela");
        if (patron != null) {
            donde.append(" and (lower(v.placa) like :patron escape '!' or lower(v.marca) like :patron escape '!'"
                + " or lower(v.modelo) like :patron escape '!'");
            if (hayNombres) {
                donde.append(" or i.usuarioCodigo in :usuarios");
            }
            donde.append(")");
        }
        if (filtro.asignacion() == FiltroAsignaciones.Asignacion.ASIGNADO) {
            donde.append(" and a.id is not null");
        } else if (filtro.asignacion() == FiltroAsignaciones.Asignacion.SIN_ASIGNAR) {
            donde.append(" and a.id is null");
        }

        TypedQuery<Object[]> consulta = entityManager.createQuery(
            "select v, a, i" + DESDE + donde + " order by v.createdAt desc, v.id desc", Object[].class);
        TypedQuery<Long> cuenta = entityManager.createQuery("select count(v)" + DESDE + donde, Long.class);
        for (TypedQuery<?> q : List.of(consulta, cuenta)) {
            q.setParameter("escuela", escuelaCodigo);
            if (patron != null) {
                q.setParameter("patron", patron);
            }
            if (hayNombres) {
                q.setParameter("usuarios", usuarios);
            }
        }
        consulta.setFirstResult((pagina - 1) * tamano).setMaxResults(tamano);

        List<AsignacionDeAuto> filas = consulta.getResultList().stream().map(fila -> {
            VehiculoModel vehiculo = (VehiculoModel) fila[0];
            AsignacionVehiculoModel vigente = (AsignacionVehiculoModel) fila[1];
            InstructorModel instructor = (InstructorModel) fila[2];
            return new AsignacionDeAuto(aVehiculo(vehiculo), vigente == null ? null : aDominio(vigente),
                instructor == null ? null : instructor.getUsuarioCodigo());
        }).toList();
        return new Pagina<>(filas, cuenta.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<AsignacionHistorial> historial(UUID escuelaCodigo, UUID vehiculoCodigo, int pagina, int tamano) {
        Page<AsignacionVehiculoModel> resultado = jpaRepository.historial(
            escuelaCodigo, vehiculoCodigo, PageRequest.of(pagina - 1, tamano));
        return new Pagina<>(resultado.getContent().stream()
            .map(m -> new AsignacionHistorial(aDominio(m), m.getInstructor().getUsuarioCodigo())).toList(),
            resultado.getTotalElements());
    }

    private AsignacionVehiculo aDominio(AsignacionVehiculoModel model) {
        return AsignacionVehiculo.builder()
            .codigo(model.getCodigo())
            .vehiculoCodigo(model.getVehiculo().getCodigo())
            .instructorCodigo(model.getInstructor().getCodigo())
            .fechaInicio(model.getFechaInicio())
            .fechaFin(model.getFechaFin())
            .build();
    }

    // * El listado no necesita la categoría del auto: el dominio la deja en null (igual que CU07)
    private Vehiculo aVehiculo(VehiculoModel model) {
        return Vehiculo.builder()
            .codigo(model.getCodigo())
            .escuelaCodigo(model.getEscuelaCodigo())
            .placa(model.getPlaca())
            .marca(model.getMarca())
            .modelo(model.getModelo())
            .anio(model.getAnio())
            .color(model.getColor())
            .transmision(model.getTransmision())
            .estado(model.getEstado())
            .kilometraje(model.getKilometraje())
            .soatVence(model.getSoatVence())
            .inspeccionVence(model.getInspeccionVence())
            .imagenUrl(model.getImagenUrl())
            .creadoEn(model.getCreatedAt())
            .build();
    }

    /**
     * Texto libre a patrón LIKE en minúsculas. Los comodines del texto (`%`, `_`) y el carácter de
     * escape (`!`) se escapan: la persona busca texto, no escribe patrones.
     */
    private String patronDeBusqueda(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return null;
        }
        String escapado = busqueda.trim().toLowerCase(Locale.ROOT)
            .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escapado + "%";
    }
}
