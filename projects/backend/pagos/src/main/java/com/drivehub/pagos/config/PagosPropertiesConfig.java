package com.drivehub.pagos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra {@link PagosProperties} como bean (el prefijo `app.pagos`).
 */
@Configuration
@EnableConfigurationProperties(PagosProperties.class)
public class PagosPropertiesConfig {
}
