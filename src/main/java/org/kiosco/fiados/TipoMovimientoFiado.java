package org.kiosco.fiados;

public enum TipoMovimientoFiado {

    LLEVO("Llevó", true),
    INTERES("Interés", true),
    PAGO("Pagó", false);

    private final String etiqueta;
    private final boolean aumentaLaDeuda;

    TipoMovimientoFiado(String etiqueta, boolean aumentaLaDeuda) {
        this.etiqueta = etiqueta;
        this.aumentaLaDeuda = aumentaLaDeuda;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean isAumentaLaDeuda() {
        return aumentaLaDeuda;
    }
}
