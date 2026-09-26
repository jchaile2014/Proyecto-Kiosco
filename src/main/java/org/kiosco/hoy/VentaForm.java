package org.kiosco.hoy;

/**
 * Lo que llega del formulario "Anotar venta". El monto viene como texto para aceptar "1.500,50".
 * Con un código de producto el monto puede quedar vacío: se usa el precio de lista.
 */
public class VentaForm {

    private String monto;
    private String descripcion;
    /** Opcional: si se vende un producto de la lista. */
    private String codigo;
    private String cantidad;

    boolean tieneCodigo() {
        return codigo != null && !codigo.isBlank();
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCantidad() {
        return cantidad;
    }

    public void setCantidad(String cantidad) {
        this.cantidad = cantidad;
    }

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
