package com.pasto.trabajo_infantil.config;

import com.pasto.trabajo_infantil.security.AdministradorDetailsService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository
    ) {
        return new CompositeSessionAuthenticationStrategy(
                List.of(
                        new ChangeSessionIdAuthenticationStrategy(),
                        new CsrfAuthenticationStrategy(csrfTokenRepository)
                )
        );
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AdministradorDetailsService administradorDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider proveedor =
                new DaoAuthenticationProvider(administradorDetailsService);

        proveedor.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(proveedor);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(csrf ->
                        csrf.csrfTokenRepository(csrfTokenRepository)
                )

                .securityContext(contexto ->
                        contexto.securityContextRepository(
                                securityContextRepository
                        )
                )

                .sessionManagement(sesion ->
                        sesion.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .requestCache(cache -> cache.disable())

                .authorizeHttpRequests(permisos -> permisos

                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/publico/**"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/admin/**",
                                "/api/auth/me"
                        )
                        .hasRole("ADMIN")

                        .anyRequest()
                        .denyAll()
                )

                .formLogin(formulario -> formulario.disable())

                .httpBasic(basica -> basica.disable())

                .logout(salida -> salida

                        .logoutUrl("/api/auth/logout")

                        .logoutSuccessHandler(
                                (request, response, authentication) ->
                                        response.setStatus(
                                                HttpServletResponse.SC_NO_CONTENT
                                        )
                        )

                        .deleteCookies("JSESSIONID")

                        .permitAll()
                )

                .exceptionHandling(errores -> errores

                        .authenticationEntryPoint(
                                (request, response, excepcion) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_UNAUTHORIZED
                                    );

                                    response.setContentType(
                                            "application/json;charset=UTF-8"
                                    );

                                    response.getWriter().write(
                                            "{\"mensaje\":"
                                                    + "\"Debes iniciar sesion.\"}"
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, excepcion) -> {
                                    response.setStatus(
                                            HttpServletResponse.SC_FORBIDDEN
                                    );

                                    response.setContentType(
                                            "application/json;charset=UTF-8"
                                    );

                                    response.getWriter().write(
                                            "{\"mensaje\":"
                                                    + "\"Acceso denegado o token "
                                                    + "de seguridad invalido.\"}"
                                    );
                                }
                        )
                );

        return http.build();
    }
}