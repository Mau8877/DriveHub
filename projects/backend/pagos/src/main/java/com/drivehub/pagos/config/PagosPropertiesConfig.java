package com.drivehub.pagos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra {@link PagosProperties} (el prefijo `app.pagos`) y {@link AlmacenProperties} (`app.pagos.almacen`)
 * como beans.
 */
@Configuration
@EnableConfigurationProperties({PagosProperties.class, AlmacenProperties.class})
public class PagosPropertiesConfig {
}
