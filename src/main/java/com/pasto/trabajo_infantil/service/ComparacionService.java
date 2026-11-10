
package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.carga.CargaResponse;

import com.pasto.trabajo_infantil.entity.CargaDatos;
import com.pasto.trabajo_infantil.entity.Periodo;
import com.pasto.trabajo_infantil.entity.Resultado;

import com.pasto.trabajo_infantil.enums.EstadoCarga;
import com.pasto.trabajo_infantil.enums.EstadoPeriodo;

import com.pasto.trabajo_infantil.exception.RecursoNoEncontradoException;

import com.pasto.trabajo_infantil.repository.CargaDatosRepository;
import com.pasto.trabajo_infantil.repository.PeriodoRepository;
import com.pasto.trabajo_infantil.repository.ResultadoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ComparacionService {

    private static final String VERSION_COMUN = "EXP-COMUN-1";

    private static final String CODIGO_POBLACION =
            "POBLACION_EXPANDIDA_5_17";

    private static final String CODIGO_TRABAJO =
            "RESPUESTAS_AFIRMATIVAS_TRABAJO";

    private final PeriodoRepository periodoRepository;
    private final CargaDatosRepository cargaDatosRepository;
    private final ResultadoRepository resultadoRepository;
    private final EntityManager entityManager;

    public ComparacionService(
            PeriodoRepository periodoRepository,
            CargaDatosRepository cargaDatosRepository,
            ResultadoRepository resultadoRepository,
            EntityManager entityManager
    ) {
        this.periodoRepository = periodoRepository;
        this.cargaDatosRepository = cargaDatosRepository;
        this.resultadoRepository = resultadoRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public ComparacionResponse compararPrivado(
            String codigoIndicador,
            int desde,
            int hasta,
            int trimestre
    ) {
        return comparar(
                codigoIndicador,
                desde,
                hasta,
                trimestre,
                true
        );
    }

    @Transactional(readOnly = true)
    public ComparacionResponse compararPublico(
            String codigoIndicador,
            int desde,
            int hasta,
            int trimestre
    ) {
        return comparar(
                codigoIndicador,
                desde,
                hasta,
                trimestre,
                false
        );
    }

    private ComparacionResponse comparar(
            String codigoIndicador,
            int desde,
            int hasta,
            int trimestre,
            boolean vistaAdministrador
    ) {
        validarParametros(codigoIndicador, desde, hasta, trimestre);

        String unidad = obtenerUnidad(codigoIndicador);

        List<PuntoComparacion> puntos = new ArrayList<>();

        for (int anio = desde; anio <= hasta; anio++) {
            puntos.add(
                    obtenerPunto(
                            codigoIndicador,
                            anio,
                            trimestre,
                            vistaAdministrador
                    )
            );
        }

        boolean tiene2022O2023 = puntos.stream().anyMatch(punto ->
                "DISPONIBLE".equals(punto.estado())
                        && punto.anio() >= 2022 && punto.anio() <= 2023
        );

        boolean tiene2024O2025 = puntos.stream().anyMatch(punto ->
                "DISPONIBLE".equals(punto.estado())
                        && punto.anio() >= 2024 && punto.anio() <= 2025
        );

        String advertencia =
                "Las respuestas corresponden a las preguntas de la encuesta "
                        + "y no equivalen automáticamente a la tasa oficial de trabajo infantil.";

        if (PoliticaVersiones.requiereRevisionEntreGrupos(
                codigoIndicador, tiene2022O2023, tiene2024O2025
        )) {
            advertencia += " " + PoliticaVersiones.AVISO_CAMBIO_PREGUNTA;
        }

        return new ComparacionResponse(
                codigoIndicador,
                trimestre,
                unidad,
                VERSION_COMUN,
                advertencia,
                puntos
        );
    }

    private PuntoComparacion obtenerPunto(
            String codigoIndicador,
            int anio,
            int trimestre,
            boolean vistaAdministrador
    ) {
        Optional<Periodo> periodoOpcional =
                periodoRepository.findByAnioAndTrimestre(
                        anio,
                        trimestre
                );

        if (periodoOpcional.isEmpty()) {
            return new PuntoComparacion(
                    anio,
                    null,
                    null,
                    null,
                    "NO_DISPONIBLE",
                    "No existe una carga para este periodo."
            );
        }

        Periodo periodo = periodoOpcional.get();

        Optional<CargaDatos> cargaOpcional;

        if (vistaAdministrador) {
            cargaOpcional = buscarUltimaCargaConResultados(periodo.getId());

        } else {
            if (periodo.getEstado() != EstadoPeriodo.DISPONIBLE) {
                return new PuntoComparacion(
                        anio,
                        null,
                        null,
                        null,
                        "NO_DISPONIBLE",
                        "El periodo no está publicado."
                );
            }

            cargaOpcional = cargaDatosRepository
                    .findByPeriodo_IdAndVigenteTrueAndEstado(
                            periodo.getId(),
                            EstadoCarga.COMPLETADA
                    );
        }

        if (cargaOpcional.isEmpty()) {
            return new PuntoComparacion(
                    anio,
                    null,
                    null,
                    null,
                    "NO_DISPONIBLE",
                    "No hay resultados disponibles para este periodo."
            );
        }

        CargaDatos carga = cargaOpcional.get();

        List<Resultado> resultados = resultadoRepository
                .findByCarga_IdOrderByIdAsc(carga.getId());

        Optional<Resultado> resultadoOpcional = resultados.stream()
                .filter(resultado ->
                        codigoIndicador.equals(
                                resultado.getIndicador().getCodigo()
                        )
                )
                .filter(resultado ->
                        "52".equals(
                                resultado.getTerritorio().getCodigoFuente()
                        )
                )
                .filter(resultado ->
                        "GENERAL".equals(
                                resultado.getCriterioAgrupacion()
                        )
                )
                .filter(resultado ->
                        "TOTAL".equals(resultado.getGrupo())
                )
                .findFirst();

        if (resultadoOpcional.isEmpty()) {
            return new PuntoComparacion(
                    anio,
                    carga.getId(),
                    null,
                    null,
                    "NO_DISPONIBLE",
                    "La carga no contiene este indicador."
            );
        }

        Resultado resultado = resultadoOpcional.get();

        if (resultado.getValor() == null) {
            return new PuntoComparacion(
                    anio,
                    carga.getId(),
                    null,
                    null,
                    "NO_DISPONIBLE",
                    resultado.getMotivoNoDisponible()
            );
        }

        if (!PoliticaVersiones.comparable(
                anio,
                trimestre,
                resultado.getIndicador().getVersionRegla()
        )) {
            return new PuntoComparacion(
                    anio,
                    carga.getId(),
                    null,
                    null,
                    "NO_COMPARABLE",
                    "La versión de cálculo requiere revisión "
                            + "o el archivo debe procesarse nuevamente."
            );
        }

        return new PuntoComparacion(
                anio,
                carga.getId(),
                resultado.getValor(),
                resultado.getCantidadMuestra(),
                "DISPONIBLE",
                resultado.getMotivoNoDisponible()
        );
    }

    private Optional<CargaDatos> buscarUltimaCargaConResultados(
            Long periodoId
    ) {
        List<CargaDatos> cargas = cargaDatosRepository
                .findByPeriodo_IdOrderByIniciadoEnDesc(periodoId);

        for (CargaDatos carga : cargas) {
            boolean estadoAdmitido =
                    carga.getEstado() == EstadoCarga.VALIDADA
                            || carga.getEstado() == EstadoCarga.COMPLETADA;

            if (!estadoAdmitido) {
                continue;
            }

            boolean tieneResultados = !resultadoRepository
                    .findByCarga_IdOrderByIdAsc(carga.getId())
                    .isEmpty();

            if (tieneResultados) {
                return Optional.of(carga);
            }
        }

        return Optional.empty();
    }

    @Transactional
    public CargaResponse publicarCarga(
            Long cargaId,
            boolean confirmar
    ) {
        if (!confirmar) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes confirmar la publicación exploratoria."
            );
        }

        CargaDatos carga = cargaDatosRepository.findById(cargaId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "La carga solicitada no existe."
                        )
                );

        Periodo periodo = carga.getPeriodo();

        /*
         * Se bloquea el periodo mientras se cambia la carga vigente.
         */
        entityManager.lock(periodo, LockModeType.PESSIMISTIC_WRITE);

        /*
         * Actualiza el estado de la carga después de obtener el bloqueo.
         */
        entityManager.refresh(carga);

        boolean estadoAdmitido =
                carga.getEstado() == EstadoCarga.VALIDADA
                        || carga.getEstado() == EstadoCarga.COMPLETADA;

        if (!estadoAdmitido) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La carga no está lista para publicarse."
            );
        }

        List<Resultado> resultados = resultadoRepository
                .findByCarga_IdOrderByIdAsc(cargaId);

        if (resultados.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La carga no tiene resultados calculados."
            );
        }

        boolean versionRevisable = resultados.stream()
                .allMatch(resultado ->
                        PoliticaVersiones.publicable(
                                periodo.getAnio(),
                                periodo.getTrimestre(),
                                resultado.getIndicador().getVersionRegla()
                        )
                );

        if (!versionRevisable) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La versión guardada no corresponde al periodo "
                            + "de la carga o no está habilitada para publicarse."
            );
        }

        boolean tieneValor = resultados.stream()
                .anyMatch(resultado -> resultado.getValor() != null);

        if (!tieneValor) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se publicará la carga porque todos sus "
                            + "indicadores están no disponibles."
            );
        }

        Optional<CargaDatos> cargaVigente = cargaDatosRepository
                .findByPeriodo_IdAndVigenteTrueAndEstado(
                        periodo.getId(),
                        EstadoCarga.COMPLETADA
                );

        if (cargaVigente.isPresent()) {
            CargaDatos anterior = cargaVigente.get();

            if (!anterior.getId().equals(carga.getId())) {
                anterior.setVigente(false);
                cargaDatosRepository.saveAndFlush(anterior);
            }
        }

        carga.setEstado(EstadoCarga.COMPLETADA);
        carga.setVigente(true);

        carga.setMensajeResultado(
                "Carga publicada. Los indicadores sin información se muestran como no disponibles."
        );

        cargaDatosRepository.saveAndFlush(carga);

        periodo.setEstado(EstadoPeriodo.DISPONIBLE);
        periodoRepository.save(periodo);

        resultados.forEach(resultado ->
                resultado.getIndicador().setActivo(true)
        );

        return convertirCarga(carga);
    }

    private void validarParametros(
            String codigo,
            int desde,
            int hasta,
            int trimestre
    ) {
        if (!CODIGO_POBLACION.equals(codigo)
                && !CODIGO_TRABAJO.equals(codigo)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El indicador solicitado no está configurado "
                            + "para comparación."
            );
        }

        if (desde < 1900 || hasta > 2100 || desde > hasta) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El rango de años no es válido."
            );
        }

        if (hasta - desde > 30) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La comparación admite un máximo de 31 años."
            );
        }

        if (trimestre < 1 || trimestre > 4) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El trimestre debe estar entre 1 y 4."
            );
        }
    }

    private String obtenerUnidad(String codigo) {
        if (CODIGO_POBLACION.equals(codigo)) {
            return "PERSONAS";
        }

        return "PORCENTAJE";
    }

    private CargaResponse convertirCarga(CargaDatos carga) {
        Periodo periodo = carga.getPeriodo();

        return new CargaResponse(
                carga.getId(),
                periodo.getId(),
                periodo.getAnio(),
                periodo.getTrimestre(),
                carga.getNombreArchivo(),
                carga.getEstado(),
                carga.isVigente(),
                carga.getRegistrosTotales(),
                carga.getRegistrosPasto(),
                carga.getRegistrosValidos(),
                carga.getRegistrosInvalidos(),
                carga.getMensajeResultado(),
                carga.getIniciadoEn(),
                carga.getFinalizadoEn()
        );
    }

    public record ComparacionResponse(
            String codigoIndicador,
            int trimestre,
            String unidad,
            String versionRegla,
            String advertencia,
            List<PuntoComparacion> puntos
    ) {
    }

    public record PuntoComparacion(
            int anio,
            Long cargaId,
            BigDecimal valor,
            Integer cantidadMuestra,
            String estado,
            String mensaje
    ) {
    }
}



