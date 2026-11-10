package com.pasto.trabajo_infantil.repository;

import com.pasto.trabajo_infantil.entity.CargaDatos;
import com.pasto.trabajo_infantil.enums.EstadoCarga;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CargaDatosRepository
        extends JpaRepository<CargaDatos, Long> {

    @EntityGraph(attributePaths = {"administrador", "periodo"})
    List<CargaDatos> findAllByOrderByIniciadoEnDesc();

    @EntityGraph(attributePaths = {"administrador", "periodo"})
    List<CargaDatos> findByPeriodo_IdOrderByIniciadoEnDesc(Long periodoId);

    @EntityGraph(attributePaths = {"administrador", "periodo"})
    Optional<CargaDatos> findByPeriodo_IdAndVigenteTrueAndEstado(
            Long periodoId,
            EstadoCarga estado
    );
}