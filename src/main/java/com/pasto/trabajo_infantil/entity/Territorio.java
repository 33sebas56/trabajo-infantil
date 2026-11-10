package com.pasto.trabajo_infantil.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "territorio")
public class Territorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_fuente", nullable = false, length = 30)
    private String codigoFuente;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(name = "contorno_geojson", columnDefinition = "TEXT")
    private String contornoGeojson;

    @Column(nullable = false)
    private boolean activo = true;

    public Territorio() {
    }

    public Long getId() {
        return id;
    }

    public String getCodigoFuente() {
        return codigoFuente;
    }

    public void setCodigoFuente(String codigoFuente) {
        this.codigoFuente = codigoFuente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getContornoGeojson() {
        return contornoGeojson;
    }

    public void setContornoGeojson(String contornoGeojson) {
        this.contornoGeojson = contornoGeojson;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}