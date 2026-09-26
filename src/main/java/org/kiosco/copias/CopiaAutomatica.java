package org.kiosco.copias;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Una copia al abrir la app (si todavía no hay una de hoy) y otra al final del día.
 * Si la PC estaba apagada a la noche, la de la mañana siguiente guarda cómo cerró el día anterior.
 */
@Component
class CopiaAutomatica {

    private final CopiaDeSeguridadService copias;

    CopiaAutomatica(CopiaDeSeguridadService copias) {
        this.copias = copias;
    }

    @EventListener(ApplicationReadyEvent.class)
    void alArrancar() {
        if (copias.disponible() && !copias.hayCopiaDeHoy()) {
            hacerSinFrenarLaApp();
        }
    }

    @Scheduled(cron = "0 55 23 * * *")
    void alFinalDelDia() {
        if (copias.disponible()) {
            hacerSinFrenarLaApp();
        }
    }

    private void hacerSinFrenarLaApp() {
        try {
            copias.hacerCopia();
        } catch (CopiaFallidaException e) {
            // Ya quedó registrado; la pantalla Hoy avisa que falló
        }
    }
}
