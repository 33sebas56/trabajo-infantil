package com.pasto.trabajo_infantil.controller;

import com.pasto.trabajo_infantil.dto.PeriodoResponse;
import com.pasto.trabajo_infantil.service.PeriodoService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/publico/periodos")
public class PeriodoController {

    private final PeriodoService periodoService;

    public PeriodoController(PeriodoService periodoService) {
        this.periodoService = periodoService;
    }

    @GetMapping
    public List<PeriodoResponse> listarPeriodos() {
        return periodoService.listarDisponibles();
    }
}