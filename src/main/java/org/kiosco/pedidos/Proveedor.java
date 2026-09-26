package org.kiosco.pedidos;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 40)
    private String telefono;

    /** Los días en que pasa el preventista a tomar el pedido. */
    @Convert(converter = DiasDeLaSemanaConverter.class)
    @Column(name = "dias_preventista", nullable = false, length = 80)
    private Set<DayOfWeek> diasPreventista = EnumSet.noneOf(DayOfWeek.class);

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("creadaEn, id")
    private List<NotaPedido> notas = new ArrayList<>();

    protected Proveedor() {
    }

    Proveedor(String nombre, String telefono, Set<DayOfWeek> diasPreventista) {
        actualizar(nombre, telefono, diasPreventista);
    }

    void actualizar(String nombre, String telefono, Set<DayOfWeek> diasPreventista) {
        this.nombre = nombre;
        this.telefono = telefono;
        // Siempre un conjunto nuevo: Hibernate detecta el cambio al comparar el valor convertido
        this.diasPreventista = diasPreventista == null || diasPreventista.isEmpty()
                ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(diasPreventista);
    }

    void darDeBaja() {
        this.activo = false;
    }

    void anotar(String texto, LocalDateTime ahora) {
        notas.add(new NotaPedido(this, texto, ahora));
    }

    void borrarNota(Long notaId) {
        notas.removeIf(nota -> nota.getId().equals(notaId));
    }

    /** Pasa las notas al pedido: devuelve sus textos y deja la lista vacía para la próxima vez. */
    List<String> vaciarNotas() {
        List<String> textos = notas.stream().map(NotaPedido::getTexto).toList();
        notas.clear();
        return textos;
    }

    public boolean vieneEl(DayOfWeek dia) {
        return diasPreventista.contains(dia);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public Set<DayOfWeek> getDiasPreventista() {
        return Collections.unmodifiableSet(diasPreventista);
    }

    public boolean isActivo() {
        return activo;
    }

    public List<NotaPedido> getNotas() {
        return Collections.unmodifiableList(notas);
    }
}
