package org.kiosco.cierre;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.kiosco.caja.Totales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Foto de cómo terminó un día: se genera sola al pasar la medianoche y queda
 * guardada para los resúmenes mensuales.
 */
@Entity
@Table(name = "cierre_diario")
public class CierreDiario {

    @Id
    private LocalDate fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ventas;

    @Column(name = "cobros_fiado", nullable = false, precision = 12, scale = 2)
    private BigDecimal cobrosFiado;

    @Column(name = "pagos_pedidos", nullable = false, precision = 12, scale = 2)
    private BigDecimal pagosPedidos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gastos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(name = "cerrado_en", nullable = false)
    private LocalDateTime cerradoEn;

    protected CierreDiario() {
    }

    CierreDiario(LocalDate fecha) {
        this.fecha = fecha;
    }

    void actualizar(Totales totales, LocalDateTime ahora) {
        this.ventas = totales.ventas();
        this.cobrosFiado = totales.cobrosFiado();
        this.pagosPedidos = totales.pagosPedidos();
        this.gastos = totales.gastos();
        this.balance = totales.balance();
        this.cerradoEn = ahora;
    }

    public Totales totales() {
        return new Totales(ventas, cobrosFiado, pagosPedidos, gastos);
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getVentas() {
        return ventas;
    }

    public BigDecimal getCobrosFiado() {
        return cobrosFiado;
    }

    public BigDecimal getPagosPedidos() {
        return pagosPedidos;
    }

    public BigDecimal getGastos() {
        return gastos;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCerradoEn() {
        return cerradoEn;
    }
}
