package org.kiosco.fiados;

import org.kiosco.caja.CajaService;
import org.kiosco.caja.MovimientoCaja;
import org.kiosco.comun.DatoInvalidoException;
import org.kiosco.comun.Formatos;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * La libreta de fiados. Lo que un cliente se lleva no toca la caja (no entró plata); lo que
 * paga, sí: entra como cobro de fiado en la caja de hoy.
 */
@Service
public class FiadoService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ClienteRepository clientes;
    private final MovimientoFiadoRepository renglones;
    private final CajaService caja;
    private final Formatos fmt;
    private final Clock clock;

    public FiadoService(ClienteRepository clientes, MovimientoFiadoRepository renglones, CajaService caja,
                        Formatos fmt, Clock clock) {
        this.clientes = clientes;
        this.renglones = renglones;
        this.caja = caja;
        this.fmt = fmt;
        this.clock = clock;
    }

    // --- Consultas ---

    /** Todos los clientes, primero los que más deben. */
    @Transactional(readOnly = true)
    public List<CuentaCliente> cuentas() {
        Map<Long, MovimientoFiadoRepository.SaldoAbierto> saldos = renglones.saldosAbiertos().stream()
                .collect(Collectors.toMap(MovimientoFiadoRepository.SaldoAbierto::getClienteId, Function.identity()));
        return clientes.activos().stream()
                .map(c -> CuentaCliente.de(c, saldos.get(c.getId())))
                .sorted(Comparator.comparing(CuentaCliente::saldo).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public CuentaCliente cuenta(Long clienteId) {
        Cliente cliente = cliente(clienteId);
        List<MovimientoFiado> abiertos = abiertos(clienteId);
        LocalDate desde = abiertos.isEmpty() ? null : abiertos.getFirst().getFecha();
        LocalDate ultimoPago = abiertos.stream()
                .filter(r -> r.getTipo() == TipoMovimientoFiado.PAGO)
                .map(MovimientoFiado::getFecha)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new CuentaCliente(cliente, saldo(abiertos), desde, ultimoPago);
    }

    /** Lo que le deben al kiosco entre todos los clientes. */
    @Transactional(readOnly = true)
    public BigDecimal totalEnLaCalle() {
        return renglones.saldosAbiertos().stream()
                .map(MovimientoFiadoRepository.SaldoAbierto::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** La libreta abierta con el saldo acumulado renglón por renglón. */
    @Transactional(readOnly = true)
    public List<RenglonLibreta> libreta(Long clienteId) {
        List<RenglonLibreta> libreta = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (MovimientoFiado renglon : abiertos(clienteId)) {
            acumulado = acumulado.add(renglon.montoConSigno());
            libreta.add(new RenglonLibreta(renglon, acumulado));
        }
        return libreta;
    }

    /** Las cuentas que ya se pagaron, la más reciente primero. */
    @Transactional(readOnly = true)
    public List<CuentaSaldada> historial(Long clienteId) {
        Map<LocalDateTime, List<MovimientoFiado>> porCuenta = renglones
                .findByClienteIdAndSaldadoEnIsNotNullOrderBySaldadoEnDescRegistradoEnAscIdAsc(clienteId).stream()
                .collect(Collectors.groupingBy(MovimientoFiado::getSaldadoEn, LinkedHashMap::new, Collectors.toList()));
        return porCuenta.entrySet().stream().map(e -> new CuentaSaldada(e.getKey(), e.getValue())).toList();
    }

    @Transactional(readOnly = true)
    public List<String> nombresDeClientes() {
        return clientes.activos().stream().map(Cliente::getNombre).toList();
    }

    @Transactional(readOnly = true)
    public Cliente cliente(Long id) {
        return clientes.findById(id)
                .filter(Cliente::isActivo)
                .orElseThrow(() -> new DatoInvalidoException("Ese cliente no existe."));
    }

    // --- Anotar ---

    /** Anota lo que se lleva alguien; si es la primera vez que lleva fiado, lo agrega como cliente. */
    @Transactional
    public MovimientoFiado anotar(String nombreCliente, String descripcion, BigDecimal monto) {
        String nombre = limpiar(nombreCliente, 80, "El nombre");
        if (nombre == null) {
            throw new DatoInvalidoException("Poné el nombre de quién se lo lleva.");
        }
        validarMonto(monto);
        Cliente cliente = clientes.activoPorNombre(nombre).orElseGet(() -> clientes.save(new Cliente(nombre, null)));
        return anotar(cliente, descripcion, monto);
    }

    @Transactional
    public MovimientoFiado anotar(Long clienteId, String descripcion, BigDecimal monto) {
        validarMonto(monto);
        return anotar(cliente(clienteId), descripcion, monto);
    }

    private MovimientoFiado anotar(Cliente cliente, String descripcion, BigDecimal monto) {
        return renglones.save(new MovimientoFiado(cliente, hoy(), ahora(), TipoMovimientoFiado.LLEVO,
                limpiar(descripcion, 200, "Lo que lleva"), monto.setScale(2, RoundingMode.HALF_UP), null));
    }

    // --- Pagos e interés ---

    /** Pagó una parte (o todo): la plata entra a la caja de hoy. */
    @Transactional
    public MovimientoFiado registrarPago(Long clienteId, BigDecimal monto) {
        validarMonto(monto);
        Cliente cliente = cliente(clienteId);
        BigDecimal saldo = saldo(abiertos(clienteId));
        if (saldo.signum() <= 0) {
            throw new DatoInvalidoException(cliente.getNombre() + " no debe nada.");
        }
        if (monto.compareTo(saldo) > 0) {
            throw new DatoInvalidoException("Debe " + fmt.plata(saldo) + ": el pago no puede ser mayor.");
        }
        MovimientoCaja cobro = caja.registrarCobroDeFiado(hoy(), monto, cliente.getNombre());
        MovimientoFiado pago = renglones.save(new MovimientoFiado(cliente, hoy(), ahora(), TipoMovimientoFiado.PAGO,
                null, cobro.getMonto(), cobro.getId()));
        saldarSiQuedoEnCero(clienteId);
        return pago;
    }

    /** "Ya pagó": cobra todo lo que debe y la cuenta pasa al historial. Devuelve cuánto pagó. */
    @Transactional
    public BigDecimal pagarTodo(Long clienteId) {
        BigDecimal saldo = saldo(abiertos(clienteId));
        registrarPago(clienteId, saldo);
        return saldo;
    }

    /** Suma un porcentaje sobre lo que debe ahora, redondeado a pesos enteros. */
    @Transactional
    public MovimientoFiado aplicarInteres(Long clienteId, BigDecimal porcentaje) {
        Cliente cliente = cliente(clienteId);
        BigDecimal saldo = saldo(abiertos(clienteId));
        if (saldo.signum() <= 0) {
            throw new DatoInvalidoException(cliente.getNombre() + " no debe nada.");
        }
        BigDecimal interes = saldo.multiply(porcentaje).divide(CIEN, 0, RoundingMode.HALF_UP);
        if (interes.signum() <= 0) {
            throw new DatoInvalidoException("Con ese porcentaje el interés da $ 0.");
        }
        String detalle = "Interés " + porcentaje.stripTrailingZeros().toPlainString().replace('.', ',')
                + " % sobre " + fmt.plata(saldo);
        return renglones.save(new MovimientoFiado(cliente, hoy(), ahora(), TipoMovimientoFiado.INTERES,
                detalle, interes.setScale(2), null));
    }

    // --- Correcciones ---

    /** Borra un renglón anotado por error. Si era un pago, también sale de la caja. */
    @Transactional
    public void borrarRenglon(Long clienteId, Long renglonId) {
        MovimientoFiado renglon = renglones.findById(renglonId)
                .filter(r -> r.getClienteId().equals(clienteId) && r.getSaldadoEn() == null)
                .orElseThrow(() -> new DatoInvalidoException("Ese renglón ya no se puede borrar."));
        List<MovimientoFiado> abiertos = abiertos(clienteId);
        if (saldo(abiertos).subtract(renglon.montoConSigno()).signum() < 0) {
            throw new DatoInvalidoException("No se puede borrar: ya pagó parte de eso. Borrá primero el pago.");
        }
        borrar(renglon);
        saldarSiQuedoEnCero(clienteId);
    }

    /**
     * Para cuando se marcó "ya pagó" por error: la última cuenta saldada vuelve a estar abierta
     * y el pago con que se cerró se borra de la caja.
     */
    @Transactional
    public void deshacerUltimaCuentaSaldada(Long clienteId) {
        List<CuentaSaldada> historial = historial(clienteId);
        if (historial.isEmpty()) {
            throw new DatoInvalidoException("No hay ninguna cuenta saldada para deshacer.");
        }
        List<MovimientoFiado> renglonesDeLaCuenta = historial.getFirst().renglones();
        MovimientoFiado pagoFinal = renglonesDeLaCuenta.stream()
                .filter(r -> r.getTipo() == TipoMovimientoFiado.PAGO)
                .max(Comparator.comparing(MovimientoFiado::getRegistradoEn).thenComparing(MovimientoFiado::getId))
                .orElseThrow(() -> new DatoInvalidoException("Esa cuenta no tiene un pago para deshacer."));
        renglonesDeLaCuenta.forEach(MovimientoFiado::reabrir);
        borrar(pagoFinal);
    }

    // --- Clientes ---

    @Transactional
    public Cliente guardarCliente(Long id, String nombre, String telefono) {
        String nombreLimpio = limpiar(nombre, 80, "El nombre");
        if (nombreLimpio == null) {
            throw new DatoInvalidoException("Poné el nombre del cliente.");
        }
        if (clientes.existeOtroConNombre(nombreLimpio, id)) {
            throw new DatoInvalidoException("Ya tenés un cliente que se llama " + nombreLimpio + ".");
        }
        String telefonoLimpio = limpiar(telefono, 40, "El teléfono");
        if (id == null) {
            return clientes.save(new Cliente(nombreLimpio, telefonoLimpio));
        }
        Cliente cliente = cliente(id);
        cliente.actualizar(nombreLimpio, telefonoLimpio);
        return cliente;
    }

    @Transactional
    public void darDeBaja(Long clienteId) {
        Cliente cliente = cliente(clienteId);
        BigDecimal saldo = saldo(abiertos(clienteId));
        if (saldo.signum() > 0) {
            throw new DatoInvalidoException(cliente.getNombre() + " todavía debe " + fmt.plata(saldo)
                    + ": no se puede sacar de la lista.");
        }
        cliente.darDeBaja();
    }

    // --- Internos ---

    /** Cuando la cuenta llega a cero, sus renglones pasan al historial y la libreta queda limpia. */
    private void saldarSiQuedoEnCero(Long clienteId) {
        List<MovimientoFiado> abiertos = abiertos(clienteId);
        if (!abiertos.isEmpty() && saldo(abiertos).signum() == 0) {
            LocalDateTime ahora = ahora();
            abiertos.forEach(r -> r.saldar(ahora));
        }
    }

    private void borrar(MovimientoFiado renglon) {
        renglones.delete(renglon);
        // Primero el renglón, que apunta al cobro de la caja; después el cobro
        renglones.flush();
        if (renglon.getMovimientoCajaId() != null) {
            caja.eliminarCobroDeFiado(renglon.getMovimientoCajaId());
        }
    }

    private List<MovimientoFiado> abiertos(Long clienteId) {
        return renglones.findByClienteIdAndSaldadoEnIsNullOrderByRegistradoEnAscIdAsc(clienteId);
    }

    private static BigDecimal saldo(List<MovimientoFiado> renglones) {
        return renglones.stream().map(MovimientoFiado::montoConSigno).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new DatoInvalidoException("El monto tiene que ser mayor a cero.");
        }
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    private static String limpiar(String texto, int largoMaximo, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.strip().replaceAll("\\s+", " ");
        if (limpio.length() > largoMaximo) {
            throw new DatoInvalidoException(campo + " es muy largo (máximo " + largoMaximo + " letras).");
        }
        return limpio;
    }
}
