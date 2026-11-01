package com.drivehub.academico.features.horarios.dto;

import java.util.UUID;

/**
 * Cuántas franjas tiene un instructor en un día y una ventana de horas (CU11), para el Administrador.
 *
 * * `instructorCodigo` es el `codigo_usuario` de Identidad de la persona.
 * * `total` son todas sus franjas de la ventana; `libres` solo las `LIBRE` que todavía no empezaron. Lo que
 * * falta para llegar al total son las libres que ya pasaron, así que un instructor puede tener franjas y
 * * ninguna libre.
 */
public record ResumenFranjasInstructorResponse(UUID instructorCodigo, int total, int libres, int reservadas,
        int bloqueadas) {
}
