package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.IndicadorResponse;
import com.pasto.trabajo_infantil.repository.IndicadorRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IndicadorService {

    private final IndicadorRepository indicadorRepository;

    public IndicadorService(IndicadorRepository indicadorRepository) {
        this.indicadorRepository = indicadorRepository;
    }

    @Transactional(readOnly = true)
    public List<IndicadorResponse> listarActivos() {
        return indicadorRepository
                .findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(indicador -> new IndicadorResponse(
                        indicador.getId(),
                        indicador.getCodigo(),
                        indicador.getNombre(),
                        indicador.getDescripcion(),
                        indicador.getUnidad(),
                        indicador.getVersionRegla()
                ))
                .toList();
    }
}