package org.kiosco.pedidos;

import org.kiosco.caja.CajaService;
import org.kiosco.comun.DatoInvalidoException;
import org.kiosco.comun.MontoPorNombre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PedidoService {

    private final ProveedorRepository proveedores;
    private final PedidoRepository pedidos;
    private final CajaService caja;
    private final Clock clock;

    public PedidoService(ProveedorRepository proveedores, PedidoRepository pedidos, CajaService caja, Clock clock) {
        this.proveedores = proveedores;
        this.pedidos = pedidos;
        this.caja = caja;
        this.clock = clock;
    }

    // --- Proveedores ---

    @Transactional(readOnly = true)
    public List<Proveedor> proveedoresConNotas() {
        return proveedores.activosConNotas();
    }

    @Transactional(readOnly = true)
    public List<Proveedor> quienesVienenHoy() {
        DayOfWeek hoy = LocalDate.now(clock).getDayOfWeek();
        return proveedores.activosConNotas().stream().filter(p -> p.vieneEl(hoy)).toList();
    }

    @Transactional(readOnly = true)
    public Proveedor proveedor(Long id) {
        return proveedores.activoConNotas(id)
                .orElseThrow(() -> new DatoInvalidoException("Ese proveedor no existe."));
    }

    /** Crea un proveedor (id null) o actualiza uno existente. */
    @Transactional
    public Proveedor guardarProveedor(Long id, String nombre, String telefono, Set<DayOfWeek> diasPreventista) {
        String nombreLimpio = limpiar(nombre, 80, "El nombre");
        if (nombreLimpio == null) {
            throw new DatoInvalidoException("Poné el nombre del proveedor.");
        }
        if (proveedores.existeOtroConNombre(nombreLimpio, id)) {
            throw new DatoInvalidoException("Ya tenés un proveedor que se llama " + nombreLimpio + ".");
        }
        String telefonoLimpio = limpiar(telefono, 40, "El teléfono");
        if (id == null) {
            return proveedores.save(new Proveedor(nombreLimpio, telefonoLimpio, diasPreventista));
        }
        Proveedor proveedor = proveedor(id);
        proveedor.actualizar(nombreLimpio, telefonoLimpio, diasPreventista);
        return proveedor;
    }

    @Transactional
    public void darDeBaja(Long id) {
        proveedor(id).darDeBaja();
    }

    // --- Notas de qué pedir ---

    @Transactional
    public void anotar(Long proveedorId, String texto) {
        String limpio = limpiar(texto, 120, "La nota");
        if (limpio == null) {
            throw new DatoInvalidoException("Escribí qué hay que pedir.");
        }
        proveedor(proveedorId).anotar(limpio, LocalDateTime.now(clock));
    }

    @Transactional
    public void borrarNota(Long proveedorId, Long notaId) {
        proveedor(proveedorId).borrarNota(notaId);
    }

    // --- Pedidos ---

    /** Anota el pedido y vacía las notas del proveedor, que ya pasaron al pedido. */
    @Transactional
    public Pedido armarPedido(Long proveedorId, String detalle, LocalDate fechaEstimada, BigDecimal montoEstimado) {
        LocalDate hoy = LocalDate.now(clock);
        if (fechaEstimada != null && fechaEstimada.isBefore(hoy)) {
            throw new DatoInvalidoException("La fecha de llegada no puede ser anterior a hoy.");
        }
        Proveedor proveedor = proveedor(proveedorId);
        Pedido pedido = new Pedido(proveedor, hoy, limpiarDetalle(detalle), fechaEstimada,
                montoEstimado == null ? null : montoEstimado.setScale(2, RoundingMode.HALF_UP));
        proveedor.vaciarNotas();
        return pedidos.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<Pedido> pendientes() {
        return pedidos.pendientes();
    }

    /** Los que tienen que estar a mano en la caja de hoy: sin fecha, de hoy o atrasados. */
    @Transactional(readOnly = true)
    public List<Pedido> pendientesParaHoy() {
        LocalDate hoy = LocalDate.now(clock);
        return pedidos.pendientes().stream().filter(p -> p.esParaHoy(hoy)).toList();
    }

    @Transactional(readOnly = true)
    public List<Pedido> llegadosRecientes() {
        return pedidos.findTop10ByEstadoOrderByFechaLlegadaDescIdDesc(EstadoPedido.LLEGO);
    }

    /** Cuánto se le pagó a cada proveedor en boletas, de mayor a menor. */
    @Transactional(readOnly = true)
    public List<MontoPorNombre> pagadoPorProveedor(LocalDate desde, LocalDate hasta) {
        return pedidos.pagadoPorProveedor(desde, hasta);
    }

    /** Llegó el pedido: queda registrado y el monto de la boleta sale de la caja de hoy. */
    @Transactional
    public Pedido registrarLlegada(Long pedidoId, BigDecimal montoBoleta) {
        LocalDate hoy = LocalDate.now(clock);
        Pedido pedido = pedido(pedidoId);
        pedido.marcarLlegada(hoy, montoBoleta);
        caja.registrarPagoDePedido(hoy, montoBoleta, "Boleta de " + pedido.getProveedor().getNombre(), pedido.getId());
        return pedido;
    }

    /** Para cuando se marcó "llegó" por error: vuelve a pendiente y se borra el pago de la caja. */
    @Transactional
    public void deshacerLlegada(Long pedidoId) {
        pedido(pedidoId).deshacerLlegada();
        caja.eliminarPagoDePedido(pedidoId);
    }

    @Transactional
    public void cancelar(Long pedidoId) {
        pedido(pedidoId).cancelar();
    }

    private Pedido pedido(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new DatoInvalidoException("Ese pedido ya no existe."));
    }

    private static String limpiarDetalle(String detalle) {
        if (detalle == null || detalle.isBlank()) {
            return null;
        }
        String limpio = detalle.strip().replaceAll("\\R+", "\n");
        if (limpio.length() > 1000) {
            throw new DatoInvalidoException("El detalle del pedido es muy largo (máximo 1000 letras).");
        }
        return limpio;
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
