package org.kiosco.pedidos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Algo que hay que acordarse de pedirle a un proveedor ("Sprite 2,25 L x6"). */
@Entity
@Table(name = "nota_pedido")
public class NotaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(nullable = false, length = 120)
    private String texto;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    protected NotaPedido() {
    }

    NotaPedido(Proveedor proveedor, String texto, LocalDateTime creadaEn) {
        this.proveedor = proveedor;
        this.texto = texto;
        this.creadaEn = creadaEn;
    }

    public Long getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }
}
