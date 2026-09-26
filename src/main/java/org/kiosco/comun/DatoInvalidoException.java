package org.kiosco.comun;

/** Dato cargado que no se puede aceptar. El mensaje se muestra tal cual en pantalla. */
public class DatoInvalidoException extends RuntimeException {

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
