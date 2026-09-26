package org.kiosco.productos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Un producto de la lista de precios. El stock es opcional: solo se lleva la cuenta en los
 * productos que tienen {@code controlaStock}; en el resto el número no se usa.
 */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Lo que se tipea (o se escanea) para encontrarlo: "7790895000997", "coca". */
    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "controla_stock", nullable = false)
    private boolean controlaStock;

    @Column(nullable = false)
    private int stock;

    /** Con este stock o menos, avisa que hay poco. Null: no avisa. */
    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(nullable = false)
    private boolean activo = true;

    protected Producto() {
    }

    Producto(String codigo, String nombre, BigDecimal precio, boolean controlaStock, int stock, Integer stockMinimo) {
        actualizar(codigo, nombre, precio, controlaStock, stockMinimo);
        this.stock = stock;
    }

    void actualizar(String codigo, String nombre, BigDecimal precio, boolean controlaStock, Integer stockMinimo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.controlaStock = controlaStock;
        this.stockMinimo = stockMinimo;
    }

    void sumarStock(int cantidad) {
        this.stock += cantidad;
    }

    void darDeBaja() {
        this.activo = false;
    }

    public boolean tienePocoStock() {
        return controlaStock && stockMinimo != null && stock <= stockMinimo;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public boolean isControlaStock() {
        return controlaStock;
    }

    public int getStock() {
        return stock;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }
}
