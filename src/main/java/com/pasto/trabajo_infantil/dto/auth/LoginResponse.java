package com.pasto.trabajo_infantil.dto.auth;

public record LoginResponse(
        Long id,
        String nombre,
        String correo,
        String mensaje
) {
}