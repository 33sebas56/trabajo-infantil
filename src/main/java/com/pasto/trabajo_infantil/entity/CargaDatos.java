package com.pasto.trabajo_infantil.entity;

import com.pasto.trabajo_infantil.enums.EstadoCarga;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "carga_datos")
public class CargaDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "administrador_id", nullable = false)
    private Administrador administrador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCarga estado = EstadoCarga.RECIBIDA;

    @Column(nullable = false)
    private boolean vigente = false;

    @Column(name = "registros_totales", nullable = false)
    private Integer registrosTotales = 0;

    @Column(name = "registros_pasto", nullable = false)
    private Integer registrosPasto = 0;

    @Column(name = "registros_validos", nullable = false)
    private Integer registrosValidos = 0;

    @Column(name = "registros_invalidos", nullable = false)
    private Integer registrosInvalidos = 0;

    @Column(name = "mensaje_resultado", columnDefinition = "TEXT")
    private String mensajeResultado;

    @Column(name = "iniciado_en", nullable = false, updatable = false)
    private Instant iniciadoEn = Instant.now();

    @Column(name = "finalizado_en")
    private Instant finalizadoEn;

    public CargaDatos() {
    }

    public Long getId() {
        return id;
    }

    public Administrador getAdministrador() {
        return administrador;
    }

    public void setAdministrador(Administrador administrador) {
        this.administrador = administrador;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
        this.periodo = periodo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public EstadoCarga getEstado() {
        return estado;
    }

    public void setEstado(EstadoCarga estado) {
        this.estado = estado;
    }

    public boolean isVigente() {
        return vigente;
    }

    public void setVigente(boolean vigente) {
        this.vigente = vigente;
    }

    public Integer getRegistrosTotales() {
        return registrosTotales;
    }

    public void setRegistrosTotales(Integer registrosTotales) {
        this.registrosTotales = registrosTotales;
    }

    public Integer getRegistrosPasto() {
        return registrosPasto;
    }

    public void setRegistrosPasto(Integer registrosPasto) {
        this.registrosPasto = registrosPasto;
    }

    public Integer getRegistrosValidos() {
        return registrosValidos;
    }

    public void setRegistrosValidos(Integer registrosValidos) {
        this.registrosValidos = registrosValidos;
    }

    public Integer getRegistrosInvalidos() {
        return registrosInvalidos;
    }

    public void setRegistrosInvalidos(Integer registrosInvalidos) {
        this.registrosInvalidos = registrosInvalidos;
    }

    public String getMensajeResultado() {
        return mensajeResultado;
    }

    public void setMensajeResultado(String mensajeResultado) {
        this.mensajeResultado = mensajeResultado;
    }

    public Instant getIniciadoEn() {
        return iniciadoEn;
    }

    public Instant getFinalizadoEn() {
        return finalizadoEn;
    }

    public void setFinalizadoEn(Instant finalizadoEn) {
        this.finalizadoEn = finalizadoEn;
    }
}