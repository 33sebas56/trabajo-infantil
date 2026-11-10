package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.carga.CargaResponse;
import com.pasto.trabajo_infantil.dto.carga.ErrorCargaResponse;

import com.pasto.trabajo_infantil.entity.Administrador;
import com.pasto.trabajo_infantil.entity.CargaDatos;
import com.pasto.trabajo_infantil.entity.ErrorCarga;
import com.pasto.trabajo_infantil.entity.Periodo;

import com.pasto.trabajo_infantil.enums.EstadoCarga;
import com.pasto.trabajo_infantil.enums.EstadoPeriodo;

import com.pasto.trabajo_infantil.exception.ArchivoInvalidoException;
import com.pasto.trabajo_infantil.exception.RecursoNoEncontradoException;

import com.pasto.trabajo_infantil.repository.AdministradorRepository;
import com.pasto.trabajo_infantil.repository.CargaDatosRepository;
import com.pasto.trabajo_infantil.repository.ErrorCargaRepository;
import com.pasto.trabajo_infantil.repository.PeriodoRepository;

import com.pasto.trabajo_infantil.service.procesamiento.LectorCsvService;
import com.pasto.trabajo_infantil.service.procesamiento
        .LectorCsvService.RevisionCsv;
import com.pasto.trabajo_infantil.service.procesamiento
        .ValidadorCsvService.DetalleError;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class CargaDatosService {

    private static final long MAXIMO_BYTES = 20L * 1024 * 1024;

    private final AdministradorRepository administradorRepository;
    private final PeriodoRepository periodoRepository;
    private final CargaDatosRepository cargaDatosRepository;
    private final ErrorCargaRepository errorCargaRepository;
    private final LectorCsvService lectorCsvService;

    public CargaDatosService(
            AdministradorRepository administradorRepository,
            PeriodoRepository periodoRepository,
            CargaDatosRepository cargaDatosRepository,
            ErrorCargaRepository errorCargaRepository,
            LectorCsvService lectorCsvService
    ) {
        this.administradorRepository = administradorRepository;
        this.periodoRepository = periodoRepository;
        this.cargaDatosRepository = cargaDatosRepository;
        this.errorCargaRepository = errorCargaRepository;
        this.lectorCsvService = lectorCsvService;
    }

    @Transactional
    public CargaResponse revisarArchivo(
            MultipartFile archivo,
            Integer anio,
            Integer trimestre,
            String correoAdministrador
    ) {
        validarDatosIniciales(archivo, anio, trimestre);

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

        cargaDatosRepository.save(carga);

        try {
            RevisionCsv revision = lectorCsvService.revisar(archivo);

            carga.setRegistrosTotales(revision.registrosTotales());
            carga.setRegistrosPasto(revision.registrosPasto());
            carga.setRegistrosValidos(revision.registrosValidos());
            carga.setRegistrosInvalidos(revision.registrosInvalidos());

            if (revision.registrosPasto() == 0) {
                carga.setEstado(EstadoCarga.FALLIDA);

                carga.setMensajeResultado(
                        "No se encontraron registros para el área de Pasto."
                );

                guardarError(
                        carga,
                        null,
                        "AREA",
                        "El archivo no contiene registros con AREA = 52."
                );

            } else if (revision.registrosInvalidos() > 0) {
                carga.setEstado(EstadoCarga.FALLIDA);

                carga.setMensajeResultado(
                        "La revisión encontró registros inválidos. "
                                + "Consulta el detalle de errores."
                );

                for (DetalleError detalle : revision.detalles()) {
                    guardarError(
                            carga,
                            detalle.numeroFila(),
                            detalle.campo(),
                            detalle.detalle()
                    );
                }

            } else {
                carga.setEstado(EstadoCarga.VALIDADA);

                carga.setMensajeResultado(
                        "La revisión inicial terminó correctamente. "
                                + "El archivo aún no se ha procesado "
                                + "ni publicado."
                );
            }

        } catch (ArchivoInvalidoException excepcion) {
            carga.setEstado(EstadoCarga.FALLIDA);

            carga.setMensajeResultado(
                    "No fue posible completar la revisión del archivo."
            );

            guardarError(
                    carga,
                    null,
                    null,
                    excepcion.getMessage()
            );
        }

        carga.setFinalizadoEn(Instant.now());

        CargaDatos cargaGuardada = cargaDatosRepository.save(carga);

        return convertirCarga(cargaGuardada);
    }

    @Transactional(readOnly = true)
    public List<CargaResponse> listarCargas() {
        return cargaDatosRepository
                .findAllByOrderByIniciadoEnDesc()
                .stream()
                .map(this::convertirCarga)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ErrorCargaResponse> listarErrores(Long cargaId) {
        if (!cargaDatosRepository.existsById(cargaId)) {
            throw new RecursoNoEncontradoException(
                    "La carga solicitada no existe."
            );
        }

        return errorCargaRepository
                .findByCarga_IdOrderByIdAsc(cargaId)
                .stream()
                .map(error -> new ErrorCargaResponse(
                        error.getId(),
                        cargaId,
                        error.getNumeroFila(),
                        error.getCampo(),
                        error.getDetalle(),
                        error.getCreadoEn()
                ))
                .toList();
    }

    private void validarDatosIniciales(
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
                    "El archivo supera el tamaño permitido de 20 MB."
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
                    "El archivo seleccionado debe tener extensión .csv."
            );
        }

        return nombre;
    }

    private Periodo crearPeriodo(Integer anio, Integer trimestre) {
        Periodo periodo = new Periodo();
        periodo.setAnio(anio);
        periodo.setTrimestre(trimestre);
        periodo.setEstado(EstadoPeriodo.PENDIENTE);

        return periodoRepository.save(periodo);
    }

    private void guardarError(
            CargaDatos carga,
            Integer numeroFila,
            String campo,
            String detalle
    ) {
        ErrorCarga error = new ErrorCarga();
        error.setCarga(carga);
        error.setNumeroFila(numeroFila);
        error.setCampo(campo);
        error.setDetalle(detalle);

        errorCargaRepository.save(error);
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
}