package com.pasto.trabajo_infantil.dto.carga;

import com.pasto.trabajo_infantil.enums.EstadoCarga;

import java.time.Instant;

public record CargaResponse(
        Long id,
        Long periodoId,
        Integer anio,
        Integer trimestre,
        String nombreArchivo,
        EstadoCarga estado,
        boolean vigente,
        Integer registrosTotales,
        Integer registrosPasto,
        Integer registrosValidos,
        Integer registrosInvalidos,
        String mensajeResultado,
        Instant iniciadoEn,
        Instant finalizadoEn
) {
}