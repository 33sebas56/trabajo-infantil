package com.pasto.trabajo_infantil.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "El correo es obligatorio.")
    @Email(message = "El correo no tiene un formato válido.")
    @Size(max = 160, message = "El correo no puede superar los 160 caracteres.")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(max = 255, message = "La contraseña es demasiado larga.")
    private String contrasena;

    public LoginRequest() {
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}