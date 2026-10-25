package com.drivehub.pagos.common.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * Base de los Models de Pagos que son de UNA escuela (multitenant): suma `escuelaCodigo` a lo de
 * {@link BaseModel}.
 *
 * * Es el `codigo_escuela` de Escuela, sin FK física (es otro servicio). Sale del claim `escuela`
 * * del token, nunca del cuerpo ni de la URL.
 * * Se asigna al construir el Model y no cambia: no hay setter. Una escuela no le pasa un plan a otra.
 * * El filtro por escuela NO es automático: cada consulta del repositorio lo pide explícito.
 * * Todo Model de Pagos que sea de una escuela la extiende (hoy `PlanPago`; inscripciones y pagos,
 * * cuando tengan su caso de uso).
 */
@Getter
@MappedSuperclass
public abstract class BaseEscuelaModel extends BaseModel {

    @Column(name = "escuela_codigo", updatable = false, nullable = false)
    private UUID escuelaCodigo;

    protected BaseEscuelaModel() {
        // * Constructor para JPA
    }

    protected BaseEscuelaModel(UUID escuelaCodigo) {
        this.escuelaCodigo = escuelaCodigo;
    }
}
