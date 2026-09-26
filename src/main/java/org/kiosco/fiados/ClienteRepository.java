package org.kiosco.fiados;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("select c from Cliente c where c.activo = true order by lower(c.nombre)")
    List<Cliente> activos();

    @Query("select c from Cliente c where c.activo = true and lower(c.nombre) = lower(:nombre)")
    Optional<Cliente> activoPorNombre(String nombre);

    @Query("""
            select count(c) > 0 from Cliente c
            where c.activo = true and lower(c.nombre) = lower(:nombre) and (:id is null or c.id <> :id)""")
    boolean existeOtroConNombre(String nombre, Long id);
}
