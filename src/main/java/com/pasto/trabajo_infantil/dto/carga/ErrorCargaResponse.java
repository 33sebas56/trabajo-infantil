package com.pasto.trabajo_infantil.dto.carga;

import java.time.Instant;

public record ErrorCargaResponse(
        Long id,
        Long cargaId,
        Integer numeroFila,
        String campo,
        String detalle,
        Instant creadoEn
) {
}