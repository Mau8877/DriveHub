package com.drivehub.identidad.infrastructure.adapter.out.imagen;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.drivehub.identidad.domain.model.FormatoImagen;
import com.drivehub.identidad.domain.model.ImagenInvalidaException;
import com.drivehub.identidad.domain.model.ImagenInvalidaException.Motivo;
import com.drivehub.identidad.domain.port.out.ImagenValidada;
import com.drivehub.identidad.domain.port.out.ValidadorImagenPort;

/**
 * Revisión de la foto con lo que trae el JDK (más el plugin de WebP), todo en memoria.
 *
 * * Orden: formato por la firma de bytes, dimensiones leyendo solo el encabezado (antes de
 * * decodificar los píxeles, para no caer en una imagen bomba), decodificación completa y, al
 * * final, que no sobren bytes después del final de la imagen.
 * ! Nunca se loguea el contenido del archivo.
 */
@Component
public class ImagenJdkValidadorAdapter implements ValidadorImagenPort {

    private static final Logger LOG = LoggerFactory.getLogger(ImagenJdkValidadorAdapter.class);
    private static final String MODULO = "perfil";

    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] FIRMA_RIFF = {'R', 'I', 'F', 'F'};

    private final int maxLadoPx;

    public ImagenJdkValidadorAdapter(@Value("${app.perfil.foto.max-lado-px:4000}") int maxLadoPx) {
        this.maxLadoPx = maxLadoPx;
    }

    @Override
    public ImagenValidada validar(byte[] contenido) {
        try {
            return revisar(contenido);
        } catch (ImagenInvalidaException e) {
            LOG.warn("Foto rechazada", kv("module", MODULO), kv("motivo", e.getMotivo()));
            throw e;
        }
    }

    private ImagenValidada revisar(byte[] contenido) {
        FormatoImagen formato = detectarFormato(contenido);
        int[] dimensiones = leerYDecodificar(contenido);
        comprobarSinDatosExtra(formato, contenido);
        return new ImagenValidada(formato, dimensiones[0], dimensiones[1]);
    }

    private FormatoImagen detectarFormato(byte[] c) {
        if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) {
            return FormatoImagen.JPEG;
        }
        if (empiezaCon(c, FIRMA_PNG)) {
            return FormatoImagen.PNG;
        }
        if (c.length >= 12 && empiezaCon(c, FIRMA_RIFF) && c[8] == 'W' && c[9] == 'E' && c[10] == 'B'
                && c[11] == 'P') {
            return FormatoImagen.WEBP;
        }
        throw new ImagenInvalidaException(Motivo.FORMATO);
    }

    /** Lee el encabezado, comprueba las dimensiones y recién entonces decodifica los píxeles. */
    private int[] leerYDecodificar(byte[] contenido) {
        ImageReader lector = null;
        try (ImageInputStream flujo = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenido))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(flujo);
            if (!lectores.hasNext()) {
                throw new ImagenInvalidaException(Motivo.ILEGIBLE);
            }
            lector = lectores.next();
            lector.setInput(flujo, true, true);
            int ancho = lector.getWidth(0);
            int alto = lector.getHeight(0);
            if (ancho <= 0 || alto <= 0) {
                throw new ImagenInvalidaException(Motivo.ILEGIBLE);
            }
            if (ancho > maxLadoPx || alto > maxLadoPx) {
                throw new ImagenInvalidaException(Motivo.DIMENSIONES);
            }
            lector.read(0);
            return new int[] {ancho, alto};
        } catch (IOException | RuntimeException e) {
            if (e instanceof ImagenInvalidaException invalida) {
                throw invalida;
            }
            throw new ImagenInvalidaException(Motivo.ILEGIBLE);
        } finally {
            if (lector != null) {
                lector.dispose();
            }
        }
    }

    private void comprobarSinDatosExtra(FormatoImagen formato, byte[] c) {
        switch (formato) {
            case JPEG -> comprobarFinJpeg(c);
            case PNG -> comprobarFinPng(c);
            case WEBP -> comprobarFinWebp(c);
            default -> throw new IllegalStateException("Formato sin revisión de final: " + formato);
        }
    }

    /** El JPEG termina en el marcador FF D9; se toleran ceros de relleno después. */
    private void comprobarFinJpeg(byte[] c) {
        int fin = c.length;
        while (fin > 0 && c[fin - 1] == 0) {
            fin--;
        }
        if (fin >= 2 && (c[fin - 2] & 0xFF) == 0xFF && (c[fin - 1] & 0xFF) == 0xD9) {
            return;
        }
        boolean hayMarcadorFinal = false;
        for (int i = 0; i + 1 < c.length; i++) {
            if ((c[i] & 0xFF) == 0xFF && (c[i + 1] & 0xFF) == 0xD9) {
                hayMarcadorFinal = true;
                break;
            }
        }
        throw new ImagenInvalidaException(hayMarcadorFinal ? Motivo.DATOS_EXTRA : Motivo.ILEGIBLE);
    }

    /** El PNG termina exactamente en el chunk IEND con su CRC. */
    private void comprobarFinPng(byte[] c) {
        int posicion = FIRMA_PNG.length;
        while (posicion + 12 <= c.length) {
            long largo = ((c[posicion] & 0xFFL) << 24) | ((c[posicion + 1] & 0xFFL) << 16)
                | ((c[posicion + 2] & 0xFFL) << 8) | (c[posicion + 3] & 0xFFL);
            boolean esIend = c[posicion + 4] == 'I' && c[posicion + 5] == 'E' && c[posicion + 6] == 'N'
                && c[posicion + 7] == 'D';
            long siguiente = posicion + 12L + largo;
            if (siguiente > c.length) {
                throw new ImagenInvalidaException(Motivo.ILEGIBLE);
            }
            if (esIend) {
                if (siguiente != c.length) {
                    throw new ImagenInvalidaException(Motivo.DATOS_EXTRA);
                }
                return;
            }
            posicion = (int) siguiente;
        }
        throw new ImagenInvalidaException(Motivo.ILEGIBLE);
    }

    /** En un WebP el tamaño del contenedor RIFF más 8 es el largo exacto del archivo. */
    private void comprobarFinWebp(byte[] c) {
        long declarado = (c[4] & 0xFFL) | ((c[5] & 0xFFL) << 8) | ((c[6] & 0xFFL) << 16) | ((c[7] & 0xFFL) << 24);
        long esperado = declarado + 8;
        if (esperado > c.length) {
            throw new ImagenInvalidaException(Motivo.ILEGIBLE);
        }
        if (esperado < c.length) {
            throw new ImagenInvalidaException(Motivo.DATOS_EXTRA);
        }
    }

    private static boolean empiezaCon(byte[] c, byte[] prefijo) {
        if (c.length < prefijo.length) {
            return false;
        }
        for (int i = 0; i < prefijo.length; i++) {
            if (c[i] != prefijo[i]) {
                return false;
            }
        }
        return true;
    }
}
