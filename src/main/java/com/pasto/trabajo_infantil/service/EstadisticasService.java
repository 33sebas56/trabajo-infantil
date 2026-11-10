
package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.resultado.ResultadoResponse;

import com.pasto.trabajo_infantil.entity.CargaDatos;
import com.pasto.trabajo_infantil.entity.Periodo;

import com.pasto.trabajo_infantil.enums.EstadoCarga;
import com.pasto.trabajo_infantil.enums.EstadoPeriodo;

import com.pasto.trabajo_infantil.repository.CargaDatosRepository;
import com.pasto.trabajo_infantil.repository.PeriodoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class EstadisticasService {

    private static final String SALARIO =
            "SALARIO_MENSUAL_P409";

    private static final String GANANCIA =
            "GANANCIA_MENSUAL_P413";

    private static final String DISPONIBLE =
            "DISPONIBLE";

    private static final String NO_DISPONIBLE =
            "NO_DISPONIBLE";

    private static final String NO_COMPARABLE =
            "NO_COMPARABLE";

    private static final String PENDIENTE_REVISION =
            "PENDIENTE_REVISION";

    private static final String INFORMACION_INSUFICIENTE =
            "INFORMACION_INSUFICIENTE";

    /*
     * Catálogo de las estadísticas preparadas en el backend.
     *
     * Los nombres son los que podrá utilizar el frontend.
     * Los códigos se utilizan internamente para buscar
     * cada resultado.
     */
    private static final List<Definicion> CATALOGO = List.of(

            new Definicion(
                    "POBLACION_EXPANDIDA_5_17",
                    "Población estimada de 5 a 17 años",
                    "PERSONAS"
            ),

            new Definicion(
                    "RESPUESTAS_AFIRMATIVAS_TRABAJO",
                    "Respuestas sobre actividad laboral",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "POBLACION_CON_RESPUESTAS_TRABAJO",
                    "Población con respuestas sobre actividad laboral",
                    "PERSONAS"
            ),

            new Definicion(
                    "SABE_LEER_ESCRIBIR",
                    "Sabe leer y escribir",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "ASISTE_INSTITUCION",
                    "Asiste a una institución educativa",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "LECTURA_CON_ACTIVIDAD",
                    "Lectura y escritura entre quienes "
                            + "reportaron actividad laboral",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "ASISTENCIA_CON_ACTIVIDAD",
                    "Asistencia educativa entre quienes "
                            + "reportaron actividad laboral",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "COCINA_PLATOS_MESA_HOGAR",
                    "Cocinar, lavar platos o poner la mesa "
                            + "en el hogar",
                    "PORCENTAJE"
            ),

            new Definicion(
                    SALARIO,
                    "Ingreso mensual por empleo reportado",
                    "COP_MENSUAL"
            ),

            new Definicion(
                    GANANCIA,
                    "Ganancia mensual independiente reportada",
                    "COP_MENSUAL"
            ),

            new Definicion(
                    "MOTIVO_TRABAJO_P405_1",
                    "Motivo: costear sus estudios",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "MOTIVO_TRABAJO_P405_2",
                    "Motivo: ayudar con los gastos del hogar",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "MOTIVO_TRABAJO_P405_3",
                    "Motivo: participar en la actividad familiar",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "MOTIVO_TRABAJO_P405_4",
                    "Motivo: considera que el trabajo le forma",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "MOTIVO_TRABAJO_P405_5",
                    "Motivo: considera que el trabajo "
                            + "le aleja de los vicios",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "MOTIVO_TRABAJO_P405_6",
                    "Motivo: tener su propio dinero",
                    "PORCENTAJE"
            ),

            new Definicion(
                    "MOTIVO_TRABAJO_P405_7",
                    "Motivo: otra razón",
                    "PORCENTAJE"
            )
    );

    private final PeriodoRepository periodoRepository;
    private final CargaDatosRepository cargaRepository;

    private final ResultadoService resultadoService;
    private final ProcesamientoService procesamientoService;

    public EstadisticasService(
            PeriodoRepository periodoRepository,
            CargaDatosRepository cargaRepository,
            ResultadoService resultadoService,
            ProcesamientoService procesamientoService
    ) {
        this.periodoRepository = periodoRepository;
        this.cargaRepository = cargaRepository;
        this.resultadoService = resultadoService;
        this.procesamientoService = procesamientoService;
    }

    /**
     * Consulta las estadísticas de un periodo.
     */
    @Transactional(readOnly = true)
    public Resumen consultar(
            int anio,
            int trimestre,
            String edad,
            String sexo,
            boolean administracion
    ) {
        validarPeriodo(anio, trimestre);

        Filtros filtros = filtros(edad, sexo);

        return consultarPeriodo(
                anio,
                trimestre,
                filtros,
                administracion
        );
    }

    /**
     * Compara un indicador entre varios años.
     */
    @Transactional(readOnly = true)
    public Comparacion comparar(
            String codigo,
            int desde,
            int hasta,
            int trimestre,
            String edad,
            String sexo,
            boolean administracion
    ) {
        validarPeriodo(desde, trimestre);
        validarPeriodo(hasta, trimestre);

        if (
                desde > hasta
                        || hasta - desde > 30
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecciona un rango válido de hasta 31 años."
            );
        }

        Definicion definicion =
                buscarDefinicion(codigo);

        Filtros filtros =
                filtros(edad, sexo);

        List<Punto> puntos =
                new ArrayList<>();

        for (int anio = desde; anio <= hasta; anio++) {

            Resumen resumen = consultarPeriodo(
                    anio,
                    trimestre,
                    filtros,
                    administracion
            );

            Dato dato = resumen.indicadores()
                    .stream()
                    .filter(indicador ->
                            definicion.codigo().equals(
                                    indicador.codigo()
                            )
                    )
                    .findFirst()
                    .orElseThrow();

            String estado = dato.estado();
            String mensaje = dato.mensaje();

            BigDecimal valor = dato.valor();
            Integer muestra = dato.muestra();

            /*
             * La versión se revisa por periodo. Así se reconoce
             * la carga de 2022 guardada sin modificar sus datos.
             */
            if (
                    DISPONIBLE.equals(estado)
                            && !PoliticaVersiones.comparable(
                            anio,
                            trimestre,
                            dato.versionRegla()
                    )
            ) {
                estado = NO_COMPARABLE;

                mensaje =
                        "La regla de cálculo de este periodo "
                                + "necesita revisión antes de compararse.";

                valor = null;
                muestra = null;
            }

            puntos.add(
                    new Punto(
                            anio,
                            valor,
                            estado,
                            mensaje,
                            muestra,
                            dato.versionRegla()
                    )
            );
        }

        boolean tiene2022O2023 = puntos.stream().anyMatch(punto ->
                DISPONIBLE.equals(punto.estado())
                        && punto.anio() >= 2022 && punto.anio() <= 2023
        );

        boolean tiene2024O2025 = puntos.stream().anyMatch(punto ->
                DISPONIBLE.equals(punto.estado())
                        && punto.anio() >= 2024 && punto.anio() <= 2025
        );

        String advertencia = PoliticaVersiones.requiereRevisionEntreGrupos(
                definicion.codigo(), tiene2022O2023, tiene2024O2025
        ) ? PoliticaVersiones.AVISO_CAMBIO_PREGUNTA : null;

        return new Comparacion(
                definicion.codigo(),
                definicion.nombre(),
                definicion.unidad(),
                trimestre,
                filtros.edad(),
                filtros.sexo(),
                List.copyOf(puntos),
                advertencia
        );
    }

    private Resumen consultarPeriodo(
            int anio,
            int trimestre,
            Filtros filtros,
            boolean administracion
    ) {
        Optional<Periodo> periodo =
                periodoRepository.findByAnioAndTrimestre(
                        anio,
                        trimestre
                );

        String sinDatos = administracion
                ? "No hay una carga disponible para este periodo."
                : "No hay resultados publicados para este periodo.";

        if (
                periodo.isEmpty()
                        || (
                        !administracion
                                && periodo.get().getEstado()
                                != EstadoPeriodo.DISPONIBLE
                )
        ) {
            return resumenSinDatos(
                    anio,
                    trimestre,
                    filtros,
                    sinDatos
            );
        }

        Optional<CargaDatos> carga;

        if (administracion) {
            carga = buscarCargaAdministrativa(
                    periodo.get().getId()
            );

        } else {
            carga = cargaRepository
                    .findByPeriodo_IdAndVigenteTrueAndEstado(
                            periodo.get().getId(),
                            EstadoCarga.COMPLETADA
                    );
        }

        if (carga.isEmpty()) {
            return resumenSinDatos(
                    anio,
                    trimestre,
                    filtros,
                    sinDatos
            );
        }

        List<ResultadoResponse> resultados;

        if (administracion) {
            resultados =
                    procesamientoService.consultarResultadosPrivados(
                            carga.get().getId()
                    );

        } else {
            resultados =
                    resultadoService.consultarResultados(
                            periodo.get().getId(),
                            filtros.criterio(),
                            filtros.grupo()
                    );
        }

        Map<String, ResultadoResponse> porCodigo =
                new LinkedHashMap<>();

        for (ResultadoResponse resultado : resultados) {

            boolean mismoCriterio =
                    filtros.criterio().equalsIgnoreCase(
                            resultado.criterioAgrupacion()
                    );

            boolean mismoGrupo =
                    filtros.grupo().equalsIgnoreCase(
                            resultado.grupo()
                    );

            if (mismoCriterio && mismoGrupo) {
                porCodigo.putIfAbsent(
                        resultado.codigoIndicador(),
                        resultado
                );
            }
        }

        List<Dato> datos = CATALOGO.stream()
                .map(definicion ->
                        convertir(
                                definicion,
                                porCodigo.get(
                                        definicion.codigo()
                                ),
                                administracion
                        )
                )
                .toList();

        return new Resumen(
                anio,
                trimestre,
                filtros.edad(),
                filtros.sexo(),
                DISPONIBLE,
                null,
                administracion
                        ? carga.get().getId()
                        : null,
                datos
        );
    }

    /**
     * Para administración buscamos la última carga
     * validada o completada que tenga resultados.
     */
    private Optional<CargaDatos> buscarCargaAdministrativa(
            Long periodoId
    ) {
        return cargaRepository
                .findByPeriodo_IdOrderByIniciadoEnDesc(
                        periodoId
                )
                .stream()
                .filter(carga ->
                        carga.getEstado() == EstadoCarga.VALIDADA
                                || carga.getEstado()
                                == EstadoCarga.COMPLETADA
                )
                .filter(carga ->
                        !procesamientoService
                                .consultarResultadosPrivados(
                                        carga.getId()
                                )
                                .isEmpty()
                )
                .findFirst();
    }

    private Dato convertir(
            Definicion definicion,
            ResultadoResponse resultado,
            boolean administracion
    ) {
        if (resultado == null) {
            return new Dato(
                    definicion.codigo(),
                    definicion.nombre(),
                    definicion.unidad(),
                    null,
                    NO_DISPONIBLE,
                    "No hay información disponible "
                            + "para este periodo y filtros.",
                    null,
                    null
            );
        }

        if (resultado.valor() == null) {

            String mensaje =
                    resultado.motivoNoDisponible() == null
                            ? "No hay respuestas disponibles "
                            + "para este indicador."
                            : resultado.motivoNoDisponible();

            boolean insuficiente =
                    mensaje.startsWith(
                            "Hay una sola respuesta de ingreso"
                    );

            if (!administracion && insuficiente) {
                mensaje =
                        "No hay suficientes respuestas "
                                + "para mostrar el ingreso.";
            }

            return new Dato(
                    definicion.codigo(),
                    definicion.nombre(),
                    definicion.unidad(),
                    null,
                    insuficiente
                            ? INFORMACION_INSUFICIENTE
                            : NO_DISPONIBLE,
                    mensaje,
                    administracion
                            ? resultado.cantidadMuestra()
                            : null,
                    resultado.versionRegla()
            );
        }

        /*
         * Los ingresos se pueden revisar en administración.
         * Todavía no se entregan como valores públicos.
         */
        boolean esIngreso =
                SALARIO.equals(definicion.codigo())
                        || GANANCIA.equals(
                        definicion.codigo()
                );

        if (!administracion && esIngreso) {
            return new Dato(
                    definicion.codigo(),
                    definicion.nombre(),
                    definicion.unidad(),
                    null,
                    PENDIENTE_REVISION,
                    "La información de ingresos está pendiente "
                            + "de revisión para su consulta pública.",
                    null,
                    resultado.versionRegla()
            );
        }

        return new Dato(
                definicion.codigo(),
                definicion.nombre(),
                definicion.unidad(),
                resultado.valor(),
                DISPONIBLE,
                resultado.motivoNoDisponible(),
                resultado.cantidadMuestra(),
                resultado.versionRegla()
        );
    }

    private Resumen resumenSinDatos(
            int anio,
            int trimestre,
            Filtros filtros,
            String mensaje
    ) {
        List<Dato> datos = CATALOGO.stream()
                .map(definicion ->
                        new Dato(
                                definicion.codigo(),
                                definicion.nombre(),
                                definicion.unidad(),
                                null,
                                NO_DISPONIBLE,
                                mensaje,
                                null,
                                null
                        )
                )
                .toList();

        return new Resumen(
                anio,
                trimestre,
                filtros.edad(),
                filtros.sexo(),
                NO_DISPONIBLE,
                mensaje,
                null,
                datos
        );
    }

    private void validarPeriodo(
            int anio,
            int trimestre
    ) {
        if (
                anio < 1900
                        || anio > 2100
                        || trimestre < 1
                        || trimestre > 4
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecciona un año y trimestre válidos."
            );
        }
    }

    private Definicion buscarDefinicion(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecciona un indicador."
            );
        }

        return CATALOGO.stream()
                .filter(definicion ->
                        definicion.codigo()
                                .equalsIgnoreCase(codigo.trim())
                )
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El indicador solicitado no existe."
                        )
                );
    }

    private Filtros filtros(
            String edad,
            String sexo
    ) {
        String edadLimpia = normalizar(
                edad,
                "TODAS"
        );

        String sexoLimpio = normalizar(
                sexo,
                "TODOS"
        );

        if (
                !List.of(
                        "TODAS",
                        "5_14",
                        "15_17"
                ).contains(edadLimpia)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecciona un grupo de edad válido."
            );
        }

        if (
                !List.of(
                        "TODOS",
                        "MASCULINO",
                        "FEMENINO",
                        "SIN_DATO"
                ).contains(sexoLimpio)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selecciona un sexo válido."
            );
        }

        if (
                "TODAS".equals(edadLimpia)
                        && "TODOS".equals(sexoLimpio)
        ) {
            return new Filtros(
                    edadLimpia,
                    sexoLimpio,
                    "GENERAL",
                    "TOTAL"
            );
        }

        if ("TODAS".equals(edadLimpia)) {
            return new Filtros(
                    edadLimpia,
                    sexoLimpio,
                    "SEXO",
                    sexoLimpio
            );
        }

        if ("TODOS".equals(sexoLimpio)) {
            return new Filtros(
                    edadLimpia,
                    sexoLimpio,
                    "EDAD",
                    edadLimpia
            );
        }

        return new Filtros(
                edadLimpia,
                sexoLimpio,
                "EDAD_SEXO",
                edadLimpia + "_" + sexoLimpio
        );
    }

    private String normalizar(
            String valor,
            String predeterminado
    ) {
        if (valor == null || valor.isBlank()) {
            return predeterminado;
        }

        return valor
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private record Definicion(
            String codigo,
            String nombre,
            String unidad
    ) {
    }

    private record Filtros(
            String edad,
            String sexo,
            String criterio,
            String grupo
    ) {
    }

    public record Dato(
            String codigo,
            String nombre,
            String unidad,
            BigDecimal valor,
            String estado,
            String mensaje,
            Integer muestra,
            String versionRegla
    ) {
    }

    public record Resumen(
            int anio,
            int trimestre,
            String edad,
            String sexo,
            String estado,
            String mensaje,
            Long cargaId,
            List<Dato> indicadores
    ) {
    }

    public record Punto(
            int anio,
            BigDecimal valor,
            String estado,
            String mensaje,
            Integer muestra,
            String versionRegla
    ) {
    }

    public record Comparacion(
            String indicador,
            String nombre,
            String unidad,
            int trimestre,
            String edad,
            String sexo,
            List<Punto> puntos,
            String advertencia
    ) {
    }
}



