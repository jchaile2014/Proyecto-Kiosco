package org.kiosco.caja;

/**
 * Para el dueño del kiosco, todo lo que sale es un pedido: la boleta de un pedido anotado
 * (PAGO_PEDIDO) o algo que se pagó en el momento, como el panadero (GASTO). Por eso los dos
 * se muestran como "Pedido"; internamente siguen separados porque se cargan distinto.
 */
public enum TipoMovimiento {

    VENTA("Venta", true),
    COBRO_FIADO("Cobro de fiado", true),
    PAGO_PEDIDO("Pedido", false),
    GASTO("Pedido", false);

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
