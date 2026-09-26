package org.kiosco.pedidos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.kiosco.comun.DatoInvalidoException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Un pedido hecho a un proveedor. La fecha de llegada y el monto pueden no saberse al
 * pedirlo; el monto real se carga cuando llega la boleta.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    private static final DateTimeFormatter DIA_DE_LA_SEMANA = DateTimeFormatter.ofPattern("EEEE", Locale.of("es", "AR"));
    private static final DateTimeFormatter DIA_Y_MES = DateTimeFormatter.ofPattern("dd/MM");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "fecha_pedido", nullable = false)
    private LocalDate fechaPedido;

    @Column(length = 1000)
    private String detalle;

    @Column(name = "fecha_estimada")
    private LocalDate fechaEstimada;

    @Column(name = "monto_estimado", precision = 12, scale = 2)
    private BigDecimal montoEstimado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(name = "fecha_llegada")
    private LocalDate fechaLlegada;

    @Column(name = "monto_boleta", precision = 12, scale = 2)
    private BigDecimal montoBoleta;

    protected Pedido() {
    }

    public Pedido(Proveedor proveedor, LocalDate fechaPedido, String detalle,
                  LocalDate fechaEstimada, BigDecimal montoEstimado) {
        this.proveedor = proveedor;
        this.fechaPedido = fechaPedido;
        this.detalle = detalle;
        this.fechaEstimada = fechaEstimada;
        this.montoEstimado = montoEstimado;
    }

    public void marcarLlegada(LocalDate fecha, BigDecimal monto) {
        if (estado != EstadoPedido.PENDIENTE) {
            throw new DatoInvalidoException("Este pedido ya no está pendiente.");
        }
        this.estado = EstadoPedido.LLEGO;
        this.fechaLlegada = fecha;
        this.montoBoleta = monto;
    }

    void deshacerLlegada() {
        if (estado != EstadoPedido.LLEGO) {
            throw new DatoInvalidoException("Este pedido no figura como llegado.");
        }
        this.estado = EstadoPedido.PENDIENTE;
        this.fechaLlegada = null;
        this.montoBoleta = null;
    }

    void cancelar() {
        if (estado != EstadoPedido.PENDIENTE) {
            throw new DatoInvalidoException("Solo se puede cancelar un pedido pendiente.");
        }
        this.estado = EstadoPedido.CANCELADO;
    }

    /** Sin fecha, llega hoy o ya debería haber llegado: es para tener a mano en la caja del día. */
    public boolean esParaHoy(LocalDate hoy) {
        return fechaEstimada == null || !fechaEstimada.isAfter(hoy);
    }

    public boolean atrasado(LocalDate hoy) {
        return fechaEstimada != null && fechaEstimada.isBefore(hoy);
    }

    /** "Llega hoy", "Llega mañana", "Llega el jueves", "Atrasado 2 días", "Sin fecha"… */
    public String cuandoLlega(LocalDate hoy) {
        if (fechaEstimada == null) {
            return "Sin fecha";
        }
        long dias = ChronoUnit.DAYS.between(hoy, fechaEstimada);
        if (dias == 0) {
            return "Llega hoy";
        }
        if (dias == 1) {
            return "Llega mañana";
        }
        if (dias == -1) {
            return "Atrasado 1 día";
        }
        if (dias < 0) {
            return "Atrasado " + (-dias) + " días";
        }
        return dias < 7 ? "Llega el " + fechaEstimada.format(DIA_DE_LA_SEMANA) : "Llega el " + fechaEstimada.format(DIA_Y_MES);
    }

    public Long getId() {
        return id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public LocalDate getFechaPedido() {
        return fechaPedido;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDate getFechaEstimada() {
        return fechaEstimada;
    }

    public BigDecimal getMontoEstimado() {
        return montoEstimado;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public BigDecimal getMontoBoleta() {
        return montoBoleta;
    }
}
