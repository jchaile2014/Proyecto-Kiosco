package org.kiosco.comun;

/** Error de carga con un mensaje pensado para mostrarle al usuario tal cual. */
public class MontoInvalidoException extends DatoInvalidoException {

    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
