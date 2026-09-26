package org.kiosco.fiados;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MovimientoFiadoRepository extends JpaRepository<MovimientoFiado, Long> {

    /** La libreta abierta, en el orden en que se fue anotando. */
    List<MovimientoFiado> findByClienteIdAndSaldadoEnIsNullOrderByRegistradoEnAscIdAsc(Long clienteId);

    List<MovimientoFiado> findByClienteIdAndSaldadoEnIsNotNullOrderBySaldadoEnDescRegistradoEnAscIdAsc(Long clienteId);

    @Query("""
            select m.cliente.id as clienteId,
                   sum(case when m.tipo = org.kiosco.fiados.TipoMovimientoFiado.PAGO then -m.monto else m.monto end) as saldo,
                   min(m.fecha) as desde,
                   max(case when m.tipo = org.kiosco.fiados.TipoMovimientoFiado.PAGO then m.fecha end) as ultimoPago
            from MovimientoFiado m
            where m.saldadoEn is null
            group by m.cliente.id""")
    List<SaldoAbierto> saldosAbiertos();

    interface SaldoAbierto {
        Long getClienteId();

        BigDecimal getSaldo();

        LocalDate getDesde();

        LocalDate getUltimoPago();
    }
}
