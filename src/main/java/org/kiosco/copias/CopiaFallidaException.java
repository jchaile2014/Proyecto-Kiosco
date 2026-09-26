package org.kiosco.copias;

import org.kiosco.comun.DatoInvalidoException;

public class CopiaFallidaException extends DatoInvalidoException {

    public CopiaFallidaException(String mensaje) {
        super(mensaje);
    }
}
