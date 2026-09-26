package org.kiosco.caja;

import org.kiosco.cierre.CierreDiarioService;
import org.kiosco.comun.DatoInvalidoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CajaService {

    static final List<String> CATEGORIAS_SUGERIDAS =
            List.of("Panadero", "Verdulería", "Servicios", "Alquiler", "Retiro personal", "Otros");

    private final MovimientoCajaRepository movimientos;
    private final CierreDiarioService cierres;
    private final Clock clock;

    public CajaService(MovimientoCajaRepository movimientos, CierreDiarioService cierres, Clock clock) {
        this.movimientos = movimientos;
        this.cierres = cierres;
        this.clock = clock;
    }

    @Transactional
    public MovimientoCaja registrarVenta(LocalDate fecha, BigDecimal monto, String descripcion) {
        return registrar(fecha, TipoMovimiento.VENTA, monto, limpiar(descripcion, 200), null);
    }

    @Transactional
    public MovimientoCaja registrarGasto(LocalDate fecha, BigDecimal monto, String categoria, String descripcion) {
        String categoriaLimpia = limpiar(categoria, 60);
        if (categoriaLimpia == null) {
            throw new DatoInvalidoException("Elegí o escribí en qué se gastó, por ejemplo Panadero.");
        }
        String conMayuscula = categoriaLimpia.substring(0, 1).toUpperCase(Locale.ROOT) + categoriaLimpia.substring(1);
        return registrar(fecha, TipoMovimiento.GASTO, monto, limpiar(descripcion, 200), conMayuscula);
    }

    /** Borra un movimiento cargado por error y devuelve el día al que pertenecía. */
    @Transactional
    public LocalDate eliminar(Long id) {
        MovimientoCaja movimiento = movimientos.findById(id)
                .orElseThrow(() -> new DatoInvalidoException("Ese movimiento ya no existe."));
        movimientos.delete(movimiento);
        cierres.recalcularSiYaTermino(movimiento.getFecha());
        return movimiento.getFecha();
    }

    @Transactional(readOnly = true)
    public List<MovimientoCaja> movimientosDel(LocalDate fecha) {
        return movimientos.findByFechaOrderByRegistradoEnDescIdDesc(fecha);
    }

    @Transactional(readOnly = true)
    public Totales totalesDel(LocalDate fecha) {
        return totalesEntre(fecha, fecha);
    }

    @Transactional(readOnly = true)
    public Totales totalesEntre(LocalDate desde, LocalDate hasta) {
        return Totales.desde(movimientos.totalesPorTipo(desde, hasta));
    }

    /** Las categorías sugeridas primero, después las que se fueron escribiendo a mano. */
    @Transactional(readOnly = true)
    public List<String> categoriasDeGasto() {
        Map<String, String> unicas = new LinkedHashMap<>();
        CATEGORIAS_SUGERIDAS.forEach(c -> unicas.put(c.toLowerCase(Locale.ROOT), c));
        List<String> usadas = new ArrayList<>(movimientos.categoriasDeGastoUsadas());
        usadas.sort(Comparator.comparing(c -> c.toLowerCase(Locale.ROOT)));
        usadas.forEach(c -> unicas.putIfAbsent(c.toLowerCase(Locale.ROOT), c));
        return List.copyOf(unicas.values());
    }

    private MovimientoCaja registrar(LocalDate fecha, TipoMovimiento tipo, BigDecimal monto,
                                     String descripcion, String categoria) {
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw new DatoInvalidoException("No se puede anotar en un día que todavía no llegó.");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new DatoInvalidoException("El monto tiene que ser mayor a cero.");
        }
        MovimientoCaja movimiento = new MovimientoCaja(fecha, LocalDateTime.now(clock), tipo,
                monto.setScale(2, RoundingMode.HALF_UP), descripcion, categoria);
        movimientos.save(movimiento);
        cierres.recalcularSiYaTermino(fecha);
        return movimiento;
    }

    private static String limpiar(String texto, int largoMaximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.strip().replaceAll("\\s+", " ");
        if (limpio.length() > largoMaximo) {
            throw new DatoInvalidoException("El texto es muy largo (máximo " + largoMaximo + " letras).");
        }
        return limpio;
    }
}
