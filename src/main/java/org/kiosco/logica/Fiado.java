package org.kiosco.logica;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "fiados")
public class Fiado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int codigo;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    private String articulo;
    private double monto;
    private LocalDate fecha;

    public Fiado() {
    }

    public Fiado(Cliente cliente, String articulo, double monto, LocalDate fecha) {
        this.cliente = cliente;
        this.articulo = articulo;
        this.monto = monto;
        this.fecha = fecha;
    }

    public int getCodigo() {
        return codigo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getArticulo() {
        return articulo;
    }

    public double getMonto() {
        return monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public void setArticulo(String articulo) {
        this.articulo = articulo;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}