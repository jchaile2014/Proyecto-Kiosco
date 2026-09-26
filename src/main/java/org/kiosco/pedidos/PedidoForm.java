package org.kiosco.pedidos;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Formulario "Armar pedido". Fecha y monto son opcionales: a veces no se saben. */
public class PedidoForm {

    private String detalle;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaEstimada;

    private String montoEstimado;

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public LocalDate getFechaEstimada() {
        return fechaEstimada;
    }

    public void setFechaEstimada(LocalDate fechaEstimada) {
        this.fechaEstimada = fechaEstimada;
    }

    public String getMontoEstimado() {
        return montoEstimado;
    }

    public void setMontoEstimado(String montoEstimado) {
        this.montoEstimado = montoEstimado;
    }
}
