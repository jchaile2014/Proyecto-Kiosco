package org.kiosco.demo;

import org.kiosco.caja.CajaService;
import org.kiosco.caja.MovimientoCaja;
import org.kiosco.caja.MovimientoCajaRepository;
import org.kiosco.caja.TipoMovimiento;
import org.kiosco.fiados.Cliente;
import org.kiosco.fiados.FiadoService;
import org.kiosco.fiados.MovimientoFiado;
import org.kiosco.fiados.MovimientoFiadoRepository;
import org.kiosco.fiados.TipoMovimientoFiado;
import org.kiosco.pedidos.Pedido;
import org.kiosco.pedidos.PedidoRepository;
import org.kiosco.pedidos.PedidoService;
import org.kiosco.pedidos.Proveedor;
import org.kiosco.productos.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

/**
 * Con el perfil "demo" la app arranca con una base en memoria y datos inventados: dos meses
 * de movimientos, proveedores con pedidos, clientes con fiado y una lista de precios. Sirve
 * para probarla sin instalar MySQL y para sacar capturas sin datos reales.
 */
@Component
@Profile("demo")
class DatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);
    private static final String[] DETALLES_VENTA =
            {null, null, null, "Gaseosa y alfajor", "Cigarrillos", "Pan y leche", "Golosinas", "Fiambre", "Yerba y azúcar"};

    private final MovimientoCajaRepository movimientos;
    private final CajaService caja;
    private final PedidoService pedidos;
    private final PedidoRepository pedidoRepository;
    private final FiadoService fiados;
    private final MovimientoFiadoRepository renglonesFiado;
    private final ProductoService productos;
    private final Clock clock;

    DatosDemo(MovimientoCajaRepository movimientos, CajaService caja, PedidoService pedidos,
              PedidoRepository pedidoRepository, FiadoService fiados, MovimientoFiadoRepository renglonesFiado,
              ProductoService productos, Clock clock) {
        this.movimientos = movimientos;
        this.caja = caja;
        this.pedidos = pedidos;
        this.pedidoRepository = pedidoRepository;
        this.fiados = fiados;
        this.renglonesFiado = renglonesFiado;
        this.productos = productos;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (movimientos.count() > 0) {
            return;
        }
        Random azar = new Random(42);
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<MovimientoCaja> nuevos = new ArrayList<>();

        for (LocalDate dia = ahora.toLocalDate().minusDays(60); !dia.isAfter(ahora.toLocalDate()); dia = dia.plusDays(1)) {
            boolean domingo = dia.getDayOfWeek() == DayOfWeek.SUNDAY;
            int ventas = domingo ? 8 + azar.nextInt(6) : 14 + azar.nextInt(10);
            for (int i = 0; i < ventas; i++) {
                String detalle = DETALLES_VENTA[azar.nextInt(DETALLES_VENTA.length)];
                agregar(nuevos, dia, azar, ahora, TipoMovimiento.VENTA, plata(azar, 300, 9000), detalle, null);
            }
            if (!domingo) {
                agregar(nuevos, dia, azar, ahora, TipoMovimiento.GASTO, plata(azar, 8000, 15000), null, "Panadero");
            }
            if (azar.nextInt(4) == 0) {
                agregar(nuevos, dia, azar, ahora, TipoMovimiento.GASTO, plata(azar, 5000, 12000), "Papa, tomate y cebolla", "Verdulería");
            }
            if (dia.getDayOfMonth() == 10) {
                agregar(nuevos, dia, azar, ahora, TipoMovimiento.GASTO, new BigDecimal("48500"), "Luz", "Servicios");
            }
        }
        movimientos.saveAll(nuevos);
        cargarPedidos(azar, ahora.toLocalDate());
        cargarFiados(ahora.toLocalDate());
        cargarProductos();
        log.info("Modo demo: {} movimientos de ejemplo cargados", movimientos.count());
    }

    private void cargarPedidos(Random azar, LocalDate hoy) {
        // Coca-Cola siempre "viene hoy", para que se vea el aviso en la pantalla principal
        Proveedor coca = pedidos.guardarProveedor(null, "Coca-Cola", "11 4000-1234",
                EnumSet.of(hoy.getDayOfWeek(), hoy.getDayOfWeek().plus(3)));
        Proveedor sur = pedidos.guardarProveedor(null, "Distribuidora Sur", null, EnumSet.of(DayOfWeek.MONDAY));
        Proveedor lacteos = pedidos.guardarProveedor(null, "Lácteos del Oeste", null, EnumSet.of(DayOfWeek.WEDNESDAY));
        Proveedor golosinas = pedidos.guardarProveedor(null, "Golosinas Mayorista", "11 5555-0000", EnumSet.of(DayOfWeek.THURSDAY));

        pedidos.anotar(coca.getId(), "Sprite 2,25 L x6");
        pedidos.anotar(coca.getId(), "Agua sin gas 500 ml x12");
        pedidos.anotar(sur.getId(), "Galletitas surtidas x10");
        pedidos.anotar(sur.getId(), "Yerba 1 kg x10");

        for (LocalDate dia = hoy.minusDays(60); dia.isBefore(hoy); dia = dia.plusDays(1)) {
            switch (dia.getDayOfWeek()) {
                case FRIDAY -> llego(coca, dia, plata(azar, 60000, 90000));
                case MONDAY -> llego(sur, dia, plata(azar, 40000, 70000));
                case WEDNESDAY -> llego(lacteos, dia, plata(azar, 20000, 35000));
                default -> {
                }
            }
        }

        pedidoRepository.save(new Pedido(sur, hoy.minusDays(1), "Fideos x20\nArroz x10", hoy, new BigDecimal("45000")));
        pedidoRepository.save(new Pedido(lacteos, hoy.minusDays(2), "Leche x24\nYogur x12", null, null));
        pedidoRepository.save(new Pedido(golosinas, hoy.minusDays(5), "Alfajores surtidos", hoy.minusDays(2), null));
        pedidoRepository.save(new Pedido(coca, hoy, "Coca-Cola 2,25 L x12", hoy.plusDays(1), new BigDecimal("80000")));
    }

    private void llego(Proveedor proveedor, LocalDate dia, BigDecimal boleta) {
        Pedido pedido = new Pedido(proveedor, dia.minusDays(1), null, dia, boleta);
        pedido.marcarLlegada(dia, boleta);
        pedido = pedidoRepository.save(pedido);
        caja.registrarPagoDePedido(dia, boleta, "Boleta de " + proveedor.getNombre(), pedido.getId());
    }

    private void cargarFiados(LocalDate hoy) {
        Cliente marta = fiados.guardarCliente(null, "Marta Gómez", "11 4567-8910");
        Cliente carlos = fiados.guardarCliente(null, "Carlos del taller", null);
        Cliente lucia = fiados.guardarCliente(null, "Lucía (3° B)", null);
        Cliente roberto = fiados.guardarCliente(null, "Roberto", null);

        llevo(marta, hoy.minusDays(9), "Pan y leche", 2300);
        llevo(marta, hoy.minusDays(7), "Fiambre y queso", 4500);
        pago(marta, hoy.minusDays(4), 5000);
        llevo(marta, hoy.minusDays(2), "Cigarrillos", 3800);
        llevo(marta, hoy, "Gaseosa 2,25 L", 2600);

        // Hace más de un mes que debe y ya se le cobró interés
        llevo(carlos, hoy.minusDays(40), "Yerba y azúcar", 6200);
        llevo(carlos, hoy.minusDays(33), "Galletitas y jugo", 3900);
        llevo(carlos, hoy.minusDays(26), "Fiambre", 5400);
        renglon(carlos, hoy.minusDays(10), TipoMovimientoFiado.INTERES, "Interés 10 % sobre $ 15.500", 1550, null);

        // Una cuenta que ya pagó entera y otra nueva
        List<MovimientoFiado> saldada = List.of(
                llevo(lucia, hoy.minusDays(20), "Pañales", 9800),
                llevo(lucia, hoy.minusDays(16), "Leche y galletitas", 3100),
                pago(lucia, hoy.minusDays(12), 12900));
        saldada.forEach(r -> r.saldar(hoy.minusDays(12).atTime(18, 0)));
        renglonesFiado.saveAll(saldada);
        llevo(lucia, hoy.minusDays(1), "Leche x2", 2400);

        // Pagó hoy: aparece en la caja del día como fiado cobrado
        List<MovimientoFiado> hoyPago = List.of(
                llevo(roberto, hoy.minusDays(3), "Carbón y hielo", 4200),
                pago(roberto, hoy, 4200));
        hoyPago.forEach(r -> r.saldar(hoy.atTime(9, 30)));
        renglonesFiado.saveAll(hoyPago);
    }

    private void cargarProductos() {
        productos.guardar(null, "7790895000997", "Coca-Cola 2,25 L", new BigDecimal("3200"), true, 18, 6);
        productos.guardar(null, "7790040613409", "Alfajor triple", new BigDecimal("1500"), true, 4, 5);
        productos.guardar(null, "yerba", "Yerba 1 kg", new BigDecimal("4200"), true, 9, 3);
        productos.guardar(null, "pan", "Pan por kilo", new BigDecimal("2500"), false, null, null);
        productos.guardar(null, "hielo", "Bolsa de hielo 2 kg", new BigDecimal("1800"), false, null, null);
    }

    private MovimientoFiado llevo(Cliente cliente, LocalDate dia, String detalle, long monto) {
        return renglon(cliente, dia, TipoMovimientoFiado.LLEVO, detalle, monto, null);
    }

    private MovimientoFiado pago(Cliente cliente, LocalDate dia, long monto) {
        MovimientoCaja cobro = caja.registrarCobroDeFiado(dia, BigDecimal.valueOf(monto), cliente.getNombre());
        return renglon(cliente, dia, TipoMovimientoFiado.PAGO, null, monto, cobro.getId());
    }

    private MovimientoFiado renglon(Cliente cliente, LocalDate dia, TipoMovimientoFiado tipo, String detalle,
                                    long monto, Long cobroId) {
        return renglonesFiado.save(new MovimientoFiado(cliente, dia, dia.atTime(9, 0), tipo, detalle,
                BigDecimal.valueOf(monto).setScale(2), cobroId));
    }

    private static void agregar(List<MovimientoCaja> lista, LocalDate dia, Random azar, LocalDateTime ahora,
                                TipoMovimiento tipo, BigDecimal monto, String detalle, String categoria) {
        LocalDateTime momento = dia.atTime(LocalTime.of(8, 0).plusMinutes(azar.nextInt(13 * 60)));
        if (momento.isAfter(ahora)) {
            return;
        }
        lista.add(new MovimientoCaja(dia, momento, tipo, monto, detalle, categoria));
    }

    /** Montos redondeados a 50, como los precios de verdad. */
    private static BigDecimal plata(Random azar, int minimo, int maximo) {
        int monto = minimo + azar.nextInt(maximo - minimo);
        return BigDecimal.valueOf(monto / 50 * 50L);
    }
}
