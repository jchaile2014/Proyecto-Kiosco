package org.kiosco.caja;

public enum TipoMovimiento {

    VENTA("Venta", true),
    COBRO_FIADO("Cobro de fiado", true),
    PAGO_PEDIDO("Pago de pedido", false),
    GASTO("Gasto", false);

    private final String etiqueta;
    private final boolean entrada;

    TipoMovimiento(String etiqueta, boolean entrada) {
        this.etiqueta = etiqueta;
        this.entrada = entrada;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** true si la plata entra a la caja, false si sale. */
    public boolean isEntrada() {
        return entrada;
    }
}
