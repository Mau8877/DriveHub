package com.drivehub.academico.features.horarios.dto;

import java.time.LocalDate;

/**
 * Resultado de bloquear o desbloquear un día completo: cuántas franjas cambiaron de estado (cero no es un error).
 */
public record DiaBloqueadoResponse(
        LocalDate fecha,
        long franjasAfectadas) {
}
