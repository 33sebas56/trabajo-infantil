package com.pasto.trabajo_infantil.repository;

import com.pasto.trabajo_infantil.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository
        extends JpaRepository<Administrador, Long> {

    Optional<Administrador> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);
}