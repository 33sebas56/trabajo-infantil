package com.pasto.trabajo_infantil.dto.resultado;

import java.math.BigDecimal;
import java.time.Instant;

public record ResultadoResponse(
        Long id,
        Integer anio,
        Integer trimestre,
        String codigoIndicador,
        String nombreIndicador,
        String versionRegla,
        String unidad,
        String territorio,
        String criterioAgrupacion,
        String grupo,
        BigDecimal valor,
        BigDecimal numerador,
        BigDecimal denominador,
        Integer cantidadMuestra,
        String motivoNoDisponible,
        Instant calculadoEn
) {
}