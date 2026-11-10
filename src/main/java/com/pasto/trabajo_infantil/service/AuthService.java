package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.auth.LoginRequest;
import com.pasto.trabajo_infantil.dto.auth.LoginResponse;
import com.pasto.trabajo_infantil.entity.Administrador;
import com.pasto.trabajo_infantil.repository.AdministradorRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final AdministradorRepository administradorRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            AdministradorRepository administradorRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.administradorRepository = administradorRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponse iniciarSesion(
            LoginRequest datos,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String correo = datos.getCorreo().trim();

        byte[] contrasenaBytes = datos.getContrasena()
                .getBytes(StandardCharsets.UTF_8);

        if (contrasenaBytes.length > 72) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }

        Authentication solicitud =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        correo,
                        datos.getContrasena()
                );

        Authentication autenticacion =
                authenticationManager.authenticate(solicitud);

        Administrador administrador = administradorRepository
                .findByCorreoIgnoreCase(autenticacion.getName())
                .filter(Administrador::isActivo)
                .orElseThrow(() ->
                        new BadCredentialsException("Credenciales inválidas.")
                );

        sessionAuthenticationStrategy.onAuthentication(
                autenticacion,
                request,
                response
        );

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);

        SecurityContextHolder.setContext(contexto);

        securityContextRepository.saveContext(
                contexto,
                request,
                response
        );

        return new LoginResponse(
                administrador.getId(),
                administrador.getNombre(),
                administrador.getCorreo(),
                "Inicio de sesión exitoso."
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse obtenerSesionActual(String correo) {
        Administrador administrador = administradorRepository
                .findByCorreoIgnoreCase(correo)
                .filter(Administrador::isActivo)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "La cuenta no está disponible."
                        )
                );

        return new LoginResponse(
                administrador.getId(),
                administrador.getNombre(),
                administrador.getCorreo(),
                "Sesión activa."
        );
    }
}