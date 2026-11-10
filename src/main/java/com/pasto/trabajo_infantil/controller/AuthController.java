package com.pasto.trabajo_infantil.controller;

import com.pasto.trabajo_infantil.dto.auth.LoginRequest;
import com.pasto.trabajo_infantil.dto.auth.LoginResponse;
import com.pasto.trabajo_infantil.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> obtenerTokenCsrf(
            CsrfToken token
    ) {
        Map<String, String> respuesta = Map.of(
                "header", token.getHeaderName(),
                "token", token.getToken()
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<?> iniciarSesion(
            @Valid @RequestBody LoginRequest datos,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            LoginResponse resultado = authService.iniciarSesion(
                    datos,
                    request,
                    response
            );

            return ResponseEntity.ok(resultado);

        } catch (AuthenticationException excepcion) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "mensaje",
                                    "Correo o contraseña incorrectos, "
                                            + "o cuenta inactiva."
                            )
                    );
        }
    }

    @GetMapping("/me")
    public LoginResponse consultarSesion(Authentication autenticacion) {
        return authService.obtenerSesionActual(autenticacion.getName());
    }
}