package com.pasto.trabajo_infantil.repository;

import com.pasto.trabajo_infantil.entity.Periodo;
import com.pasto.trabajo_infantil.enums.EstadoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PeriodoRepository
        extends JpaRepository<Periodo, Long> {

    Optional<Periodo> findByAnioAndTrimestre(
            Integer anio,
            Integer trimestre
    );

    List<Periodo> findAllByOrderByAnioDescTrimestreDesc();

    List<Periodo> findByEstadoOrderByAnioDescTrimestreDesc(
            EstadoPeriodo estado
    );
}