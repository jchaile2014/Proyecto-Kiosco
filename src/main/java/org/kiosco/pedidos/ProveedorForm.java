package org.kiosco.pedidos;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

public class ProveedorForm {

    private String nombre;
    private String telefono;
    private Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);

    static ProveedorForm de(Proveedor proveedor) {
        ProveedorForm form = new ProveedorForm();
        form.nombre = proveedor.getNombre();
        form.telefono = proveedor.getTelefono();
        form.dias = EnumSet.noneOf(DayOfWeek.class);
        form.dias.addAll(proveedor.getDiasPreventista());
        return form;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Set<DayOfWeek> getDias() {
        return dias;
    }

    public void setDias(Set<DayOfWeek> dias) {
        this.dias = dias;
    }
}
