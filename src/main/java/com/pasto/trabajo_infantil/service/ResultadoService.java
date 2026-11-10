package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.resultado.ResultadoResponse;
import com.pasto.trabajo_infantil.entity.CargaDatos;
import com.pasto.trabajo_infantil.entity.Periodo;
import com.pasto.trabajo_infantil.entity.Resultado;
import com.pasto.trabajo_infantil.enums.EstadoCarga;
import com.pasto.trabajo_infantil.enums.EstadoPeriodo;
import com.pasto.trabajo_infantil.exception.RecursoNoEncontradoException;
import com.pasto.trabajo_infantil.repository.CargaDatosRepository;
import com.pasto.trabajo_infantil.repository.PeriodoRepository;
import com.pasto.trabajo_infantil.repository.ResultadoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ResultadoService {

    private final PeriodoRepository periodoRepository;
    private final CargaDatosRepository cargaDatosRepository;
    private final ResultadoRepository resultadoRepository;

    public ResultadoService(
            PeriodoRepository periodoRepository,
            CargaDatosRepository cargaDatosRepository,
            ResultadoRepository resultadoRepository
    ) {
        this.periodoRepository = periodoRepository;
        this.cargaDatosRepository = cargaDatosRepository;
        this.resultadoRepository = resultadoRepository;
    }

    @Transactional(readOnly = true)
    public List<ResultadoResponse> consultarResultados(
            Long periodoId,
            String criterio,
            String grupo
    ) {
        String criterioLimpio = limpiarFiltro(criterio);
        String grupoLimpio = limpiarFiltro(grupo);

        if (grupoLimpio != null && criterioLimpio == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar un criterio cuando selecciones un grupo."
            );
        }

        Periodo periodo = periodoRepository.findById(periodoId)
                .filter(p -> p.getEstado() == EstadoPeriodo.DISPONIBLE)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El periodo solicitado no está disponible."
                        )
                );

        CargaDatos carga = cargaDatosRepository
                .findByPeriodo_IdAndVigenteTrueAndEstado(
                        periodo.getId(),
                        EstadoCarga.COMPLETADA
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "El periodo no tiene una carga disponible."
                        )
                );

        return resultadoRepository
                .findByCarga_IdOrderByIdAsc(carga.getId())
                .stream()
                .filter(resultado ->
                        coincide(
                                resultado.getCriterioAgrupacion(),
                                criterioLimpio
                        )
                )
                .filter(resultado ->
                        coincide(
                                resultado.getGrupo(),
                                grupoLimpio
                        )
                )
                .map(this::convertirARespuesta)
                .toList();
    }

    private String limpiarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private boolean coincide(String valorGuardado, String filtro) {
        return filtro == null || valorGuardado.equalsIgnoreCase(filtro);
    }

    private ResultadoResponse convertirARespuesta(Resultado resultado) {
        Periodo periodo = resultado.getCarga().getPeriodo();

        return new ResultadoResponse(
                resultado.getId(),
                periodo.getAnio(),
                periodo.getTrimestre(),
                resultado.getIndicador().getCodigo(),
                resultado.getIndicador().getNombre(),
                resultado.getIndicador().getVersionRegla(),
                resultado.getIndicador().getUnidad(),
                resultado.getTerritorio().getNombre(),
                resultado.getCriterioAgrupacion(),
                resultado.getGrupo(),
                resultado.getValor(),
                resultado.getNumerador(),
                resultado.getDenominador(),
                resultado.getCantidadMuestra(),
                resultado.getMotivoNoDisponible(),
                resultado.getCalculadoEn()
        );
    }
}