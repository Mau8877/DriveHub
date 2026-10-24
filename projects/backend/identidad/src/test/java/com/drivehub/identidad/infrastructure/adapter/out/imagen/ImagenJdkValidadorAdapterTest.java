package com.drivehub.identidad.infrastructure.adapter.out.imagen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.drivehub.identidad.domain.model.FormatoImagen;
import com.drivehub.identidad.domain.model.ImagenInvalidaException;
import com.drivehub.identidad.domain.model.ImagenInvalidaException.Motivo;
import com.drivehub.identidad.domain.port.out.ImagenValidada;

class ImagenJdkValidadorAdapterTest {

    // * WebP lossless de 1x1 píxel (34 bytes)
    private static final byte[] WEBP_1X1 = Base64.getDecoder().decode("UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==");

    private final ImagenJdkValidadorAdapter validador = new ImagenJdkValidadorAdapter(4000);

    private static byte[] imagen(String formato, int ancho, int alto) throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < ancho; x++) {
            imagen.setRGB(x, 0, 0x3366CC + x);
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, formato, salida);
        return salida.toByteArray();
    }

    private static byte[] conBytesAlFinal(byte[] base, byte... extra) {
        byte[] resultado = Arrays.copyOf(base, base.length + extra.length);
        System.arraycopy(extra, 0, resultado, base.length, extra.length);
        return resultado;
    }

    private void assertRechazo(byte[] contenido, Motivo motivo) {
        assertThatThrownBy(() -> validador.validar(contenido))
            .isInstanceOfSatisfying(ImagenInvalidaException.class, e -> assertThat(e.getMotivo()).isEqualTo(motivo));
    }

    @Test
    void validar_conJpegValido_devuelveFormatoYDimensiones() throws Exception {
        ImagenValidada r = validador.validar(imagen("jpg", 20, 10));

        assertThat(r).isEqualTo(new ImagenValidada(FormatoImagen.JPEG, 20, 10));
    }

    @Test
    void validar_conPngValido_devuelveFormatoYDimensiones() throws Exception {
        ImagenValidada r = validador.validar(imagen("png", 20, 10));

        assertThat(r).isEqualTo(new ImagenValidada(FormatoImagen.PNG, 20, 10));
    }

    @Test
    void validar_conWebpValido_devuelveFormatoYDimensiones() {
        ImagenValidada r = validador.validar(WEBP_1X1);

        assertThat(r).isEqualTo(new ImagenValidada(FormatoImagen.WEBP, 1, 1));
    }

    @Test
    void validar_conUnTextoOUnEjecutableRenombrado_lanzaFormato() {
        assertRechazo("esto no es una imagen".getBytes(), Motivo.FORMATO);
        assertRechazo(new byte[] {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0}, Motivo.FORMATO);
    }

    @Test
    void validar_conArregloVacio_lanzaFormato() {
        assertRechazo(new byte[0], Motivo.FORMATO);
    }

    @Test
    void validar_conPngTruncado_lanzaIlegible() throws Exception {
        byte[] png = imagen("png", 200, 200);

        assertRechazo(Arrays.copyOf(png, png.length / 2), Motivo.ILEGIBLE);
    }

    @Test
    void validar_conJpegTruncado_lanzaIlegible() throws Exception {
        byte[] jpg = imagen("jpg", 200, 200);

        assertRechazo(Arrays.copyOf(jpg, jpg.length / 2), Motivo.ILEGIBLE);
    }

    @Test
    void validar_conFirmaDePngPeroSinContenido_lanzaIlegible() {
        assertRechazo(new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3}, Motivo.ILEGIBLE);
    }

    @Test
    void validar_conBytesPegadosAlFinalDeUnPng_lanzaDatosExtra() throws Exception {
        assertRechazo(conBytesAlFinal(imagen("png", 20, 10), (byte) 'M', (byte) 'Z', (byte) 0x90), Motivo.DATOS_EXTRA);
    }

    @Test
    void validar_conBytesPegadosAlFinalDeUnJpeg_lanzaDatosExtra() throws Exception {
        assertRechazo(conBytesAlFinal(imagen("jpg", 20, 10), (byte) 'M', (byte) 'Z', (byte) 0x90), Motivo.DATOS_EXTRA);
    }

    @Test
    void validar_conBytesPegadosAlFinalDeUnWebp_lanzaDatosExtra() {
        assertRechazo(conBytesAlFinal(WEBP_1X1, (byte) 'M', (byte) 'Z'), Motivo.DATOS_EXTRA);
    }

    @Test
    void validar_conJpegConCerosDeRelleno_loAcepta() throws Exception {
        ImagenValidada r = validador.validar(conBytesAlFinal(imagen("jpg", 20, 10), (byte) 0, (byte) 0));

        assertThat(r.formato()).isEqualTo(FormatoImagen.JPEG);
    }

    @Test
    void validar_conMasPixelesDeLosPermitidos_lanzaDimensiones() throws Exception {
        assertRechazo(imagen("png", 4001, 1), Motivo.DIMENSIONES);
        assertThat(validador.validar(imagen("png", 4000, 1)).ancho()).isEqualTo(4000);
    }
}
