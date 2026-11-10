package com.pasto.trabajo_infantil.controller;

import com.pasto.trabajo_infantil.dto.carga.CargaResponse;

import com.pasto.trabajo_infantil.enums.EstadoCarga;

import com.pasto.trabajo_infantil.service.CargaEstadisticasService;
import com.pasto.trabajo_infantil.service.ComparacionService;

import com.pasto.trabajo_infantil.service
        .ComparacionService.ComparacionResponse;

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

@RestController
@RequestMapping("/api")
public class ComparacionController {

    private final CargaEstadisticasService cargaEstadisticasService;
    private final ComparacionService comparacionService;

    public ComparacionController(
            CargaEstadisticasService cargaEstadisticasService,
            ComparacionService comparacionService
    ) {
        this.cargaEstadisticasService =
                cargaEstadisticasService;

        this.comparacionService =
                comparacionService;
    }

    /**
     * Recibe un CSV y calcula sus resultados.
     */
    @PostMapping(
            value = "/admin/comparaciones/cargas",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CargaResponse> cargarArchivo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("anio") Integer anio,
            @RequestParam("trimestre") Integer trimestre,
            Authentication autenticacion
    ) {
        CargaResponse respuesta =
                cargaEstadisticasService.cargarYCalcular(
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

    /**
     * El administrador puede revisar resultados
     * aunque todavía no estén publicados.
     */
    @GetMapping("/admin/comparaciones")
    public ComparacionResponse compararComoAdministrador(
            @RequestParam("indicador") String indicador,
            @RequestParam("desde") int desde,
            @RequestParam("hasta") int hasta,
            @RequestParam(
                    name = "trimestre",
                    defaultValue = "4"
            ) int trimestre
    ) {
        return comparacionService.compararPrivado(
                indicador,
                desde,
                hasta,
                trimestre
        );
    }

    /**
     * La consulta pública solo utiliza cargas
     * vigentes y publicadas.
     */
    @GetMapping("/publico/comparaciones")
    public ComparacionResponse compararComoPublico(
            @RequestParam("indicador") String indicador,
            @RequestParam("desde") int desde,
            @RequestParam("hasta") int hasta,
            @RequestParam(
                    name = "trimestre",
                    defaultValue = "4"
            ) int trimestre
    ) {
        return comparacionService.compararPublico(
                indicador,
                desde,
                hasta,
                trimestre
        );
    }

    /**
     * La publicación sigue siendo manual.
     */
    @PostMapping(
            "/admin/comparaciones/cargas/{cargaId}/publicar"
    )
    public CargaResponse publicarCarga(
            @PathVariable("cargaId") Long cargaId,
            @RequestParam(
                    name = "confirmar",
                    defaultValue = "false"
            ) boolean confirmar
    ) {
        return comparacionService.publicarCarga(
                cargaId,
                confirmar
        );
    }
}