package org.kiosco.cierre;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CierreDiarioRepository extends JpaRepository<CierreDiario, LocalDate> {

    Optional<CierreDiario> findTopByOrderByFechaDesc();
}
