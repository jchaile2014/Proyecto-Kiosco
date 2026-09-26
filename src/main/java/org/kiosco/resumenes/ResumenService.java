package org.kiosco.resumenes;

import org.kiosco.caja.CajaService;
import org.kiosco.caja.Totales;
import org.kiosco.cierre.CierreDiario;
import org.kiosco.cierre.CierreDiarioRepository;
import org.kiosco.pedidos.PedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Los resúmenes salen de los cierres diarios que se fueron guardando. El día de hoy todavía
 * no está cerrado, así que se calcula en el momento.
 */
@Service
public class ResumenService {

    private final CierreDiarioRepository cierres;
    private final CajaService caja;
    private final PedidoService pedidos;
    private final Clock clock;

    public ResumenService(CierreDiarioRepository cierres, CajaService caja, PedidoService pedidos, Clock clock) {
        this.cierres = cierres;
        this.caja = caja;
        this.pedidos = pedidos;
        this.clock = clock;
    }

    /** Hoy y los 6 días anteriores. */
    @Transactional(readOnly = true)
    public Periodo ultimos7Dias() {
        LocalDate hoy = LocalDate.now(clock);
        return periodo(hoy.minusDays(6), hoy);
    }

    /** Los 7 días anteriores a los últimos 7, para comparar. */
    @Transactional(readOnly = true)
    public Totales semanaAnterior() {
        LocalDate hoy = LocalDate.now(clock);
        return sumar(dias(hoy.minusDays(13), hoy.minusDays(7)));
    }

    /** Un rango de días; si termina en el futuro, se corta en hoy. */
    @Transactional(readOnly = true)
    public Periodo periodo(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fin = hasta.isAfter(hoy) ? hoy : hasta;
        List<Periodo.Dia> dias = dias(desde, fin);
        return new Periodo(desde, fin, dias, sumar(dias),
                caja.gastosPorCategoria(desde, fin), pedidos.pagadoPorProveedor(desde, fin));
    }

    /** Todos los meses con movimientos, el más reciente primero. El actual es "lo que va del mes". */
    @Transactional(readOnly = true)
    public List<ResumenMes> meses() {
        LocalDate hoy = LocalDate.now(clock);
        YearMonth actual = YearMonth.from(hoy);
        Map<YearMonth, Totales> porMes = new TreeMap<>(Comparator.reverseOrder());
        for (CierreDiarioRepository.TotalMensual t : cierres.totalesPorMes()) {
            porMes.put(YearMonth.of(t.getAnio(), t.getMes()),
                    new Totales(t.getVentas(), t.getCobrosFiado(), t.getPagosPedidos(), t.getGastos()));
        }
        porMes.merge(actual, caja.totalesDel(hoy), Totales::mas);
        return porMes.entrySet().stream()
                .map(e -> new ResumenMes(e.getKey(), e.getValue(), e.getKey().equals(actual)))
                .toList();
    }

    private List<Periodo.Dia> dias(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        Map<LocalDate, CierreDiario> cerrados = cierres.findByFechaBetween(desde, hasta).stream()
                .collect(Collectors.toMap(CierreDiario::getFecha, Function.identity()));
        List<Periodo.Dia> dias = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            CierreDiario cierre = cerrados.get(dia);
            // Hoy (o un día que por algún motivo no se cerró) se calcula de los movimientos
            Totales totales = cierre != null && !dia.equals(hoy) ? cierre.totales() : caja.totalesDel(dia);
            dias.add(new Periodo.Dia(dia, totales));
        }
        return dias;
    }

    private static Totales sumar(List<Periodo.Dia> dias) {
        return dias.stream().map(Periodo.Dia::totales).reduce(Totales.CERO, Totales::mas);
    }
}
