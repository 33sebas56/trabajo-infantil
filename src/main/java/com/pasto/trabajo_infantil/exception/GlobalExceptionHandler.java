package com.pasto.trabajo_infantil.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException excepcion
    ) {
        return crearRespuesta(
                HttpStatus.NOT_FOUND,
                excepcion.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<RespuestaError> manejarArchivoInvalido(
            ArchivoInvalidoException excepcion
    ) {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                excepcion.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(
            MethodArgumentNotValidException excepcion
    ) {
        Map<String, String> detalles = new LinkedHashMap<>();

        excepcion.getBindingResult()
                .getFieldErrors()
                .forEach(error -> {
                    String mensaje = error.getDefaultMessage();

                    if (mensaje == null) {
                        mensaje = "El valor ingresado no es válido.";
                    }

                    detalles.putIfAbsent(error.getField(), mensaje);
                });

        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "Revisa los datos enviados.",
                detalles
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> manejarContenidoInvalido() {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "No se pudo leer la información enviada.",
                Map.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> manejarConflictoDeDatos() {
        log.warn("Una operación fue rechazada por una restricción de datos.");

        return crearRespuesta(
                HttpStatus.CONFLICT,
                "No fue posible guardar la información. "
                        + "Comprueba que los datos no estén repetidos "
                        + "y que sean válidos.",
                Map.of()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespuestaError> manejarArchivoMuyGrande() {
        return crearRespuesta(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamaño permitido.",
                Map.of()
        );
    }

    private ResponseEntity<RespuestaError> crearRespuesta(
            HttpStatus estado,
            String mensaje,
            Map<String, String> detalles
    ) {
        RespuestaError respuesta = new RespuestaError(
                Instant.now(),
                estado.value(),
                mensaje,
                detalles
        );

        return ResponseEntity.status(estado).body(respuesta);
    }

    public record RespuestaError(
            Instant fecha,
            int estado,
            String mensaje,
            Map<String, String> detalles
    ) {
    }
}