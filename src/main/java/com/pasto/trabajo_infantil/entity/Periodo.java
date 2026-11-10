package com.pasto.trabajo_infantil.entity;

import com.pasto.trabajo_infantil.enums.EstadoPeriodo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "periodo")
public class Periodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private Integer trimestre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPeriodo estado = EstadoPeriodo.PENDIENTE;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    public Periodo() {
    }

    public Long getId() {
        return id;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getTrimestre() {
        return trimestre;
    }

    public void setTrimestre(Integer trimestre) {
        this.trimestre = trimestre;
    }

    public EstadoPeriodo getEstado() {
        return estado;
    }

    public void setEstado(EstadoPeriodo estado) {
        this.estado = estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}