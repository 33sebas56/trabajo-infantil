package com.pasto.trabajo_infantil.config;

import com.pasto.trabajo_infantil.entity.Administrador;
import com.pasto.trabajo_infantil.repository.AdministradorRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@ConditionalOnProperty(
        prefix = "app.admin.inicial",
        name = "habilitado",
        havingValue = "true"
)
public class AdministradorInicializador implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdministradorInicializador.class);

    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.inicial.nombre:}")
    private String nombre;

    @Value("${app.admin.inicial.correo:}")
    private String correo;

    @Value("${app.admin.inicial.password:}")
    private String contrasena;

    public AdministradorInicializador(
            AdministradorRepository administradorRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.administradorRepository = administradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (administradorRepository.count() > 0) {
            log.info(
                    "Ya existe un administrador. "
                            + "No se creará otro automáticamente."
            );
            return;
        }

        String nombreLimpio = nombre.trim();
        String correoLimpio = correo.trim().toLowerCase(Locale.ROOT);

        if (nombreLimpio.isBlank() || nombreLimpio.length() > 120) {
            throw new IllegalStateException(
                    "Configura un nombre válido para el administrador inicial."
            );
        }

        if (correoLimpio.isBlank()
                || correoLimpio.length() > 160
                || !correoLimpio.contains("@")) {
            throw new IllegalStateException(
                    "Configura un correo válido para el administrador inicial."
            );
        }

        int longitudBytes = contrasena
                .getBytes(StandardCharsets.UTF_8)
                .length;

        if (contrasena.isBlank()
                || contrasena.length() < 12
                || longitudBytes > 72) {
            throw new IllegalStateException(
                    "La contraseña inicial debe tener al menos 12 caracteres "
                            + "y no superar 72 bytes en UTF-8."
            );
        }

        Administrador administrador = new Administrador();
        administrador.setNombre(nombreLimpio);
        administrador.setCorreo(correoLimpio);
        administrador.setContrasenaHash(passwordEncoder.encode(contrasena));
        administrador.setActivo(true);

        administradorRepository.save(administrador);

        log.info("Se creó el administrador inicial.");
    }
}