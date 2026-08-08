package org.kiosco.logica;

import jakarta.persistence.*;

@Entity
@Table(name = "detalle_compras")
public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int codigo;

    @ManyToOne
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private int cantidad;
    private double precioCosto;

    public DetalleCompra() {
    }

    public DetalleCompra(Compra compra, Producto producto, int cantidad, double precioCosto) {
        this.compra = compra;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioCosto = precioCosto;
    }

    public int getCodigo() {
        return codigo;
    }

    public Compra getCompra() {
        return compra;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioCosto() {
        return precioCosto;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public void setPrecioCosto(double precioCosto) {
        this.precioCosto = precioCosto;
    }
}
