package com.drivehub.pagos.features.pagos.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.drivehub.pagos.config.PagosProperties;

/**
 * La página a la que vuelve el estudiante al terminar (o cancelar) el pago en la pasarela (CU26). Es la
 * `success_url` y la `cancel_url` de la sesión de cobro: la pasarela exige direcciones `https` y la app no
 * puede serlo, así que esta página sirve de puente.
 *
 * * Es pública (sin sesión) y es la única respuesta HTML de la API: la lista de excepciones al sobre de
 * * RESPONSES_BACKEND.md la menciona.
 * * Es FIJA: no refleja ningún parámetro de la dirección (la pasarela puede agregar los suyos), así que no hay
 * * nada que inyectar. Lo único que lleva de afuera es el enlace a la app, que sale de la configuración
 * * (`PAGOS_ENLACE_APP`, no de la petición) y se escapa para HTML.
 * * No confirma nada: el pago se confirma solo con el aviso firmado de la pasarela (CU27). La app, al volver,
 * * consulta el estado; el enlace de abajo es una ayuda para abrirla, no un requisito.
 */
@RestController
@RequestMapping("/api/v1/pagos/retorno")
public class RetornoPagoController {

    private static final String PLANTILLA = """
        <!doctype html>
        <html lang="es">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>DriveHub</title>
        <style>
        body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;
        background:#F1F5F9;color:#1E293B;font-family:system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
        main{max-width:22rem;margin:1rem;padding:2rem 1.5rem;background:#fff;border-radius:1rem;
        box-shadow:0 4px 24px rgba(30,41,59,.12);text-align:center}
        h1{margin:0 0 .75rem;font-size:1.35rem}
        p{margin:0 0 1.5rem;line-height:1.5;color:#475569}
        a{display:inline-block;padding:.8rem 1.5rem;border-radius:.75rem;background:#F59E0B;color:#1E293B;
        font-weight:600;text-decoration:none}
        </style>
        </head>
        <body>
        <main>
        <h1>Vuelve a DriveHub</h1>
        <p>Si ya pagaste, tus horas aparecerán en la aplicación en unos segundos.
        Si cancelaste, no se cobró nada.</p>
        <a href="%s">Volver a la aplicación</a>
        </main>
        </body>
        </html>
        """;

    private final String pagina;

    public RetornoPagoController(PagosProperties propiedades) {
        this.pagina = PLANTILLA.formatted(HtmlUtils.htmlEscape(propiedades.pasarela().enlaceApp()));
    }

    @GetMapping(produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> retorno() {
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
            .cacheControl(CacheControl.noStore())
            .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")
            .body(pagina);
    }
}
