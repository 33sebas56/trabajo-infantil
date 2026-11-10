package com.pasto.trabajo_infantil.dto;

public record IndicadorResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String unidad,
        String versionRegla
) {
}