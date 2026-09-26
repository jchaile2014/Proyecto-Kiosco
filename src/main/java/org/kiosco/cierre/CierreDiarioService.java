package org.kiosco.cierre;

import org.kiosco.caja.MovimientoCajaRepository;
import org.kiosco.caja.Totales;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CierreDiarioService {

    private final CierreDiarioRepository cierres;
    private final MovimientoCajaRepository movimientos;
    private final Clock clock;

    public CierreDiarioService(CierreDiarioRepository cierres, MovimientoCajaRepository movimientos, Clock clock) {
        this.cierres = cierres;
        this.movimientos = movimientos;
        this.clock = clock;
    }

    /**
     * Cierra todos los días ya terminados que todavía no tienen cierre. Se llama a la
     * medianoche y al arrancar la app, por si la PC estuvo apagada varios días.
     *
     * @return cuántos días se cerraron
     */
    @Transactional
    public int cerrarDiasPendientes() {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate desde = cierres.findTopByOrderByFechaDesc()
                .map(ultimo -> ultimo.getFecha().plusDays(1))
                .or(movimientos::primeraFecha)
                .orElse(hoy);

        int cerrados = 0;
        for (LocalDate dia = desde; dia.isBefore(hoy); dia = dia.plusDays(1)) {
            cerrar(dia);
            cerrados++;
        }
        return cerrados;
    }

    /** Si se anotó o borró algo en un día que ya terminó, rehace su cierre. */
    @Transactional
    public void recalcularSiYaTermino(LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now(clock))) {
            cerrar(fecha);
        }
    }

    @Transactional(readOnly = true)
    public Optional<CierreDiario> cierreDel(LocalDate fecha) {
        return cierres.findById(fecha);
    }

    private void cerrar(LocalDate fecha) {
        Totales totales = Totales.desde(movimientos.totalesPorTipo(fecha, fecha));
        CierreDiario cierre = cierres.findById(fecha).orElseGet(() -> new CierreDiario(fecha));
        cierre.actualizar(totales, LocalDateTime.now(clock));
        cierres.save(cierre);
    }
}
