package org.kiosco.productos;

import org.kiosco.caja.CajaService;
import org.kiosco.caja.MovimientoCaja;
import org.kiosco.caja.MovimientoCajaEliminado;
import org.kiosco.comun.DatoInvalidoException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lista de precios con stock opcional. La caja sigue funcionando por montos: el producto
 * solo completa el precio y, si controla stock, descuenta lo vendido.
 */
@Service
public class ProductoService {

    private static final int CANTIDAD_MAXIMA = 999;

    private final ProductoRepository productos;
    private final MovimientoStockRepository movimientosStock;
    private final CajaService caja;
    private final Clock clock;

    public ProductoService(ProductoRepository productos, MovimientoStockRepository movimientosStock,
                           CajaService caja, Clock clock) {
        this.productos = productos;
        this.movimientosStock = movimientosStock;
        this.caja = caja;
        this.clock = clock;
    }

    /** Lo que devuelve una venta con código: para avisar cuánto se cobró y cómo quedó el stock. */
    public record VentaDeProducto(MovimientoCaja venta, Producto producto, int cantidad) {
    }

    // --- Consultas ---

    @Transactional(readOnly = true)
    public List<Producto> buscar(String busqueda) {
        return productos.buscar(busqueda == null ? "" : busqueda.strip());
    }

    @Transactional(readOnly = true)
    public List<Producto> conPocoStock() {
        return productos.buscar("").stream().filter(Producto::tienePocoStock).toList();
    }

    @Transactional(readOnly = true)
    public boolean existeCodigo(String codigo) {
        return productos.activoPorCodigo(codigo).isPresent();
    }

    @Transactional(readOnly = true)
    public Producto producto(Long id) {
        return productos.findById(id)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new DatoInvalidoException("Ese producto no existe."));
    }

    @Transactional(readOnly = true)
    public List<MovimientoStock> historialDeStock(Long productoId) {
        return movimientosStock.findTop20ByProductoIdOrderByRegistradoEnDescIdDesc(productoId);
    }

    // --- Vender ---

    /**
     * Venta de un producto por código. Si no se escribe monto, se cobra el precio de lista
     * por la cantidad. Si el producto controla stock, se descuenta (puede quedar negativo:
     * si se vendió es que había, y el aviso sirve para corregir el número).
     */
    @Transactional
    public VentaDeProducto vender(String codigo, int cantidad, LocalDate fecha, BigDecimal monto, String descripcion) {
        if (cantidad < 1 || cantidad > CANTIDAD_MAXIMA) {
            throw new DatoInvalidoException("La cantidad tiene que ser entre 1 y " + CANTIDAD_MAXIMA + ".");
        }
        String codigoLimpio = codigo == null ? "" : codigo.strip();
        Producto producto = productos.activoPorCodigo(codigoLimpio)
                .orElseThrow(() -> new DatoInvalidoException("No hay ningún producto con el código " + codigoLimpio + "."));
        BigDecimal cobrado = monto != null ? monto : producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
        String detalle = descripcion != null && !descripcion.isBlank() ? descripcion
                : (cantidad > 1 ? cantidad + " × " : "") + producto.getNombre();

        MovimientoCaja venta = caja.registrarVenta(fecha, cobrado, detalle);
        if (producto.isControlaStock()) {
            producto.sumarStock(-cantidad);
            movimientosStock.save(new MovimientoStock(producto, ahora(), -cantidad, MotivoStock.VENTA, venta.getId()));
        }
        return new VentaDeProducto(venta, producto, cantidad);
    }

    /** Si se borra de la caja una venta con producto, el stock vuelve. */
    @EventListener
    @Transactional
    public void alEliminarMovimientoDeCaja(MovimientoCajaEliminado evento) {
        movimientosStock.findByMovimientoCajaId(evento.movimientoId()).ifPresent(movimiento -> {
            movimiento.getProducto().sumarStock(-movimiento.getCantidad());
            movimientosStock.delete(movimiento);
            // El movimiento de stock apunta al de caja, que se borra a continuación
            movimientosStock.flush();
        });
    }

    // --- Stock ---

    /** Entró mercadería. */
    @Transactional
    public Producto sumarStock(Long productoId, int cantidad) {
        if (cantidad < 1 || cantidad > 99_999) {
            throw new DatoInvalidoException("Poné cuántas unidades entraron.");
        }
        Producto producto = producto(productoId);
        if (!producto.isControlaStock()) {
            throw new DatoInvalidoException(producto.getNombre() + " no lleva control de stock.");
        }
        producto.sumarStock(cantidad);
        movimientosStock.save(new MovimientoStock(producto, ahora(), cantidad, MotivoStock.INGRESO, null));
        return producto;
    }

    // --- Alta y edición ---

    /**
     * Crea (id null) o actualiza un producto. Si controla stock y el número cambió, queda
     * registrado como corrección.
     */
    @Transactional
    public Producto guardar(Long id, String codigo, String nombre, BigDecimal precio,
                            boolean controlaStock, Integer stock, Integer stockMinimo) {
        String codigoLimpio = limpiar(codigo, 40);
        String nombreLimpio = limpiar(nombre, 100);
        if (codigoLimpio == null) {
            throw new DatoInvalidoException("Poné un código: el del código de barras o uno corto que te acuerdes, como \"coca\".");
        }
        if (codigoLimpio.contains(" ")) {
            throw new DatoInvalidoException("El código no puede tener espacios.");
        }
        if (nombreLimpio == null) {
            throw new DatoInvalidoException("Poné el nombre del producto.");
        }
        if (precio == null || precio.signum() <= 0) {
            throw new DatoInvalidoException("El precio tiene que ser mayor a cero.");
        }
        if (productos.existeOtroConCodigo(codigoLimpio, id)) {
            throw new DatoInvalidoException("Ya hay otro producto con el código " + codigoLimpio + ".");
        }
        if (stockMinimo != null && stockMinimo < 0) {
            throw new DatoInvalidoException("El stock mínimo no puede ser negativo.");
        }
        BigDecimal precioLimpio = precio.setScale(2, RoundingMode.HALF_UP);
        int stockNuevo = controlaStock && stock != null ? stock : 0;

        if (id == null) {
            Producto producto = productos.save(new Producto(codigoLimpio, nombreLimpio, precioLimpio, controlaStock,
                    stockNuevo, controlaStock ? stockMinimo : null));
            if (stockNuevo != 0) {
                movimientosStock.save(new MovimientoStock(producto, ahora(), stockNuevo, MotivoStock.AJUSTE, null));
            }
            return producto;
        }
        Producto producto = producto(id);
        producto.actualizar(codigoLimpio, nombreLimpio, precioLimpio, controlaStock, controlaStock ? stockMinimo : null);
        if (controlaStock && stock != null && stock != producto.getStock()) {
            int diferencia = stock - producto.getStock();
            producto.sumarStock(diferencia);
            movimientosStock.save(new MovimientoStock(producto, ahora(), diferencia, MotivoStock.AJUSTE, null));
        }
        return producto;
    }

    @Transactional
    public void darDeBaja(Long id) {
        producto(id).darDeBaja();
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
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
