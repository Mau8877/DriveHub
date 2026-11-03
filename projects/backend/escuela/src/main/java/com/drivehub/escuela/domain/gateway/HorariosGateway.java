package com.drivehub.escuela.domain.gateway;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.drivehub.escuela.domain.entity.MomentoDelDia;

/**
 * Lo que Escuela le pide a Académico sobre las franjas de los instructores (CU11).
 *
 * * Todo se hace con el token del Administrador que opera: Académico vuelve a validar el rol y toma la escuela
 * * del token.
 * * Si Académico no responde, venció el tiempo de espera o contestó con un error, falla con
 * * `ServicioNoDisponibleException` (`ERR_ESC_20`).
 */
public interface HorariosGateway {

    /**
     * Las franjas de cada instructor de la escuela en ese día y momento del día.
     *
     * * La clave es el `codigo_usuario` de Identidad del instructor. Un instructor sin franjas no viene en el
     * * mapa.
     *
     * @param momento nulo es todo el día
     * @throws com.drivehub.escuela.common.exception.ServicioNoDisponibleException `ERR_ESC_20`
     */
    Map<UUID, ResumenFranjas> resumenDelDia(LocalDate fecha, MomentoDelDia momento);
}
