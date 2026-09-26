package org.kiosco.demo;

import org.kiosco.caja.MovimientoCaja;
import org.kiosco.caja.MovimientoCajaRepository;
import org.kiosco.caja.TipoMovimiento;
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
import java.util.List;
import java.util.Random;

/**
 * Con el perfil "demo" la app arranca con una base en memoria y dos meses de movimientos
 * inventados. Sirve para probarla sin instalar MySQL y para sacar capturas sin datos reales.
 */
@Component
@Profile("demo")
class DatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);
    private static final String[] DETALLES_VENTA =
            {null, null, null, "Gaseosa y alfajor", "Cigarrillos", "Pan y leche", "Golosinas", "Fiambre", "Yerba y azúcar"};

    private final MovimientoCajaRepository movimientos;
    private final Clock clock;

    DatosDemo(MovimientoCajaRepository movimientos, Clock clock) {
        this.movimientos = movimientos;
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
        log.info("Modo demo: {} movimientos de ejemplo cargados", nuevos.size());
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
