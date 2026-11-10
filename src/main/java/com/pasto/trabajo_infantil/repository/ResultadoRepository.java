package com.pasto.trabajo_infantil.repository;

import com.pasto.trabajo_infantil.entity.Resultado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultadoRepository
        extends JpaRepository<Resultado, Long> {

    @EntityGraph(attributePaths = {
            "carga",
            "carga.periodo",
            "indicador",
            "territorio"
    })
    List<Resultado> findByCarga_IdOrderByIdAsc(Long cargaId);

    @EntityGraph(attributePaths = {
            "carga",
            "carga.periodo",
            "indicador",
            "territorio"
    })
    List<Resultado> findByCarga_IdAndIndicador_IdOrderByIdAsc(
            Long cargaId,
            Long indicadorId
    );

    @EntityGraph(attributePaths = {
            "carga",
            "carga.periodo",
            "indicador",
            "territorio"
    })
    List<Resultado>
    findByCarga_IdAndCriterioAgrupacionAndGrupoOrderByIdAsc(
            Long cargaId,
            String criterioAgrupacion,
            String grupo
    );
}