package com.pasto.trabajo_infantil.repository;

import com.pasto.trabajo_infantil.entity.Indicador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndicadorRepository
        extends JpaRepository<Indicador, Long> {

    Optional<Indicador> findByCodigoAndVersionRegla(
            String codigo,
            String versionRegla
    );

    List<Indicador> findByActivoTrueOrderByNombreAsc();
}