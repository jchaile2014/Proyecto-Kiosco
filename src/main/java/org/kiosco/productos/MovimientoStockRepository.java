package org.kiosco.productos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    Optional<MovimientoStock> findByMovimientoCajaId(Long movimientoCajaId);

    List<MovimientoStock> findTop20ByProductoIdOrderByRegistradoEnDescIdDesc(Long productoId);
}
