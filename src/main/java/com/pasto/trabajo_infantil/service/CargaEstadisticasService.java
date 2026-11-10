package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.carga.CargaResponse;

import com.pasto.trabajo_infantil.entity.CargaDatos;
import com.pasto.trabajo_infantil.entity.Indicador;
import com.pasto.trabajo_infantil.entity.Resultado;
import com.pasto.trabajo_infantil.entity.Territorio;

import com.pasto.trabajo_infantil.enums.EstadoCarga;

import com.pasto.trabajo_infantil.exception.ArchivoInvalidoException;

import com.pasto.trabajo_infantil.repository.CargaDatosRepository;
import com.pasto.trabajo_infantil.repository.IndicadorRepository;
import com.pasto.trabajo_infantil.repository.ResultadoRepository;

import com.pasto.trabajo_infantil.service.procesamiento
        .AcumuladorEstadisticas;

import com.pasto.trabajo_infantil.service.procesamiento
        .AcumuladorEstadisticas.Detalle;

import com.pasto.trabajo_infantil.service.procesamiento
        .AcumuladorEstadisticas.Fila;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.UncheckedIOException;

import java.math.BigDecimal;

import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CargaEstadisticasService {

    private static final String POBLACION =
            "POBLACION_EXPANDIDA_5_17";

    private static final String ACTIVIDAD =
            "RESPUESTAS_AFIRMATIVAS_TRABAJO";

    private static final String ACTIVIDAD_PERSONAS =
            "POBLACION_CON_RESPUESTAS_TRABAJO";

    private static final String LECTURA =
            "SABE_LEER_ESCRIBIR";

    private static final String ASISTENCIA =
            "ASISTE_INSTITUCION";

    private static final String LECTURA_ACTIVIDAD =
            "LECTURA_CON_ACTIVIDAD";

    private static final String ASISTENCIA_ACTIVIDAD =
            "ASISTENCIA_CON_ACTIVIDAD";

    private static final String COCINA =
            "COCINA_PLATOS_MESA_HOGAR";

    private static final String SALARIO =
            "SALARIO_MENSUAL_P409";

    private static final String GANANCIA =
            "GANANCIA_MENSUAL_P413";

    private static final String PREFIJO_MOTIVO =
            "MOTIVO_TRABAJO_P405_";

    private static final Set<String> OPCIONES_ACTIVIDAD =
            Set.of("1", "2", "3", "4", "5", "6");

    private static final List<String> MOTIVOS = List.of(
            "Costear sus estudios",
            "Ayudar con los gastos del hogar",
            "Participar en la actividad económica familiar",
            "Considera que el trabajo le forma",
            "Considera que el trabajo le aleja de los vicios",
            "Tener su propio dinero",
            "Otra razón"
    );

    private final ComparacionCargaService cargaOriginal;
    private final CargaDatosRepository cargaRepository;
    private final ResultadoRepository resultadoRepository;
    private final IndicadorRepository indicadorRepository;

    public CargaEstadisticasService(
            ComparacionCargaService cargaOriginal,
            CargaDatosRepository cargaRepository,
            ResultadoRepository resultadoRepository,
            IndicadorRepository indicadorRepository
    ) {
        this.cargaOriginal = cargaOriginal;
        this.cargaRepository = cargaRepository;
        this.resultadoRepository = resultadoRepository;
        this.indicadorRepository = indicadorRepository;
    }

    /**
     * Ejecuta la carga que ya teníamos y agrega las nuevas
     * estadísticas dentro de la misma transacción.
     */
    @Transactional
    public CargaResponse cargarYCalcular(
            MultipartFile archivo,
            Integer anio,
            Integer trimestre,
            String correoAdministrador
    ) {
        CargaResponse respuesta = cargaOriginal.cargarYCalcular(
                archivo,
                anio,
                trimestre,
                correoAdministrador
        );

        if (respuesta.estado() != EstadoCarga.VALIDADA) {
            return respuesta;
        }

        CargaDatos carga = cargaRepository
                .findById(respuesta.id())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró la carga recién creada."
                        )
                );

        List<Resultado> anteriores =
                resultadoRepository.findByCarga_IdOrderByIdAsc(
                        respuesta.id()
                );

        Resultado base = anteriores.stream()
                .filter(resultado ->
                        POBLACION.equals(
                                resultado.getIndicador().getCodigo()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Falta el resultado base de la carga."
                        )
                );

        Territorio territorio = base.getTerritorio();

        String version =
                base.getIndicador().getVersionRegla();

        Lectura lectura = leer(archivo);

        /*
         * Guardamos los indicadores encontrados durante
         * esta carga para evitar consultas repetidas.
         */
        Map<String, Indicador> cache = new HashMap<>();

        for (Resultado resultado : anteriores) {
            cache.put(
                    resultado.getIndicador().getCodigo(),
                    resultado.getIndicador()
            );
        }

        List<Detalle> detalles =
                new ArrayList<>(lectura.detalles());

        /*
         * Si no se pudo formar ningún grupo, dejamos
         * constancia de que las estadísticas no tienen
         * registros válidos.
         */
        if (detalles.isEmpty()) {
            for (String codigo : codigosNuevos()) {
                detalles.add(
                        new Detalle(
                                codigo,
                                "GENERAL",
                                "TOTAL",
                                null,
                                null,
                                null,
                                0,
                                0,
                                0,
                                "No hay registros válidos "
                                        + "para calcular esta estadística."
                        )
                );
            }
        }

        List<Resultado> nuevos = new ArrayList<>();

        for (Detalle detalle : detalles) {
            if (esResultadoGeneralExistente(detalle)) {
                continue;
            }

            Definicion definicion =
                    definicion(detalle.codigoIndicador());

            Indicador indicador = cache.computeIfAbsent(
                    detalle.codigoIndicador(),
                    codigo ->
                            indicadorRepository
                                    .findByCodigoAndVersionRegla(
                                            codigo,
                                            version
                                    )
                                    .orElseGet(() ->
                                            crearIndicador(
                                                    codigo,
                                                    version,
                                                    definicion
                                            )
                                    )
            );

            Resultado resultado = convertir(
                    detalle,
                    definicion,
                    lectura.columnas(),
                    carga,
                    territorio,
                    indicador
            );

            nuevos.add(resultado);
        }

        resultadoRepository.saveAll(nuevos);

        return respuesta;
    }

    private Resultado convertir(
            Detalle detalle,
            Definicion definicion,
            Set<String> columnas,
            CargaDatos carga,
            Territorio territorio,
            Indicador indicador
    ) {
        Resultado resultado = new Resultado();

        resultado.setCarga(carga);
        resultado.setTerritorio(territorio);
        resultado.setIndicador(indicador);

        resultado.setCriterioAgrupacion(
                detalle.criterio()
        );

        resultado.setGrupo(
                detalle.grupo()
        );

        List<String> requeridas = new ArrayList<>(
                List.of(
                        "DIRECTORIO",
                        "SECUENCIA_P",
                        "ORDEN",
                        "P6040",
                        "FEX_C"
                )
        );

        requeridas.addAll(definicion.columnas());

        List<String> faltantes = requeridas.stream()
                .distinct()
                .filter(columna ->
                        !columnas.contains(columna)
                )
                .toList();

        if (!faltantes.isEmpty()) {
            resultado.setCantidadMuestra(0);
            resultado.setMuestraNumerador(0);
            resultado.setMuestraDenominador(0);

            List<String> nombres = faltantes.stream()
                    .map(this::nombreCampo)
                    .distinct()
                    .toList();

            resultado.setMotivoNoDisponible(
                    "El archivo no incluye datos necesarios: "
                            + String.join(", ", nombres)
                            + "."
            );

            return resultado;
        }

        resultado.setValor(
                detalle.valor()
        );

        resultado.setNumerador(
                detalle.numerador()
        );

        resultado.setDenominador(
                detalle.denominador()
        );

        /*
         * La muestra mostrada para este indicador es
         * la cantidad de respuestas válidas utilizadas.
         */
        resultado.setCantidadMuestra(
                detalle.muestraDenominador()
        );

        resultado.setMuestraNumerador(
                detalle.muestraNumerador()
        );

        resultado.setMuestraDenominador(
                detalle.muestraDenominador()
        );

        String observacion =
                detalle.motivoNoDisponible();

        if (
                detalle.valor() != null
                        && carga.getRegistrosInvalidos() != null
                        && carga.getRegistrosInvalidos() > 0
        ) {
            observacion =
                    "Se utilizaron los registros válidos; "
                            + "otros registros tienen datos básicos incompletos.";

        } else if (
                detalle.valor() != null
                        && esPreguntaGeneral(
                        detalle.codigoIndicador()
                )
                        && detalle.muestraDenominador()
                        < detalle.muestraGrupo()
        ) {
            observacion =
                    "Calculado con las respuestas válidas; "
                            + "algunas respuestas no están disponibles.";
        }

        resultado.setMotivoNoDisponible(observacion);

        return resultado;
    }

    private boolean esPreguntaGeneral(String codigo) {
        return POBLACION.equals(codigo)
                || ACTIVIDAD.equals(codigo)
                || ACTIVIDAD_PERSONAS.equals(codigo)
                || LECTURA.equals(codigo)
                || ASISTENCIA.equals(codigo)
                || COCINA.equals(codigo);
    }

    private String nombreCampo(String campo) {
        return switch (campo) {

            case "DIRECTORIO", "SECUENCIA_P", "ORDEN" ->
                    "identificadores de la encuesta";

            case "P6040" ->
                    "edad";

            case "FEX_C" ->
                    "factor de expansión";

            case "P400", "P401", "P402", "P403" ->
                    "respuestas sobre actividad laboral";

            case "P6160" ->
                    "lectura y escritura";

            case "P6170" ->
                    "asistencia educativa";

            case "P3131S1" ->
                    "tareas en el hogar";

            case "P409" ->
                    "ingreso mensual por empleo";

            case "P413" ->
                    "ganancia mensual independiente";

            case "P405" ->
                    "motivo del trabajo";

            default ->
                    "datos de la encuesta";
        };
    }

    private boolean esResultadoGeneralExistente(
            Detalle detalle
    ) {
        return "GENERAL".equals(detalle.criterio())
                && "TOTAL".equals(detalle.grupo())
                && (
                POBLACION.equals(
                        detalle.codigoIndicador()
                )
                        || ACTIVIDAD.equals(
                        detalle.codigoIndicador()
                )
        );
    }

    private Indicador crearIndicador(
            String codigo,
            String version,
            Definicion definicion
    ) {
        Indicador indicador = new Indicador();

        indicador.setCodigo(codigo);
        indicador.setNombre(definicion.nombre());
        indicador.setDescripcion(definicion.descripcion());
        indicador.setUnidad(definicion.unidad());
        indicador.setReglaCalculo(definicion.regla());
        indicador.setVersionRegla(version);

        /*
         * El indicador se activa en el catálogo público
         * cuando el administrador publica la carga.
         */
        indicador.setActivo(false);

        return indicadorRepository.save(indicador);
    }

    private record Definicion(
            String nombre,
            String descripcion,
            String unidad,
            String regla,
            List<String> columnas
    ) {
    }

    private Definicion definicion(String codigo) {
        String porcentaje =
                "Suma de factores de quienes cumplen la condición, "
                        + "dividida por la suma de factores con respuesta "
                        + "válida, multiplicada por 100.";

        List<String> trabajo = List.of(
                "P400",
                "P401",
                "P402",
                "P403"
        );

        return switch (codigo) {

            case POBLACION -> new Definicion(
                    "Población estimada de 5 a 17 años",
                    "Población estimada de la zona de Pasto "
                            + "y del grupo seleccionado.",
                    "PERSONAS",
                    "Suma de FEX_C de las personas con "
                            + "edad y factor válidos.",
                    List.of("P6040", "FEX_C")
            );

            case ACTIVIDAD -> new Definicion(
                    "Respuestas sobre actividad laboral",
                    "Proporción con alguna respuesta afirmativa "
                            + "en las preguntas laborales usadas "
                            + "por el sistema.",
                    "PORCENTAJE",
                    porcentaje,
                    trabajo
            );

            case ACTIVIDAD_PERSONAS -> new Definicion(
                    "Población con respuestas sobre actividad laboral",
                    "Suma de factores de personas con alguna "
                            + "respuesta afirmativa laboral.",
                    "PERSONAS",
                    "Suma de FEX_C cuando P400, P401, "
                            + "P402 o P403 es 1.",
                    trabajo
            );

            case LECTURA -> new Definicion(
                    "Sabe leer y escribir",
                    "Porcentaje entre personas con respuesta "
                            + "válida a la pregunta de lectura "
                            + "y escritura.",
                    "PORCENTAJE",
                    porcentaje,
                    List.of("P6160")
            );

            case ASISTENCIA -> new Definicion(
                    "Asiste a una institución educativa",
                    "Porcentaje entre personas con respuesta "
                            + "válida sobre asistencia educativa.",
                    "PORCENTAJE",
                    porcentaje,
                    List.of("P6170")
            );

            case LECTURA_ACTIVIDAD -> new Definicion(
                    "Lectura y escritura entre quienes reportaron "
                            + "actividad laboral",
                    "Porcentaje de respuestas afirmativas de lectura "
                            + "entre quienes reportaron actividad laboral.",
                    "PORCENTAJE",
                    porcentaje,
                    List.of(
                            "P400",
                            "P401",
                            "P402",
                            "P403",
                            "P6160"
                    )
            );

            case ASISTENCIA_ACTIVIDAD -> new Definicion(
                    "Asistencia educativa entre quienes reportaron "
                            + "actividad laboral",
                    "Porcentaje de asistencia entre quienes reportaron "
                            + "actividad laboral y respondieron "
                            + "la pregunta.",
                    "PORCENTAJE",
                    porcentaje,
                    List.of(
                            "P400",
                            "P401",
                            "P402",
                            "P403",
                            "P6170"
                    )
            );

            case COCINA -> new Definicion(
                    "Cocinar, lavar platos o poner la mesa en el hogar",
                    "Respuesta sobre estas tareas sin remuneración "
                            + "durante la semana anterior. "
                            + "No define trabajo infantil por sí sola.",
                    "PORCENTAJE",
                    porcentaje,
                    List.of("P3131S1")
            );

            case SALARIO -> new Definicion(
                    "Ingreso mensual por empleo reportado",
                    "Promedio de P409 entre quienes reportaron "
                            + "un monto válido; pesos corrientes, "
                            + "sin ajustar por inflación.",
                    "COP_MENSUAL",
                    "Suma de P409 por FEX_C dividida por "
                            + "los factores con monto válido.",
                    List.of("P409")
            );

            case GANANCIA -> new Definicion(
                    "Ganancia mensual por actividad independiente reportada",
                    "Promedio de P413 entre quienes reportaron "
                            + "un monto válido; pesos corrientes, "
                            + "sin ajustar por inflación.",
                    "COP_MENSUAL",
                    "Suma de P413 por FEX_C dividida por "
                            + "los factores con monto válido.",
                    List.of("P413")
            );

            default ->
                    definicionMotivo(codigo, porcentaje);
        };
    }

    private Definicion definicionMotivo(
            String codigo,
            String regla
    ) {
        if (
                !codigo.matches(
                        PREFIJO_MOTIVO + "[1-7]"
                )
        ) {
            throw new IllegalArgumentException(
                    "Indicador no configurado: " + codigo
            );
        }

        int numero = Integer.parseInt(
                codigo.substring(
                        PREFIJO_MOTIVO.length()
                )
        );

        return new Definicion(
                "Motivo: " + MOTIVOS.get(numero - 1),
                "Distribución de la razón principal entre "
                        + "quienes respondieron P405.",
                "PORCENTAJE",
                regla,
                List.of("P405")
        );
    }

    private List<String> codigosNuevos() {
        List<String> codigos = new ArrayList<>(
                List.of(
                        ACTIVIDAD_PERSONAS,
                        LECTURA,
                        ASISTENCIA,
                        LECTURA_ACTIVIDAD,
                        ASISTENCIA_ACTIVIDAD,
                        COCINA,
                        SALARIO,
                        GANANCIA
                )
        );

        for (int i = 1; i <= 7; i++) {
            codigos.add(PREFIJO_MOTIVO + i);
        }

        return codigos;
    }

    private record Lectura(
            Set<String> columnas,
            List<Detalle> detalles
    ) {
    }

    private Lectura leer(MultipartFile archivo) {
        try (
                InputStreamReader lectorBase =
                        new InputStreamReader(
                                archivo.getInputStream(),
                                StandardCharsets.UTF_8
                                        .newDecoder()
                                        .onMalformedInput(
                                                CodingErrorAction.REPORT
                                        )
                                        .onUnmappableCharacter(
                                                CodingErrorAction.REPORT
                                        )
                        );

                PushbackReader entrada =
                        new PushbackReader(lectorBase, 1)
        ) {
            int primero = entrada.read();

            if (primero != -1 && primero != '\uFEFF') {
                entrada.unread(primero);
            }

            try (
                    BufferedReader lector =
                            new BufferedReader(entrada)
            ) {
                lector.mark(131072);

                String encabezado = lector.readLine();

                if (
                        encabezado == null
                                || encabezado.isBlank()
                ) {
                    throw new ArchivoInvalidoException(
                            "El archivo no contiene encabezados."
                    );
                }

                char separador =
                        detectarSeparador(encabezado);

                lector.reset();

                CSVFormat formato = CSVFormat.DEFAULT
                        .builder()
                        .setDelimiter(separador)
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true)
                        .setDuplicateHeaderMode(
                                DuplicateHeaderMode.DISALLOW
                        )
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .get();

                try (
                        CSVParser parser =
                                formato.parse(lector)
                ) {
                    Set<String> columnas =
                            new HashSet<>();

                    parser.getHeaderMap()
                            .keySet()
                            .forEach(columna ->
                                    columnas.add(
                                            columna.toUpperCase(
                                                    Locale.ROOT
                                            )
                                    )
                            );

                    AcumuladorEstadisticas acumulador =
                            new AcumuladorEstadisticas();

                    boolean tieneBase =
                            columnas.containsAll(
                                    List.of(
                                            "DIRECTORIO",
                                            "SECUENCIA_P",
                                            "ORDEN",
                                            "P6040",
                                            "FEX_C"
                                    )
                            );

                    boolean tieneActividad =
                            columnas.containsAll(
                                    List.of(
                                            "P400",
                                            "P401",
                                            "P402",
                                            "P403"
                                    )
                            );

                    for (CSVRecord registro : parser) {
                        if (!registro.isConsistent()) {
                            throw new ArchivoInvalidoException(
                                    "El archivo contiene "
                                            + "una fila incompleta."
                            );
                        }

                        if (
                                !"52".equals(
                                        valor(registro, "AREA")
                                )
                                        || !tieneBase
                        ) {
                            continue;
                        }

                        if (
                                valor(registro, "DIRECTORIO") == null
                                        || valor(
                                        registro,
                                        "SECUENCIA_P"
                                ) == null
                                        || valor(registro, "ORDEN") == null
                        ) {
                            continue;
                        }

                        Integer edad = edad(
                                valor(registro, "P6040")
                        );

                        BigDecimal factor = factor(
                                valor(registro, "FEX_C")
                        );

                        if (
                                edad == null
                                        || edad < 5
                                        || edad > 17
                                        || factor == null
                        ) {
                            continue;
                        }

                        Boolean actividad = actividad(
                                registro,
                                tieneActividad
                        );

                        acumulador.agregar(
                                new Fila(
                                        edad,
                                        valor(registro, "P3271"),
                                        factor,
                                        actividad,
                                        valor(registro, "P6160"),
                                        valor(registro, "P6170"),
                                        valor(registro, "P3131S1"),
                                        valor(registro, "P409"),
                                        valor(registro, "P413"),
                                        valor(registro, "P405")
                                )
                        );
                    }

                    return new Lectura(
                            Set.copyOf(columnas),
                            acumulador.resultados()
                    );
                }
            }

        } catch (
                IOException
                | UncheckedIOException
                | IllegalArgumentException excepcion
        ) {
            throw new ArchivoInvalidoException(
                    "No se pudieron calcular los detalles "
                            + "del CSV. Comprueba su formato."
            );
        }
    }

    private Boolean actividad(
            CSVRecord registro,
            boolean columnasCompletas
    ) {
        if (!columnasCompletas) {
            return null;
        }

        String p400 = valor(registro, "P400");
        String p401 = valor(registro, "P401");
        String p402 = valor(registro, "P402");
        String p403 = valor(registro, "P403");

        if (
                p400 == null
                        || !OPCIONES_ACTIVIDAD.contains(p400)
                        || !opcionalValida(p401)
                        || !opcionalValida(p402)
                        || !opcionalValida(p403)
        ) {
            return null;
        }

        return "1".equals(p400)
                || "1".equals(p401)
                || "1".equals(p402)
                || "1".equals(p403);
    }

    private boolean opcionalValida(String valor) {
        return valor == null
                || "1".equals(valor)
                || "2".equals(valor);
    }

    private String valor(
            CSVRecord registro,
            String columna
    ) {
        if (
                !registro.isMapped(columna)
                        || !registro.isSet(columna)
        ) {
            return null;
        }

        String valor = registro.get(columna);

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private Integer edad(String texto) {
        if (texto == null) {
            return null;
        }

        try {
            return Integer.valueOf(texto);

        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private BigDecimal factor(String texto) {
        if (texto == null) {
            return null;
        }

        try {
            BigDecimal factor = new BigDecimal(texto);

            return factor.signum() > 0
                    ? factor
                    : null;

        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private char detectarSeparador(String encabezado) {
        int comas = 0;
        int puntosYComas = 0;

        boolean entreComillas = false;

        for (
                int i = 0;
                i < encabezado.length();
                i++
        ) {
            char caracter = encabezado.charAt(i);

            if (caracter == '"') {

                if (
                        entreComillas
                                && i + 1 < encabezado.length()
                                && encabezado.charAt(i + 1) == '"'
                ) {
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }

            } else if (!entreComillas) {

                if (caracter == ',') {
                    comas++;

                } else if (caracter == ';') {
                    puntosYComas++;
                }
            }
        }

        if (
                entreComillas
                        || (comas == 0 && puntosYComas == 0)
                        || comas == puntosYComas
        ) {
            throw new ArchivoInvalidoException(
                    "No se pudo identificar el separador del CSV."
            );
        }

        return puntosYComas > comas
                ? ';'
                : ',';
    }
}