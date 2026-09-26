package org.kiosco.fiados;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un renglón de la libreta de fiados. Mientras la cuenta está abierta, {@code saldadoEn} es
 * null; cuando el cliente termina de pagar, todos sus renglones se marcan como saldados y
 * pasan al historial.
 */
@Entity
@Table(name = "movimiento_fiado")
public class MovimientoFiado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoMovimientoFiado tipo;

    @Column(length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Si es un pago, el cobro que entró a la caja. */
    @Column(name = "movimiento_caja_id")
    private Long movimientoCajaId;

    @Column(name = "saldado_en")
    private LocalDateTime saldadoEn;

    protected MovimientoFiado() {
    }

    public MovimientoFiado(Cliente cliente, LocalDate fecha, LocalDateTime registradoEn, TipoMovimientoFiado tipo,
                           String descripcion, BigDecimal monto, Long movimientoCajaId) {
        this.cliente = cliente;
        this.fecha = fecha;
        this.registradoEn = registradoEn;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.monto = monto;
        this.movimientoCajaId = movimientoCajaId;
    }

    public void saldar(LocalDateTime cuando) {
        this.saldadoEn = cuando;
    }

    void reabrir() {
        this.saldadoEn = null;
    }

    /** Positivo si aumenta lo que debe, negativo si es un pago. */
    public BigDecimal montoConSigno() {
        return tipo.isAumentaLaDeuda() ? monto : monto.negate();
    }

    public String getDetalle() {
        return descripcion != null ? descripcion : tipo.getEtiqueta();
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalDateTime getRegistradoEn() {
        return registradoEn;
    }

    public TipoMovimientoFiado getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public Long getMovimientoCajaId() {
        return movimientoCajaId;
    }

    public LocalDateTime getSaldadoEn() {
        return saldadoEn;
    }

    Long getClienteId() {
        return cliente.getId();
    }
}
