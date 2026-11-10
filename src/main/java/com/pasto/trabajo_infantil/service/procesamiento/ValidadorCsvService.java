package com.pasto.trabajo_infantil.service.procesamiento;

import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ValidadorCsvService {

    private static final Set<String> ACTIVIDADES_VALIDAS = Set.of(
            "1", "2", "3", "4", "5", "6"
    );

    public List<DetalleError> validarRegistro(
            CSVRecord registro,
            int numeroFila
    ) {
        List<DetalleError> errores = new ArrayList<>();

        validarEdad(registro, numeroFila, errores);
        validarFactorExpansion(registro, numeroFila, errores);
        validarActividadPrincipal(registro, numeroFila, errores);

        return errores;
    }

    private void validarEdad(
            CSVRecord registro,
            int numeroFila,
            List<DetalleError> errores
    ) {
        String edadTexto = registro.get("P6040").trim();

        try {
            int edad = Integer.parseInt(edadTexto);

            if (edad < 5 || edad > 17) {
                errores.add(new DetalleError(
                        numeroFila,
                        "P6040",
                        "La edad debe estar entre 5 y 17 años."
                ));
            }

        } catch (NumberFormatException excepcion) {
            errores.add(new DetalleError(
                    numeroFila,
                    "P6040",
                    "La edad no contiene un número entero válido."
            ));
        }
    }

    private void validarFactorExpansion(
            CSVRecord registro,
            int numeroFila,
            List<DetalleError> errores
    ) {
        String factorTexto = registro.get("FEX_C").trim();

        try {
            BigDecimal factor = new BigDecimal(factorTexto);

            if (factor.signum() <= 0) {
                errores.add(new DetalleError(
                        numeroFila,
                        "FEX_C",
                        "El factor de expansión debe ser mayor que cero."
                ));
            }

        } catch (NumberFormatException excepcion) {
            errores.add(new DetalleError(
                    numeroFila,
                    "FEX_C",
                    "El factor de expansión no es un número válido."
            ));
        }
    }

    private void validarActividadPrincipal(
            CSVRecord registro,
            int numeroFila,
            List<DetalleError> errores
    ) {
        String actividad = registro.get("P400").trim();

        if (!ACTIVIDADES_VALIDAS.contains(actividad)) {
            errores.add(new DetalleError(
                    numeroFila,
                    "P400",
                    "El código de actividad principal no está "
                            + "entre los valores esperados."
            ));
        }
    }

    public record DetalleError(
            int numeroFila,
            String campo,
            String detalle
    ) {
    }
}