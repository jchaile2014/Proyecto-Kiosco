package org.kiosco.productos;

public enum MotivoStock {

    VENTA("Venta"),
    INGRESO("Entró mercadería"),
    AJUSTE("Corrección");

    private final String etiqueta;

    MotivoStock(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
