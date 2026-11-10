package com.pasto.trabajo_infantil.service;

import com.pasto.trabajo_infantil.dto.carga.CargaResponse;
import com.pasto.trabajo_infantil.dto.resultado.ResultadoResponse;

import com.pasto.trabajo_infantil.entity.Administrador;
import com.pasto.trabajo_infantil.entity.CargaDatos;
import com.pasto.trabajo_infantil.entity.Indicador;
import com.pasto.trabajo_infantil.entity.Periodo;
import com.pasto.trabajo_infantil.entity.Resultado;
import com.pasto.trabajo_infantil.entity.Territorio;

import com.pasto.trabajo_infantil.enums.EstadoCarga;
import com.pasto.trabajo_infantil.enums.EstadoPeriodo;

import com.pasto.trabajo_infantil.exception.ArchivoInvalidoException;
import com.pasto.trabajo_infantil.exception.RecursoNoEncontradoException;

import com.pasto.trabajo_infantil.repository.AdministradorRepository;
import com.pasto.trabajo_infantil.repository.CargaDatosRepository;
import com.pasto.trabajo_infantil.repository.IndicadorRepository;
import com.pasto.trabajo_infantil.repository.PeriodoRepository;
import com.pasto.trabajo_infantil.repository.ResultadoRepository;
import com.pasto.trabajo_infantil.repository.TerritorioRepository;

import com.pasto.trabajo_infantil.service.procesamiento
        .CalculadorIndicadoresService;
import com.pasto.trabajo_infantil.service.procesamiento
        .CalculadorIndicadoresService.ResumenCalculo;
import com.pasto.trabajo_infantil.service.procesamiento
        .LectorCsvService;
import com.pasto.trabajo_infantil.service.procesamiento
        .LectorCsvService.RevisionCsv;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class ProcesamientoService {

    private static final String VERSION_REGLA = "EXP-2025";
    private static final long MAXIMO_BYTES = 20L * 1024 * 1024;

    private final AdministradorRepository administradorRepository;
    private final PeriodoRepository periodoRepository;
    private final CargaDatosRepository cargaDatosRepository;
    private final TerritorioRepository territorioRepository;
    private final IndicadorRepository indicadorRepository;
    private final ResultadoRepository resultadoRepository;

    private final LectorCsvService lectorCsvService;
    private final CalculadorIndicadoresService calculadorIndicadoresService;

    public ProcesamientoService(
            AdministradorRepository administradorRepository,
            PeriodoRepository periodoRepository,
            CargaDatosRepository cargaDatosRepository,
            TerritorioRepository territorioRepository,
            IndicadorRepository indicadorRepository,
            ResultadoRepository resultadoRepository,
            LectorCsvService lectorCsvService,
            CalculadorIndicadoresService calculadorIndicadoresService
    ) {
        this.administradorRepository = administradorRepository;
        this.periodoRepository = periodoRepository;
        this.cargaDatosRepository = cargaDatosRepository;
        this.territorioRepository = territorioRepository;
        this.indicadorRepository = indicadorRepository;
        this.resultadoRepository = resultadoRepository;
        this.lectorCsvService = lectorCsvService;
        this.calculadorIndicadoresService = calculadorIndicadoresService;
    }

    @Transactional
    public CargaResponse procesarArchivo(
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

        /*
         * Primera lectura: revisa estructura y valores.
         */
        RevisionCsv revision = lectorCsvService.revisar(archivo);

        if (revision.registrosPasto() == 0) {
            throw new ArchivoInvalidoException(
                    "No se encontraron registros de Pasto."
            );
        }

        if (revision.registrosInvalidos() > 0) {
            throw new ArchivoInvalidoException(
                    "El archivo contiene registros inválidos. "
                            + "Revisa primero los errores de la carga."
            );
        }

        /*
         * Segunda lectura: calcula resultados exploratorios.
         */
        ResumenCalculo calculo =
                calculadorIndicadoresService.calcular(archivo);

        if (calculo.cantidadMuestra() != revision.registrosValidos()) {
            throw new ArchivoInvalidoException(
                    "La cantidad de registros calculados no coincide "
                            + "con la cantidad validada."
            );
        }

        Periodo periodo = periodoRepository
                .findByAnioAndTrimestre(anio, trimestre)
                .orElseGet(() -> crearPeriodo(anio, trimestre));

        Territorio territorio = territorioRepository
                .findByCodigoFuente("52")
                .orElseGet(this::crearTerritorioPasto);

        Indicador poblacion = obtenerIndicadorPoblacion();
        Indicador respuestas = obtenerIndicadorRespuestas();

        CargaDatos carga = new CargaDatos();
        carga.setAdministrador(administrador);
        carga.setPeriodo(periodo);
        carga.setNombreArchivo(obtenerNombreSeguro(archivo));

        /*
         * Permanece VALIDADA y no vigente:
         * todavía no se publican resultados exploratorios.
         */
        carga.setEstado(EstadoCarga.VALIDADA);
        carga.setVigente(false);

        carga.setRegistrosTotales(revision.registrosTotales());
        carga.setRegistrosPasto(revision.registrosPasto());
        carga.setRegistrosValidos(revision.registrosValidos());
        carga.setRegistrosInvalidos(revision.registrosInvalidos());

        carga.setMensajeResultado(
                "Se guardaron resultados exploratorios para revisión. "
                        + "La carga no está publicada."
        );

        carga.setFinalizadoEn(Instant.now());

        CargaDatos cargaGuardada = cargaDatosRepository.saveAndFlush(carga);

        Resultado resultadoPoblacion = new Resultado();
        resultadoPoblacion.setCarga(cargaGuardada);
        resultadoPoblacion.setIndicador(poblacion);
        resultadoPoblacion.setTerritorio(territorio);
        resultadoPoblacion.setCriterioAgrupacion("GENERAL");
        resultadoPoblacion.setGrupo("TOTAL");
        resultadoPoblacion.setValor(calculo.poblacionExpandida());
        resultadoPoblacion.setCantidadMuestra(calculo.cantidadMuestra());

        Resultado resultadoRespuestas = new Resultado();
        resultadoRespuestas.setCarga(cargaGuardada);
        resultadoRespuestas.setIndicador(respuestas);
        resultadoRespuestas.setTerritorio(territorio);
        resultadoRespuestas.setCriterioAgrupacion("GENERAL");
        resultadoRespuestas.setGrupo("TOTAL");
        resultadoRespuestas.setValor(calculo.proporcionAfirmativa());

        resultadoRespuestas.setNumerador(
                calculo.respuestasAfirmativasExpandidas()
        );

        resultadoRespuestas.setDenominador(
                calculo.poblacionExpandida()
        );

        resultadoRespuestas.setCantidadMuestra(
                calculo.cantidadMuestra()
        );

        resultadoRepository.saveAll(
                List.of(resultadoPoblacion, resultadoRespuestas)
        );

        return convertirCarga(cargaGuardada);
    }

    @Transactional(readOnly = true)
    public List<ResultadoResponse> consultarResultadosPrivados(Long cargaId) {
        if (!cargaDatosRepository.existsById(cargaId)) {
            throw new RecursoNoEncontradoException(
                    "La carga solicitada no existe."
            );
        }

        return resultadoRepository
                .findByCarga_IdOrderByIdAsc(cargaId)
                .stream()
                .map(this::convertirResultado)
                .toList();
    }

    private void validarSolicitud(
            MultipartFile archivo,
            Integer anio,
            Integer trimestre
    ) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException(
                    "Debes seleccionar un archivo CSV."
            );
        }

        if (archivo.getSize() > MAXIMO_BYTES) {
            throw new ArchivoInvalidoException(
                    "El archivo supera el límite de 20 MB."
            );
        }

        /*
         * Por ahora solo tenemos revisado el archivo de 2025,
         * correspondiente al cuarto trimestre.
         */
        if (anio == null
                || trimestre == null
                || anio != 2025
                || trimestre != 4) {
            throw new ArchivoInvalidoException(
                    "Este cálculo exploratorio solo está preparado "
                            + "para el cuarto trimestre de 2025."
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

    private Periodo crearPeriodo(Integer anio, Integer trimestre) {
        Periodo periodo = new Periodo();
        periodo.setAnio(anio);
        periodo.setTrimestre(trimestre);
        periodo.setEstado(EstadoPeriodo.PENDIENTE);

        return periodoRepository.save(periodo);
    }

    private Territorio crearTerritorioPasto() {
        Territorio territorio = new Territorio();
        territorio.setCodigoFuente("52");
        territorio.setNombre("Pasto");
        territorio.setTipo("AREA_GEIH");
        territorio.setActivo(true);

        return territorioRepository.save(territorio);
    }

    private Indicador obtenerIndicadorPoblacion() {
        return indicadorRepository
                .findByCodigoAndVersionRegla(
                        "POBLACION_EXPANDIDA_5_17",
                        VERSION_REGLA
                )
                .orElseGet(() -> crearIndicador(
                        "POBLACION_EXPANDIDA_5_17",
                        "Población estimada de 5 a 17 años - exploratorio",
                        "Suma de los factores de expansión de los registros "
                                + "de 5 a 17 años correspondientes a Pasto. "
                                + "Resultado pendiente de validación estadística.",
                        "PERSONAS",
                        "Sumar FEX_C de los registros válidos "
                                + "con AREA igual a 52."
                ));
    }

    private Indicador obtenerIndicadorRespuestas() {
        return indicadorRepository
                .findByCodigoAndVersionRegla(
                        "RESPUESTAS_AFIRMATIVAS_TRABAJO",
                        VERSION_REGLA
                )
                .orElseGet(() -> crearIndicador(
                        "RESPUESTAS_AFIRMATIVAS_TRABAJO",
                        "Respuestas afirmativas sobre trabajo - exploratorio",
                        "Proporción expandida de registros con al menos una "
                                + "respuesta afirmativa en P400, P401, P402 "
                                + "o P403. No corresponde por sí sola a la "
                                + "tasa oficial de trabajo infantil.",
                        "PORCENTAJE",
                        "Sumar FEX_C cuando al menos una de las variables "
                                + "P400, P401, P402 o P403 sea igual a 1; "
                                + "dividir por la suma total de FEX_C y "
                                + "multiplicar por 100."
                ));
    }

    private Indicador crearIndicador(
            String codigo,
            String nombre,
            String descripcion,
            String unidad,
            String regla
    ) {
        Indicador indicador = new Indicador();

        indicador.setCodigo(codigo);
        indicador.setNombre(nombre);
        indicador.setDescripcion(descripcion);
        indicador.setUnidad(unidad);
        indicador.setReglaCalculo(regla);
        indicador.setVersionRegla(VERSION_REGLA);

        /*
         * No se incluyen en el catálogo público
         * mientras estén pendientes de revisión.
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

    private ResultadoResponse convertirResultado(Resultado resultado) {
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