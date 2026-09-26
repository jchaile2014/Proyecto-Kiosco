package org.kiosco.caja;

import org.kiosco.cierre.CierreDiarioService;
import org.kiosco.comun.DatoInvalidoException;
import org.kiosco.comun.MontoPorNombre;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public CajaService(MovimientoCajaRepository movimientos, CierreDiarioService cierres,
                       ApplicationEventPublisher eventos, Clock clock) {
        this.movimientos = movimientos;
        this.cierres = cierres;
        this.eventos = eventos;
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

    /** Anota la salida de plata por la boleta de un pedido que llegó. */
    @Transactional
    public MovimientoCaja registrarPagoDePedido(LocalDate fecha, BigDecimal monto, String descripcion, Long pedidoId) {
        validar(fecha, monto);
        MovimientoCaja pago = new MovimientoCaja(fecha, LocalDateTime.now(clock),
                monto.setScale(2, RoundingMode.HALF_UP), limpiar(descripcion, 200), pedidoId);
        movimientos.save(pago);
        cierres.recalcularSiYaTermino(fecha);
        return pago;
    }

    /** Lo que paga un cliente de su fiado entra a la caja de ese día. */
    @Transactional
    public MovimientoCaja registrarCobroDeFiado(LocalDate fecha, BigDecimal monto, String descripcion) {
        return registrar(fecha, TipoMovimiento.COBRO_FIADO, monto, limpiar(descripcion, 200), null);
    }

    /** Se usa al borrar de la libreta un pago anotado por error. */
    @Transactional
    public void eliminarCobroDeFiado(Long movimientoId) {
        movimientos.findById(movimientoId)
                .filter(m -> m.getTipo() == TipoMovimiento.COBRO_FIADO)
                .ifPresent(cobro -> {
                    movimientos.delete(cobro);
                    cierres.recalcularSiYaTermino(cobro.getFecha());
                });
    }

    /** Se usa al deshacer la llegada de un pedido marcado por error. */
    @Transactional
    public void eliminarPagoDePedido(Long pedidoId) {
        movimientos.findByPedidoId(pedidoId).ifPresent(pago -> {
            movimientos.delete(pago);
            cierres.recalcularSiYaTermino(pago.getFecha());
        });
    }

    /** Borra un movimiento cargado por error y devuelve el día al que pertenecía. */
    @Transactional
    public LocalDate eliminar(Long id) {
        MovimientoCaja movimiento = movimientos.findById(id)
                .orElseThrow(() -> new DatoInvalidoException("Ese movimiento ya no existe."));
        // Si se borraran acá, el pedido quedaría "llegado" sin su pago o la libreta con un pago que no entró
        if (movimiento.getPedidoId() != null) {
            throw new DatoInvalidoException("Es el pago de un pedido: corregilo desde Pedidos con \"Deshacer\".");
        }
        if (movimiento.getTipo() == TipoMovimiento.COBRO_FIADO) {
            throw new DatoInvalidoException("Es el cobro de un fiado: corregilo desde la libreta del cliente en Fiados.");
        }
        eventos.publishEvent(new MovimientoCajaEliminado(id));
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

    /** En qué se fue la plata de los gastos, de mayor a menor. */
    @Transactional(readOnly = true)
    public List<MontoPorNombre> gastosPorCategoria(LocalDate desde, LocalDate hasta) {
        return movimientos.gastosPorCategoria(desde, hasta);
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
        validar(fecha, monto);
        MovimientoCaja movimiento = new MovimientoCaja(fecha, LocalDateTime.now(clock), tipo,
                monto.setScale(2, RoundingMode.HALF_UP), descripcion, categoria);
        movimientos.save(movimiento);
        cierres.recalcularSiYaTermino(fecha);
        return movimiento;
    }

    private void validar(LocalDate fecha, BigDecimal monto) {
        if (fecha.isAfter(LocalDate.now(clock))) {
            throw new DatoInvalidoException("No se puede anotar en un día que todavía no llegó.");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new DatoInvalidoException("El monto tiene que ser mayor a cero.");
        }
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
