package org.kiosco.pedidos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    @Query("""
            select p from Proveedor p left join fetch p.notas n
            where p.activo = true
            order by lower(p.nombre), n.creadaEn, n.id""")
    List<Proveedor> activosConNotas();

    /** Con las notas ya cargadas: las pantallas las leen fuera de la transacción. */
    @Query("select p from Proveedor p left join fetch p.notas where p.id = :id and p.activo = true")
    Optional<Proveedor> activoConNotas(Long id);

    @Query("""
            select count(p) > 0 from Proveedor p
            where p.activo = true and lower(p.nombre) = lower(:nombre) and (:id is null or p.id <> :id)""")
    boolean existeOtroConNombre(String nombre, Long id);
}
