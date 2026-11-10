package com.pasto.trabajo_infantil.security;

import com.pasto.trabajo_infantil.entity.Administrador;
import com.pasto.trabajo_infantil.repository.AdministradorRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministradorDetailsService implements UserDetailsService {

    private final AdministradorRepository administradorRepository;

    public AdministradorDetailsService(
            AdministradorRepository administradorRepository
    ) {
        this.administradorRepository = administradorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) {
        Administrador administrador = administradorRepository
                .findByCorreoIgnoreCase(correo)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "No fue posible autenticar al administrador."
                        )
                );

        return User.withUsername(administrador.getCorreo())
                .password(administrador.getContrasenaHash())
                .roles("ADMIN")
                .disabled(!administrador.isActivo())
                .build();
    }
}