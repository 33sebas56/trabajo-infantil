package com.pasto.trabajo_infantil.repository;

import com.pasto.trabajo_infantil.entity.Territorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TerritorioRepository
        extends JpaRepository<Territorio, Long> {

    Optional<Territorio> findByCodigoFuente(String codigoFuente);

    List<Territorio> findByActivoTrueOrderByNombreAsc();
}