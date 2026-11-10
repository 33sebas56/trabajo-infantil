package com.pasto.trabajo_infantil.service.procesamiento;

import com.pasto.trabajo_infantil.exception.ArchivoInvalidoException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.UncheckedIOException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CalculadorIndicadoresService {

    private static final String CODIGO_AREA_PASTO = "52";

    private static final Set<String> COLUMNAS_REQUERIDAS = Set.of(
            "DIRECTORIO",
            "SECUENCIA_P",
            "ORDEN",
            "AREA",
            "P6040",
            "FEX_C",
            "P400",
            "P401",
            "P402",
            "P403"
    );

    public ResumenCalculo calcular(MultipartFile archivo) {
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();

        try (
                InputStreamReader lectorBase = new InputStreamReader(
                        archivo.getInputStream(),
                        StandardCharsets.UTF_8.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPORT)
                                .onUnmappableCharacter(CodingErrorAction.REPORT)
                );

                PushbackReader lector = new PushbackReader(lectorBase, 1)
        ) {
            int primerCaracter = lector.read();

            if (primerCaracter != -1 && primerCaracter != '\uFEFF') {
                lector.unread(primerCaracter);
            }

            try (CSVParser parser = formato.parse(lector)) {
                validarEncabezados(parser.getHeaderMap());

                BigDecimal poblacionExpandida = BigDecimal.ZERO;
                BigDecimal respuestasAfirmativasExpandidas = BigDecimal.ZERO;

                int cantidadMuestra = 0;

                Set<List<String>> identificadores = new HashSet<>();

                for (CSVRecord registro : parser) {
                    if (!registro.isConsistent()) {
                        throw new ArchivoInvalidoException(
                                "El archivo contiene un registro con "
                                        + "una cantidad incorrecta de columnas."
                        );
                    }

                    String area = registro.get("AREA").trim();

                    if (!CODIGO_AREA_PASTO.equals(area)) {
                        continue;
                    }

                    validarIdentificador(registro, identificadores);

                    int edad = leerEdad(registro);
                    BigDecimal factor = leerFactorExpansion(registro);

                    if (edad < 5 || edad > 17) {
                        throw new ArchivoInvalidoException(
                                "Se encontró una edad fuera del rango "
                                        + "de 5 a 17 años."
                        );
                    }

                    String p400 = registro.get("P400").trim();
                    String p401 = registro.get("P401").trim();
                    String p402 = registro.get("P402").trim();
                    String p403 = registro.get("P403").trim();

                    validarActividadPrincipal(p400);
                    validarRespuestaOpcional(p401, "P401");
                    validarRespuestaOpcional(p402, "P402");
                    validarRespuestaOpcional(p403, "P403");

                    cantidadMuestra++;
                    poblacionExpandida = poblacionExpandida.add(factor);

                    if (tieneRespuestaAfirmativa(p400, p401, p402, p403)) {
                        respuestasAfirmativasExpandidas =
                                respuestasAfirmativasExpandidas.add(factor);
                    }
                }

                if (cantidadMuestra == 0) {
                    throw new ArchivoInvalidoException(
                            "No se encontraron registros de Pasto "
                                    + "para realizar los cálculos."
                    );
                }

                if (poblacionExpandida.signum() <= 0) {
                    throw new ArchivoInvalidoException(
                            "El total del factor de expansión no es válido."
                    );
                }

                BigDecimal proporcionAfirmativa =
                        respuestasAfirmativasExpandidas
                                .multiply(BigDecimal.valueOf(100))
                                .divide(
                                        poblacionExpandida,
                                        6,
                                        RoundingMode.HALF_UP
                                );

                return new ResumenCalculo(
                        cantidadMuestra,
                        ajustarDecimales(poblacionExpandida),
                        ajustarDecimales(respuestasAfirmativasExpandidas),
                        proporcionAfirmativa
                );
            }

        } catch (IOException
                 | UncheckedIOException
                 | IllegalArgumentException excepcion) {

            throw new ArchivoInvalidoException(
                    "No fue posible calcular los resultados. "
                            + "Revisa que el archivo sea un CSV UTF-8 válido."
            );
        }
    }

    private void validarEncabezados(Map<String, Integer> encabezados) {
        List<String> faltantes = COLUMNAS_REQUERIDAS.stream()
                .filter(columna -> !encabezados.containsKey(columna))
                .sorted()
                .toList();

        if (!faltantes.isEmpty()) {
            throw new ArchivoInvalidoException(
                    "Faltan columnas para el cálculo: "
                            + String.join(", ", faltantes)
            );
        }
    }

    private void validarIdentificador(
            CSVRecord registro,
            Set<List<String>> identificadores
    ) {
        String directorio = registro.get("DIRECTORIO").trim();
        String secuencia = registro.get("SECUENCIA_P").trim();
        String orden = registro.get("ORDEN").trim();

        if (directorio.isBlank()
                || secuencia.isBlank()
                || orden.isBlank()) {
            throw new ArchivoInvalidoException(
                    "Se encontró un registro de Pasto "
                            + "con identificadores incompletos."
            );
        }

        List<String> identificador = List.of(
                directorio,
                secuencia,
                orden
        );

        if (!identificadores.add(identificador)) {
            throw new ArchivoInvalidoException(
                    "Se encontraron registros duplicados de Pasto."
            );
        }
    }

    private int leerEdad(CSVRecord registro) {
        try {
            return Integer.parseInt(
                    registro.get("P6040").trim()
            );

        } catch (NumberFormatException excepcion) {
            throw new ArchivoInvalidoException(
                    "Se encontró una edad que no es válida."
            );
        }
    }

    private BigDecimal leerFactorExpansion(CSVRecord registro) {
        try {
            BigDecimal factor = new BigDecimal(
                    registro.get("FEX_C").trim()
            );

            if (factor.signum() <= 0) {
                throw new ArchivoInvalidoException(
                        "Se encontró un factor de expansión no válido."
                );
            }

            return factor;

        } catch (NumberFormatException excepcion) {
            throw new ArchivoInvalidoException(
                    "Se encontró un factor de expansión que no es numérico."
            );
        }
    }

    private void validarActividadPrincipal(String valor) {
        if (!Set.of("1", "2", "3", "4", "5", "6").contains(valor)) {
            throw new ArchivoInvalidoException(
                    "Se encontró un valor no válido en P400."
            );
        }
    }

    private void validarRespuestaOpcional(
            String valor,
            String columna
    ) {
        if (!valor.isEmpty()
                && !valor.equals("1")
                && !valor.equals("2")) {
            throw new ArchivoInvalidoException(
                    "Se encontró un valor no válido en " + columna + "."
            );
        }
    }

    private boolean tieneRespuestaAfirmativa(
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

    public record ResumenCalculo(
            int cantidadMuestra,
            BigDecimal poblacionExpandida,
            BigDecimal respuestasAfirmativasExpandidas,
            BigDecimal proporcionAfirmativa
    ) {
    }
}