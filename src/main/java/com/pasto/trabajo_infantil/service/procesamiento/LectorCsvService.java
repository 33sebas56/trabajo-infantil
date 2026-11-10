package com.pasto.trabajo_infantil.service.procesamiento;

import com.pasto.trabajo_infantil.exception.ArchivoInvalidoException;
import com.pasto.trabajo_infantil.service.procesamiento
        .ValidadorCsvService.DetalleError;

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

import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LectorCsvService {

    private static final String CODIGO_AREA_PASTO = "52";

    private static final int MAXIMO_DETALLES_ERROR = 25;

    private static final Set<String> COLUMNAS_REQUERIDAS = Set.of(
            "AREA",
            "P6040",
            "FEX_C",
            "P400"
    );

    private final ValidadorCsvService validadorCsvService;

    public LectorCsvService(ValidadorCsvService validadorCsvService) {
        this.validadorCsvService = validadorCsvService;
    }

    public RevisionCsv revisar(MultipartFile archivo) {
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
            // Algunos archivos UTF-8 empiezan con una marca invisible.
            int primerCaracter = lector.read();

            if (primerCaracter != -1 && primerCaracter != '\uFEFF') {
                lector.unread(primerCaracter);
            }

            try (CSVParser parser = formato.parse(lector)) {
                validarEncabezados(parser.getHeaderMap());

                int registrosTotales = 0;
                int registrosPasto = 0;
                int registrosValidos = 0;
                int registrosInvalidos = 0;

                List<DetalleError> detalles = new ArrayList<>();

                for (CSVRecord registro : parser) {
                    registrosTotales++;

                    int numeroFila = Math.toIntExact(
                            registro.getRecordNumber() + 1
                    );

                    if (!registro.isConsistent()) {
                        throw new ArchivoInvalidoException(
                                "El registro " + numeroFila
                                        + " no tiene la misma cantidad "
                                        + "de columnas que el encabezado."
                        );
                    }

                    String area = registro.get("AREA").trim();

                    if (!area.matches("\\d{1,2}")) {
                        throw new ArchivoInvalidoException(
                                "El registro " + numeroFila
                                        + " no tiene un código AREA válido."
                        );
                    }

                    if (!CODIGO_AREA_PASTO.equals(area)) {
                        continue;
                    }

                    registrosPasto++;

                    List<DetalleError> erroresRegistro =
                            validadorCsvService.validarRegistro(
                                    registro,
                                    numeroFila
                            );

                    if (erroresRegistro.isEmpty()) {
                        registrosValidos++;
                    } else {
                        registrosInvalidos++;

                        for (DetalleError error : erroresRegistro) {
                            if (detalles.size() >= MAXIMO_DETALLES_ERROR) {
                                break;
                            }

                            detalles.add(error);
                        }
                    }
                }

                return new RevisionCsv(
                        registrosTotales,
                        registrosPasto,
                        registrosValidos,
                        registrosInvalidos,
                        List.copyOf(detalles)
                );
            }

        } catch (IOException
                 | UncheckedIOException
                 | IllegalArgumentException excepcion) {

            throw new ArchivoInvalidoException(
                    "No fue posible leer el archivo. "
                            + "Comprueba que sea un CSV UTF-8 válido "
                            + "y que no tenga encabezados duplicados."
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
                    "Faltan columnas obligatorias: "
                            + String.join(", ", faltantes)
            );
        }
    }

    public record RevisionCsv(
            int registrosTotales,
            int registrosPasto,
            int registrosValidos,
            int registrosInvalidos,
            List<DetalleError> detalles
    ) {
    }
}