
package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.carga.CargaResponse;

import com.pasto.trabajo_infantil.entity.Administrador;
import com.pasto.trabajo_infantil.entity.CargaDatos;
import com.pasto.trabajo_infantil.entity.ErrorCarga;
import com.pasto.trabajo_infantil.entity.Indicador;
import com.pasto.trabajo_infantil.entity.Periodo;
import com.pasto.trabajo_infantil.entity.Resultado;
import com.pasto.trabajo_infantil.entity.Territorio;

import com.pasto.trabajo_infantil.enums.EstadoCarga;
import com.pasto.trabajo_infantil.enums.EstadoPeriodo;

import com.pasto.trabajo_infantil.exception.ArchivoInvalidoException;

import com.pasto.trabajo_infantil.repository.AdministradorRepository;
import com.pasto.trabajo_infantil.repository.CargaDatosRepository;
import com.pasto.trabajo_infantil.repository.ErrorCargaRepository;
import com.pasto.trabajo_infantil.repository.IndicadorRepository;
import com.pasto.trabajo_infantil.repository.PeriodoRepository;
import com.pasto.trabajo_infantil.repository.ResultadoRepository;
import com.pasto.trabajo_infantil.repository.TerritorioRepository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.UncheckedIOException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ComparacionCargaService {

    private static final String AREA_PASTO = "52";

    private static final String VERSION_COMUN = PoliticaVersiones.VERSION_COMUN;

    private static final long MAXIMO_BYTES = 20L * 1024 * 1024;

    private static final Set<String> ACTIVIDADES_VALIDAS = Set.of(
            "1", "2", "3", "4", "5", "6"
    );

    private final AdministradorRepository administradorRepository;
    private final PeriodoRepository periodoRepository;
    private final CargaDatosRepository cargaDatosRepository;
    private final ErrorCargaRepository errorCargaRepository;
    private final TerritorioRepository territorioRepository;
    private final IndicadorRepository indicadorRepository;
    private final ResultadoRepository resultadoRepository;

    public ComparacionCargaService(
            AdministradorRepository administradorRepository,
            PeriodoRepository periodoRepository,
            CargaDatosRepository cargaDatosRepository,
            ErrorCargaRepository errorCargaRepository,
            TerritorioRepository territorioRepository,
            IndicadorRepository indicadorRepository,
            ResultadoRepository resultadoRepository
    ) {
        this.administradorRepository = administradorRepository;
        this.periodoRepository = periodoRepository;
        this.cargaDatosRepository = cargaDatosRepository;
        this.errorCargaRepository = errorCargaRepository;
        this.territorioRepository = territorioRepository;
        this.indicadorRepository = indicadorRepository;
        this.resultadoRepository = resultadoRepository;
    }

    @Transactional
    public CargaResponse cargarYCalcular(
            MultipartFile archivo,
            Integer anio,
            Integer trimestre,
            String correoAdministrador
    ) {
        validarSolicitud(archivo, anio, trimestre);

        Administrador administrador = administradorRepository
                .findByCorreoIgnoreCase(correoAdministrador)
                .filter(Administrador::isActivo)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "La cuenta de administrador no está disponible."
                        )
                );

        Periodo periodo = periodoRepository
                .findByAnioAndTrimestre(anio, trimestre)
                .orElseGet(() -> crearPeriodo(anio, trimestre));

        CargaDatos carga = new CargaDatos();
        carga.setAdministrador(administrador);
        carga.setPeriodo(periodo);
        carga.setNombreArchivo(obtenerNombreSeguro(archivo));
        carga.setEstado(EstadoCarga.VALIDANDO);
        carga.setVigente(false);

        cargaDatosRepository.saveAndFlush(carga);

        try {
            ResumenArchivo resumen = analizarArchivo(archivo);

            carga.setRegistrosTotales(resumen.registrosTotales());
            carga.setRegistrosPasto(resumen.registrosPasto());
            carga.setRegistrosValidos(resumen.registrosValidos());
            carga.setRegistrosInvalidos(resumen.registrosInvalidos());

            String version = obtenerVersion(anio, trimestre);

            Territorio territorio = territorioRepository
                    .findByCodigoFuente(AREA_PASTO)
                    .orElseGet(this::crearTerritorioPasto);

            Indicador indicadorPoblacion =
                    obtenerIndicadorPoblacion(version);

            Indicador indicadorTrabajo =
                    obtenerIndicadorTrabajo(version);

            Resultado resultadoPoblacion = new Resultado();
            resultadoPoblacion.setCarga(carga);
            resultadoPoblacion.setIndicador(indicadorPoblacion);
            resultadoPoblacion.setTerritorio(territorio);
            resultadoPoblacion.setCriterioAgrupacion("GENERAL");
            resultadoPoblacion.setGrupo("TOTAL");

            if (resumen.poblacionDisponible()) {
                resultadoPoblacion.setValor(
                        resumen.poblacionExpandida()
                );

                resultadoPoblacion.setCantidadMuestra(resumen.registrosValidos());

            } else {
                resultadoPoblacion.setCantidadMuestra(0);
            }
            resultadoPoblacion.setMotivoNoDisponible(resumen.motivoPoblacion());

            Resultado resultadoTrabajo = new Resultado();
            resultadoTrabajo.setCarga(carga);
            resultadoTrabajo.setIndicador(indicadorTrabajo);
            resultadoTrabajo.setTerritorio(territorio);
            resultadoTrabajo.setCriterioAgrupacion("GENERAL");
            resultadoTrabajo.setGrupo("TOTAL");

            if (resumen.trabajoDisponible()) {
                resultadoTrabajo.setValor(
                        resumen.proporcionAfirmativa()
                );

                resultadoTrabajo.setNumerador(
                        resumen.respuestasAfirmativas()
                );

                resultadoTrabajo.setDenominador(
                        resumen.poblacionConRespuestaValida()
                );

                resultadoTrabajo.setCantidadMuestra(
                        resumen.registrosTrabajoValidos()
                );

            } else {
                resultadoTrabajo.setCantidadMuestra(0);
            }
            resultadoTrabajo.setMotivoNoDisponible(resumen.motivoTrabajo());

            resultadoRepository.saveAll(
                    List.of(resultadoPoblacion, resultadoTrabajo)
            );

            carga.setEstado(EstadoCarga.VALIDADA);
            carga.setVigente(false);

            if (!resumen.poblacionDisponible()
                    || !resumen.trabajoDisponible()) {

                carga.setMensajeResultado(
                        "El archivo se cargó. Uno o más indicadores "
                                + "están marcados como no disponibles."
                );

            } else if (!VERSION_COMUN.equals(version)) {

                carga.setMensajeResultado(
                        "El archivo se cargó con resultados. Revisa las variables "
                                + "y el periodo antes de publicarlos. La comparación "
                                + "con otros años requiere revisión."
                );

            } else if (resumen.registrosInvalidos() > 0
                    || resumen.registrosTrabajoValidos() < resumen.registrosValidos()) {

                carga.setMensajeResultado(
                        "Se guardaron resultados con los registros y respuestas válidos. "
                                + "Revisa los datos incompletos antes de publicarlos."
                );

            } else {
                carga.setMensajeResultado(
                        "Se guardaron los resultados. "
                                + "La carga aún no está publicada."
                );
            }

        } catch (ArchivoInvalidoException excepcion) {
            carga.setEstado(EstadoCarga.FALLIDA);
            carga.setVigente(false);

            carga.setMensajeResultado(
                    "No fue posible revisar el archivo. "
                            + "Consulta el detalle del error."
            );

            ErrorCarga error = new ErrorCarga();
            error.setCarga(carga);
            error.setDetalle(excepcion.getMessage());

            errorCargaRepository.save(error);
        }

        carga.setFinalizadoEn(Instant.now());

        CargaDatos guardada = cargaDatosRepository.save(carga);

        return convertirCarga(guardada);
    }

    private ResumenArchivo analizarArchivo(MultipartFile archivo) {
        try (
                InputStreamReader lectorBase = new InputStreamReader(
                        archivo.getInputStream(),
                        StandardCharsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                );

                PushbackReader entrada =
                        new PushbackReader(lectorBase, 1)
        ) {
            int primerCaracter = entrada.read();

            if (primerCaracter != -1 && primerCaracter != '\uFEFF') {
                entrada.unread(primerCaracter);
            }

            try (BufferedReader lector = new BufferedReader(entrada)) {
                lector.mark(131072);

                String encabezado = lector.readLine();

                if (encabezado == null || encabezado.isBlank()) {
                    throw new ArchivoInvalidoException(
                            "El archivo no contiene un encabezado."
                    );
                }

                char separador = detectarSeparador(encabezado);

                lector.reset();

                CSVFormat formato = CSVFormat.DEFAULT.builder()
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

                try (CSVParser parser = formato.parse(lector)) {
                    return leerRegistros(parser);
                }
            }

        } catch (IOException
                 | UncheckedIOException
                 | IllegalArgumentException excepcion) {

            throw new ArchivoInvalidoException(
                    "No fue posible leer el archivo. Comprueba que sea "
                            + "un CSV UTF-8 válido y que no tenga "
                            + "encabezados duplicados."
            );
        }
    }

    private ResumenArchivo leerRegistros(CSVParser parser) {
        Map<String, Integer> columnas = parser.getHeaderMap();

        if (!columnas.containsKey("AREA")) {
            throw new ArchivoInvalidoException(
                    "Falta la columna AREA. Sin ella no es posible "
                            + "identificar los registros de Pasto."
            );
        }

        boolean tieneIdentificadores =
                columnas.containsKey("DIRECTORIO")
                        && columnas.containsKey("SECUENCIA_P")
                        && columnas.containsKey("ORDEN");

        boolean tieneBase =
                columnas.containsKey("P6040")
                        && columnas.containsKey("FEX_C")
                        && tieneIdentificadores;

        boolean tienePreguntasTrabajo =
                columnas.containsKey("P400")
                        && columnas.containsKey("P401")
                        && columnas.containsKey("P402")
                        && columnas.containsKey("P403");

        int registrosTotales = 0;
        int registrosPasto = 0;
        int registrosValidos = 0;
        int registrosInvalidos = 0;
        int registrosTrabajoValidos = 0;

        BigDecimal sumaFactores = BigDecimal.ZERO;
        BigDecimal sumaFactoresTrabajo = BigDecimal.ZERO;
        BigDecimal sumaAfirmativas = BigDecimal.ZERO;

        Set<List<String>> identificadores = new HashSet<>();

        for (CSVRecord registro : parser) {
            registrosTotales++;

            if (!registro.isConsistent()) {
                throw new ArchivoInvalidoException(
                        "El archivo contiene un registro con una "
                                + "cantidad incorrecta de columnas."
                );
            }

            String area = obtenerValor(registro, "AREA");

            if (area == null || !area.matches("\\d{1,2}")) {
                throw new ArchivoInvalidoException(
                        "Se encontró un registro sin un código AREA válido."
                );
            }

            if (!AREA_PASTO.equals(area)) {
                continue;
            }

            registrosPasto++;

            boolean identificadorValido = true;

            if (tieneIdentificadores) {
                String directorio = obtenerValor(registro, "DIRECTORIO");
                String secuencia = obtenerValor(registro, "SECUENCIA_P");
                String orden = obtenerValor(registro, "ORDEN");

                if (directorio == null
                        || secuencia == null
                        || orden == null) {

                    identificadorValido = false;

                } else {
                    List<String> identificador = List.of(
                            directorio,
                            secuencia,
                            orden
                    );

                    if (!identificadores.add(identificador)) {
                        throw new ArchivoInvalidoException(
                                "Se encontraron registros duplicados "
                                        + "para el área de Pasto."
                        );
                    }
                }

            } else {
                identificadorValido = false;
            }

            Integer edad = leerEdad(obtenerValor(registro, "P6040"));

            BigDecimal factor = leerFactor(
                    obtenerValor(registro, "FEX_C")
            );

            boolean baseRegistroValida =
                    identificadorValido
                            && edad != null
                            && edad >= 5
                            && edad <= 17
                            && factor != null;

            if (baseRegistroValida) {
                registrosValidos++;
                sumaFactores = sumaFactores.add(factor);

            } else {
                registrosInvalidos++;
            }

            String p400 = obtenerValor(registro, "P400");
            String p401 = obtenerValor(registro, "P401");
            String p402 = obtenerValor(registro, "P402");
            String p403 = obtenerValor(registro, "P403");

            boolean respuestasValidas =
                    tienePreguntasTrabajo
                            && p400 != null
                            && ACTIVIDADES_VALIDAS.contains(p400)
                            && respuestaOpcionalValida(p401)
                            && respuestaOpcionalValida(p402)
                            && respuestaOpcionalValida(p403);

            if (baseRegistroValida && respuestasValidas) {
                registrosTrabajoValidos++;
                sumaFactoresTrabajo = sumaFactoresTrabajo.add(factor);
                if (tieneAfirmativa(p400, p401, p402, p403)) {
                    sumaAfirmativas = sumaAfirmativas.add(factor);
                }
            }
        }

        boolean poblacionDisponible =
                tieneBase && registrosValidos > 0 && sumaFactores.signum() > 0;

        boolean trabajoDisponible =
                poblacionDisponible && tienePreguntasTrabajo
                        && registrosTrabajoValidos > 0 && sumaFactoresTrabajo.signum() > 0;

        String motivoPoblacion = null;
        String motivoTrabajo = null;

        if (!poblacionDisponible) {
            motivoPoblacion =
                    "No es posible calcular la población expandida: "
                            + "faltan registros de Pasto, identificadores, "
                            + "edades o factores de expansión válidos.";
        } else if (registrosInvalidos > 0) {
            motivoPoblacion = "Calculado con registros válidos; se excluyeron registros con datos básicos incompletos.";
        }

        if (!trabajoDisponible) {
            if (!poblacionDisponible) {
                motivoTrabajo =
                        "No está disponible porque faltan datos válidos "
                                + "para la población de referencia.";

            } else {
                motivoTrabajo =
                        "No está disponible porque faltan columnas o "
                                + "respuestas necesarias de P400 a P403.";
            }
        } else if (registrosInvalidos > 0 || registrosTrabajoValidos < registrosValidos) {
            motivoTrabajo = "Calculado con las respuestas válidas; algunos registros o respuestas se excluyeron.";
        }

        BigDecimal poblacion = null;
        BigDecimal poblacionRespuesta = null;
        BigDecimal afirmativas = null;
        BigDecimal proporcion = null;

        if (poblacionDisponible) {
            poblacion = ajustarDecimales(sumaFactores);
        }

        if (trabajoDisponible) {
            afirmativas = ajustarDecimales(sumaAfirmativas);
            poblacionRespuesta = ajustarDecimales(sumaFactoresTrabajo);

            proporcion = sumaAfirmativas
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            sumaFactoresTrabajo,
                            6,
                            RoundingMode.HALF_UP
                    );
        }

        return new ResumenArchivo(
                registrosTotales,
                registrosPasto,
                registrosValidos,
                registrosInvalidos,
                registrosTrabajoValidos,
                poblacionDisponible,
                trabajoDisponible,
                poblacion,
                poblacionRespuesta,
                afirmativas,
                proporcion,
                motivoPoblacion,
                motivoTrabajo
        );
    }

    private char detectarSeparador(String encabezado) {
        int comas = 0;
        int puntosYComas = 0;

        boolean dentroDeComillas = false;

        for (int i = 0; i < encabezado.length(); i++) {
            char caracter = encabezado.charAt(i);

            if (caracter == '"') {
                if (dentroDeComillas
                        && i + 1 < encabezado.length()
                        && encabezado.charAt(i + 1) == '"') {
                    i++;
                } else {
                    dentroDeComillas = !dentroDeComillas;
                }

            } else if (!dentroDeComillas) {
                if (caracter == ',') {
                    comas++;
                } else if (caracter == ';') {
                    puntosYComas++;
                }
            }
        }

        if (dentroDeComillas
                || (comas == 0 && puntosYComas == 0)
                || comas == puntosYComas) {

            throw new ArchivoInvalidoException(
                    "No fue posible identificar si el CSV usa "
                            + "comas o puntos y comas."
            );
        }

        return puntosYComas > comas ? ';' : ',';
    }

    private String obtenerValor(CSVRecord registro, String columna) {
        if (!registro.isMapped(columna) || !registro.isSet(columna)) {
            return null;
        }

        String valor = registro.get(columna);

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private Integer leerEdad(String texto) {
        if (texto == null) {
            return null;
        }

        try {
            return Integer.valueOf(texto);

        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private BigDecimal leerFactor(String texto) {
        if (texto == null) {
            return null;
        }

        try {
            BigDecimal factor = new BigDecimal(texto);

            return factor.signum() > 0 ? factor : null;

        } catch (NumberFormatException excepcion) {
            return null;
        }
    }

    private boolean respuestaOpcionalValida(String valor) {
        return valor == null
                || valor.equals("1")
                || valor.equals("2");
    }

    private boolean tieneAfirmativa(
            String p400,
            String p401,
            String p402,
            String p403
    ) {
        return "1".equals(p400)
                || "1".equals(p401)
                || "1".equals(p402)
                || "1".equals(p403);
    }

    private BigDecimal ajustarDecimales(BigDecimal valor) {
        return valor.setScale(6, RoundingMode.HALF_UP);
    }

    private void validarSolicitud(
            MultipartFile archivo,
            Integer anio,
            Integer trimestre
    ) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException(
                    "Debes seleccionar un archivo CSV que no esté vacío."
            );
        }

        if (archivo.getSize() > MAXIMO_BYTES) {
            throw new ArchivoInvalidoException(
                    "El archivo supera el límite de 20 MB."
            );
        }

        if (anio == null || anio < 1900 || anio > 2100) {
            throw new ArchivoInvalidoException(
                    "El año debe estar entre 1900 y 2100."
            );
        }

        if (trimestre == null || trimestre < 1 || trimestre > 4) {
            throw new ArchivoInvalidoException(
                    "El trimestre debe estar entre 1 y 4."
            );
        }

        obtenerNombreSeguro(archivo);
    }

    private String obtenerNombreSeguro(MultipartFile archivo) {
        String nombreOriginal = archivo.getOriginalFilename();

        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new ArchivoInvalidoException(
                    "El archivo debe tener un nombre."
            );
        }

        String nombreNormalizado = nombreOriginal
                .replace('\\', '/')
                .trim();

        int ultimaBarra = nombreNormalizado.lastIndexOf('/');

        String nombre = nombreNormalizado
                .substring(ultimaBarra + 1)
                .trim();

        if (nombre.isBlank() || nombre.length() > 255) {
            throw new ArchivoInvalidoException(
                    "El nombre del archivo no es válido."
            );
        }

        if (!nombre.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ArchivoInvalidoException(
                    "El archivo debe tener extensión .csv."
            );
        }

        return nombre;
    }

    private String obtenerVersion(Integer anio, Integer trimestre) {
        return PoliticaVersiones.versionParaCarga(anio, trimestre);
    }

    private Periodo crearPeriodo(Integer anio, Integer trimestre) {
        Periodo periodo = new Periodo();

        periodo.setAnio(anio);
        periodo.setTrimestre(trimestre);
        periodo.setEstado(EstadoPeriodo.PENDIENTE);

        return periodoRepository.save(periodo);
    }

    private Territorio crearTerritorioPasto() {
        Territorio territorio = new Territorio();

        territorio.setCodigoFuente(AREA_PASTO);
        territorio.setNombre("Pasto");
        territorio.setTipo("AREA_GEIH");
        territorio.setActivo(true);

        return territorioRepository.save(territorio);
    }

    private Indicador obtenerIndicadorPoblacion(String version) {
        return indicadorRepository
                .findByCodigoAndVersionRegla(
                        "POBLACION_EXPANDIDA_5_17",
                        version
                )
                .orElseGet(() -> crearIndicador(
                        "POBLACION_EXPANDIDA_5_17",
                        "Población estimada de 5 a 17 años - exploratoria",
                        "Estimación obtenida mediante la suma de los "
                                + "factores de expansión para el área "
                                + "de Pasto. Pendiente de validación "
                                + "estadística.",
                        "PERSONAS",
                        "Sumar FEX_C de los registros válidos de "
                                + "5 a 17 años con AREA igual a 52.",
                        version
                ));
    }

    private Indicador obtenerIndicadorTrabajo(String version) {
        return indicadorRepository
                .findByCodigoAndVersionRegla(
                        "RESPUESTAS_AFIRMATIVAS_TRABAJO",
                        version
                )
                .orElseGet(() -> crearIndicador(
                        "RESPUESTAS_AFIRMATIVAS_TRABAJO",
                        "Respuestas afirmativas sobre trabajo - exploratoria",
                        "Proporción de registros expandidos con al menos "
                                + "una respuesta afirmativa en P400, P401, "
                                + "P402 o P403. No es la tasa oficial de "
                                + "trabajo infantil.",
                        "PORCENTAJE",
                        "Sumar FEX_C cuando al menos una de las variables "
                                + "P400 a P403 sea igual a 1; dividir por "
                                + "la suma de FEX_C de la población de "
                                + "referencia y multiplicar por 100.",
                        version
                ));
    }

    private Indicador crearIndicador(
            String codigo,
            String nombre,
            String descripcion,
            String unidad,
            String regla,
            String version
    ) {
        Indicador indicador = new Indicador();

        indicador.setCodigo(codigo);
        indicador.setNombre(nombre);
        indicador.setDescripcion(descripcion);
        indicador.setUnidad(unidad);
        indicador.setReglaCalculo(regla);
        indicador.setVersionRegla(version);

        /*
         * Solo se activará para el catálogo público
         * cuando un administrador publique la carga.
         */
        indicador.setActivo(false);

        return indicadorRepository.save(indicador);
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

    private record ResumenArchivo(
            int registrosTotales,
            int registrosPasto,
            int registrosValidos,
            int registrosInvalidos,
            int registrosTrabajoValidos,
            boolean poblacionDisponible,
            boolean trabajoDisponible,
            BigDecimal poblacionExpandida,
            BigDecimal poblacionConRespuestaValida,
            BigDecimal respuestasAfirmativas,
            BigDecimal proporcionAfirmativa,
            String motivoPoblacion,
            String motivoTrabajo
    ) {
    }
}



