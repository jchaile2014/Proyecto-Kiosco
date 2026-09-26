package org.kiosco.caja;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un renglón del libro de caja: cada peso que entra o sale queda anotado acá.
 * El monto siempre es positivo; si suma o resta lo decide el {@link TipoMovimiento}.
 */
@Entity
@Table(name = "movimiento_caja")
public class MovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Día de caja al que pertenece (puede ser anterior al día en que se cargó). */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 200)
    private String descripcion;

    @Column(length = 60)
    private String categoria;

    protected MovimientoCaja() {
    }

    public MovimientoCaja(LocalDate fecha, LocalDateTime registradoEn, TipoMovimiento tipo,
                          BigDecimal monto, String descripcion, String categoria) {
        this.fecha = fecha;
        this.registradoEn = registradoEn;
        this.tipo = tipo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.categoria = categoria;
    }

    /** Texto para mostrar en listas: la categoría del gasto y/o el detalle que se anotó. */
    public String getDetalle() {
        if (categoria != null && descripcion != null) {
            return categoria + " · " + descripcion;
        }
        if (categoria != null) {
            return categoria;
        }
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

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCategoria() {
        return categoria;
    }
}
