package org.kiosco.fiados;

/** "Anotar fiado": quién, qué se lleva y cuánto. En la libreta de un cliente, el nombre no se usa. */
public class FiadoForm {

    private String cliente;
    private String descripcion;
    private String monto;

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMonto() {
        return monto;
    }

    public void setMonto(String monto) {
        this.monto = monto;
    }
}
