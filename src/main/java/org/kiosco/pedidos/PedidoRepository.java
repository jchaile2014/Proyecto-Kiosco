package org.kiosco.pedidos;

import org.kiosco.comun.MontoPorNombre;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Primero los que tienen fecha (los más próximos arriba), al final los que no se sabe cuándo llegan. */
    @Query("""
            select p from Pedido p join fetch p.proveedor
            where p.estado = org.kiosco.pedidos.EstadoPedido.PENDIENTE
            order by case when p.fechaEstimada is null then 1 else 0 end, p.fechaEstimada, p.fechaPedido, p.id""")
    List<Pedido> pendientes();

    @Query("""
            select new org.kiosco.comun.MontoPorNombre(p.proveedor.nombre, sum(p.montoBoleta))
            from Pedido p
            where p.estado = org.kiosco.pedidos.EstadoPedido.LLEGO and p.fechaLlegada between :desde and :hasta
            group by p.proveedor.nombre
            order by sum(p.montoBoleta) desc""")
    List<MontoPorNombre> pagadoPorProveedor(LocalDate desde, LocalDate hasta);

    @EntityGraph(attributePaths = "proveedor")
    List<Pedido> findTop10ByEstadoOrderByFechaLlegadaDescIdDesc(EstadoPedido estado);
}
