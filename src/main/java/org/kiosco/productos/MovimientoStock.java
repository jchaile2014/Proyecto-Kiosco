package org.kiosco.productos;

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

import java.time.LocalDateTime;

/** Un cambio en el stock de un producto: positivo si entra, negativo si sale. */
@Entity
@Table(name = "movimiento_stock")
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @Column(nullable = false)
    private int cantidad;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MotivoStock motivo;

    /** Si fue una venta, su movimiento en la caja. */
    @Column(name = "movimiento_caja_id")
    private Long movimientoCajaId;

    protected MovimientoStock() {
    }

    MovimientoStock(Producto producto, LocalDateTime registradoEn, int cantidad, MotivoStock motivo, Long movimientoCajaId) {
        this.producto = producto;
        this.registradoEn = registradoEn;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.movimientoCajaId = movimientoCajaId;
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public LocalDateTime getRegistradoEn() {
        return registradoEn;
    }

    public int getCantidad() {
        return cantidad;
    }

    public MotivoStock getMotivo() {
        return motivo;
    }

    public Long getMovimientoCajaId() {
        return movimientoCajaId;
    }
}
