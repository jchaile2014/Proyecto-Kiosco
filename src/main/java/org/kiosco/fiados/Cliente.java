package org.kiosco.fiados;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 40)
    private String telefono;

    @Column(nullable = false)
    private boolean activo = true;

    protected Cliente() {
    }

    Cliente(String nombre, String telefono) {
        actualizar(nombre, telefono);
    }

    void actualizar(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }

    void darDeBaja() {
        this.activo = false;
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

    public boolean isActivo() {
        return activo;
    }
}
