package com.pasto.trabajo_infantil.controller;

import com.pasto.trabajo_infantil.dto.resultado.ResultadoResponse;

import com.pasto.trabajo_infantil.service.ResultadoService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/publico/periodos")
public class ResultadoController {

    private final ResultadoService resultadoService;

    public ResultadoController(
            ResultadoService resultadoService
    ) {
        this.resultadoService = resultadoService;
    }

    @GetMapping("/{periodoId}/resultados")
    public List<ResultadoResponse> consultarResultados(
            @PathVariable("periodoId") Long periodoId,

            @RequestParam(
                    name = "criterio",
                    required = false
            ) String criterio,

            @RequestParam(
                    name = "grupo",
                    required = false
            ) String grupo
    ) {
        return resultadoService
                .consultarResultados(
                        periodoId,
                        criterio,
                        grupo
                )
                .stream()
                .map(this::protegerIngresos)
                .toList();
    }

    private ResultadoResponse protegerIngresos(
            ResultadoResponse resultado
    ) {
        String codigo = resultado.codigoIndicador();

        boolean esIngreso =
                "SALARIO_MENSUAL_P409".equals(codigo)
                        || "GANANCIA_MENSUAL_P413".equals(codigo);

        if (!esIngreso) {
            return resultado;
        }

        String motivo =
                "La información de ingresos está pendiente "
                        + "de revisión para su consulta pública.";

        if (
                resultado.valor() == null
                        && resultado.motivoNoDisponible() != null
        ) {
            motivo = resultado.motivoNoDisponible();

            if (
                    motivo.startsWith(
                            "Hay una sola respuesta de ingreso"
                    )
            ) {
                motivo =
                        "No hay suficientes respuestas "
                                + "para mostrar el ingreso.";
            }
        }

        return new ResultadoResponse(
                resultado.id(),
                resultado.anio(),
                resultado.trimestre(),
                resultado.codigoIndicador(),
                resultado.nombreIndicador(),
                resultado.versionRegla(),
                resultado.unidad(),
                resultado.territorio(),
                resultado.criterioAgrupacion(),
                resultado.grupo(),
                null,
                null,
                null,
                null,
                motivo,
                resultado.calculadoEn()
        );
    }
}