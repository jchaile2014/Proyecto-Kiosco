package org.kiosco.cierre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CierreDiarioRepository extends JpaRepository<CierreDiario, LocalDate> {

    @Query("select c.fecha from CierreDiario c where c.fecha between :desde and :hasta")
    List<LocalDate> fechasCerradasEntre(LocalDate desde, LocalDate hasta);

    List<CierreDiario> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /** Lo que fue quedando guardado día a día, sumado por mes. */
    @Query("""
            select year(c.fecha) as anio, month(c.fecha) as mes,
                   sum(c.ventas) as ventas, sum(c.cobrosFiado) as cobrosFiado,
                   sum(c.pagosPedidos) as pagosPedidos, sum(c.gastos) as gastos
            from CierreDiario c
            group by year(c.fecha), month(c.fecha)""")
    List<TotalMensual> totalesPorMes();

    interface TotalMensual {
        Integer getAnio();

        Integer getMes();

        BigDecimal getVentas();

        BigDecimal getCobrosFiado();

        BigDecimal getPagosPedidos();

        BigDecimal getGastos();
    }
}
