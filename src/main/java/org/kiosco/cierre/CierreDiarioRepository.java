package org.kiosco.cierre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface CierreDiarioRepository extends JpaRepository<CierreDiario, LocalDate> {

    @Query("select c.fecha from CierreDiario c where c.fecha between :desde and :hasta")
    List<LocalDate> fechasCerradasEntre(LocalDate desde, LocalDate hasta);
}
