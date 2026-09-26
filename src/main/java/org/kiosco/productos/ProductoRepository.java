package org.kiosco.productos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Por nombre o por código; con búsqueda vacía trae todos. */
    @Query("""
            select p from Producto p
            where p.activo = true
              and (:busqueda = '' or lower(p.nombre) like concat('%', lower(:busqueda), '%')
                   or lower(p.codigo) like concat('%', lower(:busqueda), '%'))
            order by lower(p.nombre)""")
    List<Producto> buscar(String busqueda);

    @Query("select p from Producto p where p.activo = true and lower(p.codigo) = lower(:codigo)")
    Optional<Producto> activoPorCodigo(String codigo);

    @Query("""
            select count(p) > 0 from Producto p
            where p.activo = true and lower(p.codigo) = lower(:codigo) and (:id is null or p.id <> :id)""")
    boolean existeOtroConCodigo(String codigo, Long id);
}
