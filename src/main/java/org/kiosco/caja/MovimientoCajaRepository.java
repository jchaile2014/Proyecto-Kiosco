package org.kiosco.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {

    List<MovimientoCaja> findByFechaOrderByRegistradoEnDescIdDesc(LocalDate fecha);

    Optional<MovimientoCaja> findByPedidoId(Long pedidoId);

    @Query("""
            select m.tipo as tipo, sum(m.monto) as total
            from MovimientoCaja m
            where m.fecha between :desde and :hasta
            group by m.tipo""")
    List<TotalPorTipo> totalesPorTipo(LocalDate desde, LocalDate hasta);

    @Query("select min(m.fecha) from MovimientoCaja m")
    Optional<LocalDate> primeraFecha();

    @Query("""
            select distinct m.categoria from MovimientoCaja m
            where m.tipo = org.kiosco.caja.TipoMovimiento.GASTO and m.categoria is not null""")
    List<String> categoriasDeGastoUsadas();

    interface TotalPorTipo {
        TipoMovimiento getTipo();

        BigDecimal getTotal();
    }
}
