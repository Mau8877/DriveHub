package com.drivehub.identidad.common.exception;

/**
 * Catálogo de códigos de error del servicio Identidad (prefijo IDE).
 *
 * * Cada código nuevo se documenta en docs/errors/identidad/ERRORES_IDENTIDAD.md
 * * en el mismo cambio (ver GUIA_ERRORES_BACKEND.md).
 */
public final class IdentidadErrorCodes {

    public static final String CORREO_EN_USO = "ERR_IDE_01";
    public static final String MENOR_DE_EDAD = "ERR_IDE_02";
    public static final String ESCUELA_NO_DISPONIBLE = "ERR_IDE_03";
    public static final String CREDENCIALES_INCORRECTAS = "ERR_IDE_04";
    public static final String CUENTA_BLOQUEADA = "ERR_IDE_05";
    public static final String CUENTA_INACTIVA = "ERR_IDE_06";
    public static final String ESCUELA_SUSPENDIDA = "ERR_IDE_07";
    public static final String PLATAFORMA_NO_PERMITIDA = "ERR_IDE_08";
    public static final String SESION_INVALIDA = "ERR_IDE_09";
    public static final String CORREO_YA_VERIFICADO = "ERR_IDE_10";
    public static final String ENLACE_INVALIDO = "ERR_IDE_11";
    public static final String ADMIN_YA_EXISTE_EN_ESCUELA = "ERR_IDE_12";
    public static final String ADMIN_EN_USO = "ERR_IDE_13";

    private IdentidadErrorCodes() {
    }
}
