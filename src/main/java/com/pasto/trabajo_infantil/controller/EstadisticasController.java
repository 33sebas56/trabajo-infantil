package com.pasto.trabajo_infantil.controller;

import com.pasto.trabajo_infantil.service.EstadisticasService;

import com.pasto.trabajo_infantil.service
        .EstadisticasService.Comparacion;

import com.pasto.trabajo_infantil.service
        .EstadisticasService.Resumen;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(
            EstadisticasService estadisticasService
    ) {
        this.estadisticasService = estadisticasService;
    }

    /**
     * Consulta pública de un periodo.
     */
    @GetMapping("/publico/estadisticas")
    public Resumen consultarPublico(
            @RequestParam("anio") int anio,

            @RequestParam(
                    name = "trimestre",
                    defaultValue = "4"
            )
            int trimestre,

            @RequestParam(
                    name = "edad",
                    defaultValue = "TODAS"
            )
            String edad,

            @RequestParam(
                    name = "sexo",
                    defaultValue = "TODOS"
            )
            String sexo
    ) {
        return estadisticasService.consultar(
                anio,
                trimestre,
                edad,
                sexo,
                false
        );
    }

    /**
     * Consulta administrativa de un periodo.
     */
    @GetMapping("/admin/estadisticas")
    public Resumen consultarAdministracion(
            @RequestParam("anio") int anio,

            @RequestParam(
                    name = "trimestre",
                    defaultValue = "4"
            )
            int trimestre,

            @RequestParam(
                    name = "edad",
                    defaultValue = "TODAS"
            )
            String edad,

            @RequestParam(
                    name = "sexo",
                    defaultValue = "TODOS"
            )
            String sexo
    ) {
        return estadisticasService.consultar(
                anio,
                trimestre,
                edad,
                sexo,
                true
        );
    }

    /**
     * Comparación pública entre años.
     */
    @GetMapping("/publico/estadisticas/comparacion")
    public Comparacion compararPublico(
            @RequestParam("indicador") String indicador,
            @RequestParam("desde") int desde,
            @RequestParam("hasta") int hasta,

            @RequestParam(
                    name = "trimestre",
                    defaultValue = "4"
            )
            int trimestre,

            @RequestParam(
                    name = "edad",
                    defaultValue = "TODAS"
            )
            String edad,

            @RequestParam(
                    name = "sexo",
                    defaultValue = "TODOS"
            )
            String sexo
    ) {
        return estadisticasService.comparar(
                indicador,
                desde,
                hasta,
                trimestre,
                edad,
                sexo,
                false
        );
    }

    /**
     * Comparación administrativa entre años.
     */
    @GetMapping("/admin/estadisticas/comparacion")
    public Comparacion compararAdministracion(
            @RequestParam("indicador") String indicador,
            @RequestParam("desde") int desde,
            @RequestParam("hasta") int hasta,

            @RequestParam(
                    name = "trimestre",
                    defaultValue = "4"
            )
            int trimestre,

            @RequestParam(
                    name = "edad",
                    defaultValue = "TODAS"
            )
            String edad,

            @RequestParam(
                    name = "sexo",
                    defaultValue = "TODOS"
            )
            String sexo
    ) {
        return estadisticasService.comparar(
                indicador,
                desde,
                hasta,
                trimestre,
                edad,
                sexo,
                true
        );
    }
}