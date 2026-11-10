package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.PeriodoResponse;
import com.pasto.trabajo_infantil.enums.EstadoPeriodo;
import com.pasto.trabajo_infantil.repository.PeriodoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PeriodoService {

    private final PeriodoRepository periodoRepository;

    public PeriodoService(PeriodoRepository periodoRepository) {
        this.periodoRepository = periodoRepository;
    }

    @Transactional(readOnly = true)
    public List<PeriodoResponse> listarDisponibles() {
        return periodoRepository
                .findByEstadoOrderByAnioDescTrimestreDesc(
                        EstadoPeriodo.DISPONIBLE
                )
                .stream()
                .map(periodo -> new PeriodoResponse(
                        periodo.getId(),
                        periodo.getAnio(),
                        periodo.getTrimestre()
                ))
                .toList();
    }
}