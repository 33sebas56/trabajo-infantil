package com.pasto.trabajo_infantil.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "error_carga")
public class ErrorCarga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carga_id", nullable = false)
    private CargaDatos carga;

    @Column(name = "numero_fila")
    private Integer numeroFila;

    @Column(length = 100)
    private String campo;

    @Column(name = "valor_recibido", columnDefinition = "TEXT")
    private String valorRecibido;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detalle;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    public ErrorCarga() {
    }

    public Long getId() {
        return id;
    }

    public CargaDatos getCarga() {
        return carga;
    }

    public void setCarga(CargaDatos carga) {
        this.carga = carga;
    }

    public Integer getNumeroFila() {
        return numeroFila;
    }

    public void setNumeroFila(Integer numeroFila) {
        this.numeroFila = numeroFila;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public String getValorRecibido() {
        return valorRecibido;
    }

    public void setValorRecibido(String valorRecibido) {
        this.valorRecibido = valorRecibido;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}