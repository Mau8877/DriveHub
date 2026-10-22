package com.drivehub.identidad.application.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.drivehub.identidad.common.exception.IdentidadErrorCodes;
import com.drivehub.identidad.common.exception.UnauthorizedException;
import com.drivehub.identidad.common.exception.ValidationException;
import com.drivehub.identidad.common.util.UuidV7;
import com.drivehub.identidad.domain.model.ImagenInvalidaException;
import com.drivehub.identidad.domain.model.Usuario;
import com.drivehub.identidad.domain.port.in.ActualizarFotoPerfilUseCase;
import com.drivehub.identidad.domain.port.in.PerfilUsuario;
import com.drivehub.identidad.domain.port.out.AlmacenFotosPort;
import com.drivehub.identidad.domain.port.out.ImagenValidada;
import com.drivehub.identidad.domain.port.out.UsuarioRepositoryPort;
import com.drivehub.identidad.domain.port.out.ValidadorImagenPort;

/**
 * CU06 (F2): subir o reemplazar la foto de la cuenta.
 *
 * * Orden: validar, guardar el objeto nuevo en el almacenamiento, actualizar la cuenta y borrar el
 * * objeto anterior. Cada subida usa una clave nueva, así el navegador nunca muestra una foto
 * * cacheada. Si la base falla se borra el objeto nuevo (compensación); si borrar el anterior
 * * falla solo queda un WARN: el huérfano no afecta a nadie.
 * * Sin @Transactional a propósito: el cambio en la base tiene que estar confirmado ANTES de
 * * borrar el objeto anterior, y un fallo al confirmar tiene que poder compensarse acá.
 * ! Nunca se loguean los bytes del archivo ni la URL prefirmada.
 */
@Service
public class ActualizarFotoPerfilService implements ActualizarFotoPerfilUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ActualizarFotoPerfilService.class);
    private static final String MODULO = "perfil";

    private final UsuarioRepositoryPort usuarioRepository;
    private final ValidadorImagenPort validadorImagen;
    private final AlmacenFotosPort almacenFotos;
    private final PerfilArmador perfilArmador;
    private final int maxBytes;

    public ActualizarFotoPerfilService(
            UsuarioRepositoryPort usuarioRepository,
            ValidadorImagenPort validadorImagen,
            AlmacenFotosPort almacenFotos,
            PerfilArmador perfilArmador,
            @Value("${app.perfil.foto.max-bytes:5242880}") int maxBytes) {
        this.usuarioRepository = usuarioRepository;
        this.validadorImagen = validadorImagen;
        this.almacenFotos = almacenFotos;
        this.perfilArmador = perfilArmador;
        this.maxBytes = maxBytes;
    }

    @Override
    public PerfilUsuario actualizar(UUID usuarioCodigo, byte[] contenido) {
        Usuario usuario = usuarioRepository.buscarPorCodigo(usuarioCodigo)
            .orElseThrow(() -> new UnauthorizedException(IdentidadErrorCodes.SESION_INVALIDA, "Tu sesión terminó"));
        ImagenValidada imagen = revisar(contenido);

        String claveNueva = "usuarios/" + usuarioCodigo + "/" + UuidV7.generar() + "." + imagen.formato().extension();
        almacenFotos.guardar(claveNueva, contenido, imagen.formato().contentType());

        Usuario actualizado;
        try {
            actualizado = usuarioRepository.actualizar(usuario.toBuilder().fotoUrl(claveNueva).build());
        } catch (RuntimeException e) {
            descartar(claveNueva, usuarioCodigo, "No se pudo guardar la foto en la cuenta: se descarta el objeto nuevo");
            throw e;
        }

        if (usuario.getFotoUrl() != null) {
            descartar(usuario.getFotoUrl(), usuarioCodigo, "No se pudo borrar la foto anterior: queda un objeto huérfano");
        }
        LOG.info("Foto de perfil actualizada", kv("module", MODULO), kv("usuarioCodigo", usuarioCodigo));
        return perfilArmador.armar(actualizado);
    }

    private ImagenValidada revisar(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw rechazo(IdentidadErrorCodes.FOTO_INVALIDA, "El archivo está vacío");
        }
        if (contenido.length > maxBytes) {
            throw rechazo(IdentidadErrorCodes.FOTO_DEMASIADO_GRANDE, "La foto pesa más de " + maxBytes / (1024 * 1024) + " MB");
        }
        try {
            return validadorImagen.validar(contenido);
        } catch (ImagenInvalidaException e) {
            throw rechazo(IdentidadErrorCodes.FOTO_INVALIDA, mensajeDe(e.getMotivo()));
        }
    }

    private static String mensajeDe(ImagenInvalidaException.Motivo motivo) {
        return switch (motivo) {
            case FORMATO -> "El archivo no es una imagen JPG, PNG o WebP";
            case ILEGIBLE, DATOS_EXTRA -> "La imagen está dañada o no se puede leer";
            case DIMENSIONES -> "La imagen es demasiado grande en píxeles: máximo 4000×4000";
        };
    }

    private ValidationException rechazo(String codigo, String mensaje) {
        LOG.warn("Foto de perfil rechazada", kv("module", MODULO), kv("errorCode", codigo));
        return new ValidationException(codigo, mensaje);
    }

    /** Borra un objeto sin dejar que un fallo del almacenamiento rompa el resultado de la operación. */
    private void descartar(String clave, UUID usuarioCodigo, String mensaje) {
        try {
            almacenFotos.borrar(clave);
        } catch (RuntimeException e) {
            LOG.warn(mensaje, kv("module", MODULO), kv("usuarioCodigo", usuarioCodigo));
        }
    }
}
