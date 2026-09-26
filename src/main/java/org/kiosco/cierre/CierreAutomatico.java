package org.kiosco.cierre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara el cierre de caja sin que nadie tenga que acordarse. */
@Component
class CierreAutomatico {

    private static final Logger log = LoggerFactory.getLogger(CierreAutomatico.class);

    private final CierreDiarioService servicio;

    CierreAutomatico(CierreDiarioService servicio) {
        this.servicio = servicio;
    }

    @EventListener(ApplicationReadyEvent.class)
    void alArrancar() {
        cerrar();
    }

    @Scheduled(cron = "5 0 0 * * *")
    void aLaMedianoche() {
        cerrar();
    }

    private void cerrar() {
        int cerrados = servicio.cerrarDiasPendientes();
        if (cerrados > 0) {
            log.info("Cierre automático: {} día(s) cerrado(s)", cerrados);
        }
    }
}
