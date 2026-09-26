package org.kiosco.pedidos;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Primero los que tienen fecha (los más próximos arriba), al final los que no se sabe cuándo llegan. */
    @Query("""
            select p from Pedido p join fetch p.proveedor
            where p.estado = org.kiosco.pedidos.EstadoPedido.PENDIENTE
            order by case when p.fechaEstimada is null then 1 else 0 end, p.fechaEstimada, p.fechaPedido, p.id""")
    List<Pedido> pendientes();

    @EntityGraph(attributePaths = "proveedor")
    List<Pedido> findTop10ByEstadoOrderByFechaLlegadaDescIdDesc(EstadoPedido estado);
}
