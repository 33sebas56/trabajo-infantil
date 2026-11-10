package com.pasto.trabajo_infantil.controller;

import com.pasto.trabajo_infantil.dto.IndicadorResponse;
import com.pasto.trabajo_infantil.service.IndicadorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/publico/indicadores")
public class IndicadorController {

    private final IndicadorService indicadorService;

    public IndicadorController(IndicadorService indicadorService) {
        this.indicadorService = indicadorService;
    }

    @GetMapping
    public List<IndicadorResponse> listarIndicadores() {
        return indicadorService.listarActivos();
    }
}