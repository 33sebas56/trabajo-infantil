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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "resultado")
public class Resultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carga_id", nullable = false)
    private CargaDatos carga;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "indicador_id", nullable = false)
    private Indicador indicador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "territorio_id", nullable = false)
    private Territorio territorio;

    @Column(
            name = "criterio_agrupacion",
            nullable = false,
            length = 80
    )
    private String criterioAgrupacion = "GENERAL";

    @Column(nullable = false, length = 120)
    private String grupo = "TOTAL";

    @Column(precision = 24, scale = 6)
    private BigDecimal valor;

    @Column(precision = 24, scale = 6)
    private BigDecimal numerador;

    @Column(precision = 24, scale = 6)
    private BigDecimal denominador;

    @Column(name = "cantidad_muestra")
    private Integer cantidadMuestra;

    @Column(name = "muestra_numerador")
    private Integer muestraNumerador;

    @Column(name = "muestra_denominador")
    private Integer muestraDenominador;

    @Column(
            name = "motivo_no_disponible",
            columnDefinition = "TEXT"
    )
    private String motivoNoDisponible;

    @Column(
            name = "calculado_en",
            nullable = false,
            updatable = false
    )
    private Instant calculadoEn = Instant.now();

    public Resultado() {
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

    public Indicador getIndicador() {
        return indicador;
    }

    public void setIndicador(Indicador indicador) {
        this.indicador = indicador;
    }

    public Territorio getTerritorio() {
        return territorio;
    }

    public void setTerritorio(Territorio territorio) {
        this.territorio = territorio;
    }

    public String getCriterioAgrupacion() {
        return criterioAgrupacion;
    }

    public void setCriterioAgrupacion(String criterioAgrupacion) {
        this.criterioAgrupacion = criterioAgrupacion;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getNumerador() {
        return numerador;
    }

    public void setNumerador(BigDecimal numerador) {
        this.numerador = numerador;
    }

    public BigDecimal getDenominador() {
        return denominador;
    }

    public void setDenominador(BigDecimal denominador) {
        this.denominador = denominador;
    }

    public Integer getCantidadMuestra() {
        return cantidadMuestra;
    }

    public void setCantidadMuestra(Integer cantidadMuestra) {
        this.cantidadMuestra = cantidadMuestra;
    }

    public Integer getMuestraNumerador() {
        return muestraNumerador;
    }

    public void setMuestraNumerador(Integer muestraNumerador) {
        this.muestraNumerador = muestraNumerador;
    }

    public Integer getMuestraDenominador() {
        return muestraDenominador;
    }

    public void setMuestraDenominador(Integer muestraDenominador) {
        this.muestraDenominador = muestraDenominador;
    }

    public String getMotivoNoDisponible() {
        return motivoNoDisponible;
    }

    public void setMotivoNoDisponible(
            String motivoNoDisponible
    ) {
        this.motivoNoDisponible = motivoNoDisponible;
    }

    public Instant getCalculadoEn() {
        return calculadoEn;
    }
}