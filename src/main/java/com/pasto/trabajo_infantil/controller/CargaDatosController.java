package com.pasto.trabajo_infantil.controller;

import com.pasto.trabajo_infantil.dto.carga.CargaResponse;
import com.pasto.trabajo_infantil.dto.carga.ErrorCargaResponse;
import com.pasto.trabajo_infantil.dto.resultado.ResultadoResponse;

import com.pasto.trabajo_infantil.enums.EstadoCarga;

import com.pasto.trabajo_infantil.service.CargaDatosService;
import com.pasto.trabajo_infantil.service.ProcesamientoService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cargas")
public class CargaDatosController {

    private final CargaDatosService cargaDatosService;
    private final ProcesamientoService procesamientoService;

    public CargaDatosController(
            CargaDatosService cargaDatosService,
            ProcesamientoService procesamientoService
    ) {
        this.cargaDatosService = cargaDatosService;
        this.procesamientoService = procesamientoService;
    }

    /*
     * Revisión inicial del CSV.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CargaResponse> revisarArchivo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("anio") Integer anio,
            @RequestParam("trimestre") Integer trimestre,
            Authentication autenticacion
    ) {
        CargaResponse respuesta = cargaDatosService.revisarArchivo(
                archivo,
                anio,
                trimestre,
                autenticacion.getName()
        );

        if (respuesta.estado() == EstadoCarga.FALLIDA) {
            return ResponseEntity
                    .status(422)
                    .body(respuesta);
        }

        return ResponseEntity.ok(respuesta);
    }

    /*
     * Calcula y guarda resultados exploratorios privados.
     */
    @PostMapping(
            value = "/procesar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CargaResponse> procesarArchivo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("anio") Integer anio,
            @RequestParam("trimestre") Integer trimestre,
            Authentication autenticacion
    ) {
        CargaResponse respuesta = procesamientoService.procesarArchivo(
                archivo,
                anio,
                trimestre,
                autenticacion.getName()
        );

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping
    public List<CargaResponse> listarCargas() {
        return cargaDatosService.listarCargas();
    }

    @GetMapping("/{cargaId}/errores")
    public List<ErrorCargaResponse> listarErrores(
            @PathVariable("cargaId") Long cargaId
    ) {
        return cargaDatosService.listarErrores(cargaId);
    }

    /*
     * Resultados exploratorios visibles solo para administradores.
     */
    @GetMapping("/{cargaId}/resultados")
    public List<ResultadoResponse> consultarResultados(
            @PathVariable("cargaId") Long cargaId
    ) {
        return procesamientoService.consultarResultadosPrivados(cargaId);
    }
}