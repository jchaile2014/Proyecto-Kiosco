package org.kiosco.hoy;

/** Lo que llega del formulario "Anotar venta". El monto viene como texto para aceptar "1.500,50". */
public class VentaForm {

    private String monto;
    private String descripcion;

    public String getMonto() {
        return monto;
    }

    public void setMonto(String monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
