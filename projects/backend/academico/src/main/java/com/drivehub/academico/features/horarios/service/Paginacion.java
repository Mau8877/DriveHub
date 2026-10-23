package com.drivehub.academico.features.horarios.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Normaliza la paginación pedida por el cliente: `page` 1-indexed (mínimo 1) y `pageSize` entre 1 y 100.
 */
final class Paginacion {

    static final int PAGE_SIZE_MAXIMO = 100;

    private Paginacion() {
    }

    static PageRequest de(int page, int pageSize, Sort orden) {
        int pagina = Math.max(page, 1);
        int tamano = Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAXIMO);
        return PageRequest.of(pagina - 1, tamano, orden);
    }
}
