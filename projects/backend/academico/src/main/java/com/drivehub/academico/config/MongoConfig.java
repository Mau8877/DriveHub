package com.drivehub.academico.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Habilita @CreatedDate/@LastModifiedDate en los documentos (BaseDocument).
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
