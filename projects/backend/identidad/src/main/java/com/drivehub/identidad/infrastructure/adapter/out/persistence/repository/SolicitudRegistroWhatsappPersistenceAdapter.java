package com.drivehub.identidad.infrastructure.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.drivehub.identidad.domain.model.SolicitudRegistroWhatsapp;
import com.drivehub.identidad.domain.port.out.SolicitudRegistroWhatsappRepositoryPort;
import com.drivehub.identidad.infrastructure.adapter.out.persistence.model.SolicitudRegistroWhatsappModel;

import lombok.RequiredArgsConstructor;

/** Registros por WhatsApp pendientes de la contraseña (`solicitud_registro_whatsapp`). */
@Component
@RequiredArgsConstructor
public class SolicitudRegistroWhatsappPersistenceAdapter implements SolicitudRegistroWhatsappRepositoryPort {

    private final SolicitudRegistroWhatsappJpaRepository jpaRepository;

    @Override
    public SolicitudRegistroWhatsapp guardar(SolicitudRegistroWhatsapp s) {
        return aDominio(jpaRepository.save(new SolicitudRegistroWhatsappModel(s.getTelefono(), s.getNombres(),
            s.getApellidos(), s.getFechaNacimiento(), s.getSexo(), s.getCi(), s.getCorreo(), s.getLatitud(),
            s.getLongitud(), s.getTokenHash(), s.getExpiraEn())));
    }

    @Override
    public Optional<SolicitudRegistroWhatsapp> buscarPorTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::aDominio);
    }

    @Override
    @Transactional
    public void eliminarDelTelefono(String telefono) {
        jpaRepository.borrarDelTelefono(telefono);
    }

    @Override
    @Transactional
    public void eliminar(SolicitudRegistroWhatsapp solicitud) {
        jpaRepository.findByTokenHash(solicitud.getTokenHash()).ifPresent(m -> jpaRepository.borrarDelTelefono(m.getTelefono()));
    }

    @Override
    @Transactional
    public int eliminarVencidas(Instant ahora) {
        return jpaRepository.borrarVencidas(ahora);
    }

    private SolicitudRegistroWhatsapp aDominio(SolicitudRegistroWhatsappModel m) {
        return SolicitudRegistroWhatsapp.builder()
            .codigo(m.getCodigo())
            .telefono(m.getTelefono())
            .nombres(m.getNombres())
            .apellidos(m.getApellidos())
            .fechaNacimiento(m.getFechaNacimiento())
            .sexo(m.getSexo())
            .ci(m.getCi())
            .correo(m.getCorreo())
            .latitud(m.getLatitud())
            .longitud(m.getLongitud())
            .tokenHash(m.getTokenHash())
            .expiraEn(m.getExpiraEn())
            .build();
    }
}
