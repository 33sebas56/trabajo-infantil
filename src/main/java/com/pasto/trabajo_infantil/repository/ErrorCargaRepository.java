package com.pasto.trabajo_infantil.repository;

import com.pasto.trabajo_infantil.entity.ErrorCarga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorCargaRepository
        extends JpaRepository<ErrorCarga, Long> {

    List<ErrorCarga> findByCarga_IdOrderByIdAsc(Long cargaId);

    long countByCarga_Id(Long cargaId);
}