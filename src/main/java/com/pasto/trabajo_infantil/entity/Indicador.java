package com.pasto.trabajo_infantil.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "indicador")
public class Indicador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 30)
    private String unidad;

    @Column(name = "regla_calculo", nullable = false, columnDefinition = "TEXT")
    private String reglaCalculo;

    @Column(name = "version_regla", nullable = false, length = 20)
    private String versionRegla = "1.0";

    @Column(nullable = false)
    private boolean activo = true;

    public Indicador() {
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public String getReglaCalculo() {
        return reglaCalculo;
    }

    public void setReglaCalculo(String reglaCalculo) {
        this.reglaCalculo = reglaCalculo;
    }

    public String getVersionRegla() {
        return versionRegla;
    }

    public void setVersionRegla(String versionRegla) {
        this.versionRegla = versionRegla;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}