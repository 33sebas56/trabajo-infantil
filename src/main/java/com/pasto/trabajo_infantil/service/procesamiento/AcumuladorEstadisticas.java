package com.pasto.trabajo_infantil.service.procesamiento;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acumula resultados por edad y sexo sin conservar
 * registros individuales.
 *
 * La carga debe comprobar:
 * - Área.
 * - Identificadores.
 * - Edad.
 * - Factor de expansión.
 *
 * Esta clase no guarda ni publica resultados por sí sola.
 */
public final class AcumuladorEstadisticas {

    private static final BigDecimal CIEN =
            BigDecimal.valueOf(100);

    private final Map<Grupo, AcumuladoGrupo> grupos =
            new LinkedHashMap<>();

    /**
     * Los campos opcionales pueden ser null.
     *
     * Una actividad null significa que no fue posible
     * determinarla. No se interpreta como respuesta "no".
     */
    public record Fila(
            int edad,
            String sexo,
            BigDecimal factor,
            Boolean actividad,
            String lectura,
            String asistencia,
            String tareasCocina,
            String salarioMensual,
            String gananciaMensual,
            String motivoTrabajo
    ) {
    }

    /**
     * Numerador y denominador contienen sumas ponderadas.
     *
     * Las muestras son cantidades de registros sin expandir.
     *
     * El valor puede ser null cuando no existe información
     * suficiente para calcularlo.
     */
    public record Detalle(
            String codigoIndicador,
            String criterio,
            String grupo,
            BigDecimal valor,
            BigDecimal numerador,
            BigDecimal denominador,
            int muestraGrupo,
            int muestraNumerador,
            int muestraDenominador,
            String motivoNoDisponible
    ) {
    }

    private record Grupo(
            String criterio,
            String nombre
    ) {
    }

    public void agregar(Fila fila) {
        if (
                fila == null
                        || fila.edad() < 5
                        || fila.edad() > 17
                        || fila.factor() == null
                        || fila.factor().signum() <= 0
        ) {
            throw new IllegalArgumentException(
                    "La fila debe tener edad entre 5 y 17 años "
                            + "y un factor de expansión positivo."
            );
        }

        String edad = fila.edad() <= 14
                ? "5_14"
                : "15_17";

        String sexo = normalizarSexo(fila.sexo());

        acumular(
                new Grupo("GENERAL", "TOTAL"),
                fila
        );

        acumular(
                new Grupo("EDAD", edad),
                fila
        );

        acumular(
                new Grupo("SEXO", sexo),
                fila
        );

        acumular(
                new Grupo(
                        "EDAD_SEXO",
                        edad + "_" + sexo
                ),
                fila
        );
    }

    private void acumular(
            Grupo grupo,
            Fila fila
    ) {
        grupos.computeIfAbsent(
                        grupo,
                        ignorado -> new AcumuladoGrupo()
                )
                .agregar(fila);
    }

    /**
     * Devuelve los resultados calculados.
     *
     * No crea resultados para grupos que no existen
     * en el archivo.
     */
    public List<Detalle> resultados() {
        List<Detalle> salida = new ArrayList<>();

        grupos.forEach((grupo, acumulado) -> {

            salida.add(
                    acumulado.poblacion(grupo)
            );

            salida.add(
                    acumulado.actividad.porcentaje(
                            "RESPUESTAS_AFIRMATIVAS_TRABAJO",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            salida.add(
                    acumulado.actividad.totalAfirmativo(
                            "POBLACION_CON_RESPUESTAS_TRABAJO",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            salida.add(
                    acumulado.lectura.porcentaje(
                            "SABE_LEER_ESCRIBIR",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            salida.add(
                    acumulado.asistencia.porcentaje(
                            "ASISTE_INSTITUCION",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            salida.add(
                    acumulado.lecturaActividad.porcentaje(
                            "LECTURA_CON_ACTIVIDAD",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            salida.add(
                    acumulado.asistenciaActividad.porcentaje(
                            "ASISTENCIA_CON_ACTIVIDAD",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            salida.add(
                    acumulado.tareasCocina.porcentaje(
                            "COCINA_PLATOS_MESA_HOGAR",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            salida.add(
                    acumulado.salario.promedio(
                            "SALARIO_MENSUAL_P409",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            salida.add(
                    acumulado.ganancia.promedio(
                            "GANANCIA_MENSUAL_P413",
                            grupo,
                            acumulado.muestraGrupo
                    )
            );

            for (
                    int i = 0;
                    i < acumulado.motivos.length;
                    i++
            ) {
                salida.add(
                        acumulado.motivos[i].porcentaje(
                                "MOTIVO_TRABAJO_P405_" + (i + 1),
                                grupo,
                                acumulado.muestraGrupo
                        )
                );
            }
        });

        return List.copyOf(salida);
    }

    private static final class AcumuladoGrupo {

        private int muestraGrupo;

        private BigDecimal sumaFactores =
                BigDecimal.ZERO;

        private final Binario actividad =
                new Binario();

        private final Binario lectura =
                new Binario();

        private final Binario asistencia =
                new Binario();

        private final Binario lecturaActividad =
                new Binario();

        private final Binario asistenciaActividad =
                new Binario();

        private final Binario tareasCocina =
                new Binario();

        private final Promedio salario =
                new Promedio();

        private final Promedio ganancia =
                new Promedio();

        private final Binario[] motivos = {
                new Binario(),
                new Binario(),
                new Binario(),
                new Binario(),
                new Binario(),
                new Binario(),
                new Binario()
        };

        void agregar(Fila fila) {
            muestraGrupo++;

            BigDecimal factor = fila.factor();

            sumaFactores = sumaFactores.add(factor);

            actividad.agregar(
                    fila.actividad(),
                    factor
            );

            lectura.agregar(
                    respuestaSiNo(fila.lectura()),
                    factor
            );

            asistencia.agregar(
                    respuestaSiNo(fila.asistencia()),
                    factor
            );

            tareasCocina.agregar(
                    respuestaSiNo(fila.tareasCocina()),
                    factor
            );

            if (Boolean.TRUE.equals(fila.actividad())) {
                lecturaActividad.agregar(
                        respuestaSiNo(fila.lectura()),
                        factor
                );

                asistenciaActividad.agregar(
                        respuestaSiNo(fila.asistencia()),
                        factor
                );
            }

            /*
             * Los montos y motivos pueden existir aunque
             * falte alguna columna de actividad.
             *
             * No se incluyen si existe una contradicción
             * explícita con actividad = false.
             */
            if (!Boolean.FALSE.equals(fila.actividad())) {

                salario.agregar(
                        monto(fila.salarioMensual()),
                        factor
                );

                ganancia.agregar(
                        monto(fila.gananciaMensual()),
                        factor
                );

                String motivo =
                        limpiar(fila.motivoTrabajo());

                if (
                        motivo != null
                                && motivo.matches("[1-7]")
                ) {
                    int seleccionado =
                            Integer.parseInt(motivo) - 1;

                    for (
                            int i = 0;
                            i < motivos.length;
                            i++
                    ) {
                        motivos[i].agregar(
                                i == seleccionado,
                                factor
                        );
                    }
                }
            }
        }

        Detalle poblacion(Grupo grupo) {
            BigDecimal valor =
                    redondear(sumaFactores);

            if (!cabeEnBase(valor)) {
                return fueraDeRango(
                        "POBLACION_EXPANDIDA_5_17",
                        grupo,
                        muestraGrupo,
                        muestraGrupo,
                        muestraGrupo
                );
            }

            return new Detalle(
                    "POBLACION_EXPANDIDA_5_17",
                    grupo.criterio(),
                    grupo.nombre(),
                    valor,
                    valor,
                    null,
                    muestraGrupo,
                    muestraGrupo,
                    muestraGrupo,
                    null
            );
        }
    }

    private static final class Binario {

        private BigDecimal pesoValido =
                BigDecimal.ZERO;

        private BigDecimal pesoSi =
                BigDecimal.ZERO;

        private int registrosValidos;
        private int registrosSi;

        void agregar(
                Boolean respuesta,
                BigDecimal factor
        ) {
            if (respuesta == null) {
                return;
            }

            registrosValidos++;

            pesoValido = pesoValido.add(factor);

            if (respuesta) {
                registrosSi++;

                pesoSi = pesoSi.add(factor);
            }
        }

        Detalle porcentaje(
                String codigo,
                Grupo grupo,
                int muestraGrupo
        ) {
            if (
                    registrosValidos == 0
                            || pesoValido.signum() == 0
            ) {
                return sinDato(
                        codigo,
                        grupo,
                        muestraGrupo
                );
            }

            BigDecimal valor = pesoSi
                    .multiply(CIEN)
                    .divide(
                            pesoValido,
                            6,
                            RoundingMode.HALF_UP
                    );

            if (
                    !cabeEnBase(valor)
                            || !cabeEnBase(pesoSi)
                            || !cabeEnBase(pesoValido)
            ) {
                return fueraDeRango(
                        codigo,
                        grupo,
                        muestraGrupo,
                        registrosSi,
                        registrosValidos
                );
            }

            return new Detalle(
                    codigo,
                    grupo.criterio(),
                    grupo.nombre(),
                    valor,
                    redondear(pesoSi),
                    redondear(pesoValido),
                    muestraGrupo,
                    registrosSi,
                    registrosValidos,
                    null
            );
        }

        Detalle totalAfirmativo(
                String codigo,
                Grupo grupo,
                int muestraGrupo
        ) {
            if (registrosValidos == 0) {
                return sinDato(
                        codigo,
                        grupo,
                        muestraGrupo
                );
            }

            BigDecimal valor = redondear(pesoSi);

            if (
                    !cabeEnBase(valor)
                            || !cabeEnBase(pesoValido)
            ) {
                return fueraDeRango(
                        codigo,
                        grupo,
                        muestraGrupo,
                        registrosSi,
                        registrosValidos
                );
            }

            return new Detalle(
                    codigo,
                    grupo.criterio(),
                    grupo.nombre(),
                    valor,
                    valor,
                    redondear(pesoValido),
                    muestraGrupo,
                    registrosSi,
                    registrosValidos,
                    null
            );
        }
    }

    private static final class Promedio {

        private BigDecimal sumaPonderada =
                BigDecimal.ZERO;

        private BigDecimal sumaFactores =
                BigDecimal.ZERO;

        private int registros;

        void agregar(
                BigDecimal valor,
                BigDecimal factor
        ) {
            if (valor == null) {
                return;
            }

            registros++;

            sumaPonderada = sumaPonderada.add(
                    valor.multiply(factor)
            );

            sumaFactores = sumaFactores.add(factor);
        }

        Detalle promedio(
                String codigo,
                Grupo grupo,
                int muestraGrupo
        ) {
            if (
                    registros == 0
                            || sumaFactores.signum() == 0
            ) {
                return sinDato(
                        codigo,
                        grupo,
                        muestraGrupo
                );
            }

            /*
             * Si existe una sola respuesta, el promedio
             * sería el monto de una persona.
             */
            if (registros == 1) {
                return new Detalle(
                        codigo,
                        grupo.criterio(),
                        grupo.nombre(),
                        null,
                        null,
                        null,
                        muestraGrupo,
                        registros,
                        registros,
                        "Hay una sola respuesta de ingreso; "
                                + "no se publica un monto individual."
                );
            }

            BigDecimal valor = sumaPonderada.divide(
                    sumaFactores,
                    6,
                    RoundingMode.HALF_UP
            );

            if (
                    !cabeEnBase(valor)
                            || !cabeEnBase(sumaPonderada)
                            || !cabeEnBase(sumaFactores)
            ) {
                return fueraDeRango(
                        codigo,
                        grupo,
                        muestraGrupo,
                        registros,
                        registros
                );
            }

            return new Detalle(
                    codigo,
                    grupo.criterio(),
                    grupo.nombre(),
                    valor,
                    redondear(sumaPonderada),
                    redondear(sumaFactores),
                    muestraGrupo,
                    registros,
                    registros,
                    null
            );
        }
    }

    private static Detalle sinDato(
            String codigo,
            Grupo grupo,
            int muestraGrupo
    ) {
        return new Detalle(
                codigo,
                grupo.criterio(),
                grupo.nombre(),
                null,
                null,
                null,
                muestraGrupo,
                0,
                0,
                "No hay respuestas válidas disponibles."
        );
    }

    private static Detalle fueraDeRango(
            String codigo,
            Grupo grupo,
            int muestraGrupo,
            int muestraNumerador,
            int muestraDenominador
    ) {
        return new Detalle(
                codigo,
                grupo.criterio(),
                grupo.nombre(),
                null,
                null,
                null,
                muestraGrupo,
                muestraNumerador,
                muestraDenominador,
                "El resultado supera el rango admitido "
                        + "para su almacenamiento."
        );
    }

    private static String normalizarSexo(String valor) {
        String limpio = limpiar(valor);

        if ("1".equals(limpio)) {
            return "MASCULINO";
        }

        if ("2".equals(limpio)) {
            return "FEMENINO";
        }

        return "SIN_DATO";
    }

    private static Boolean respuestaSiNo(String valor) {
        String limpio = limpiar(valor);

        if ("1".equals(limpio)) {
            return Boolean.TRUE;
        }

        if ("2".equals(limpio)) {
            return Boolean.FALSE;
        }

        return null;
    }

    private static BigDecimal monto(String texto) {
        String limpio = limpiar(texto);

        if (
                limpio == null
                        || !limpio.matches("[0-9]{1,15}")
        ) {
            return null;
        }

        BigDecimal monto = new BigDecimal(limpio);

        /*
         * Se apartan los valores 98 y 99 hasta confirmar
         * su interpretación para cada versión del archivo.
         */
        if (
                monto.compareTo(BigDecimal.valueOf(98)) == 0
                        || monto.compareTo(BigDecimal.valueOf(99)) == 0
        ) {
            return null;
        }

        return monto;
    }

    private static String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(
                6,
                RoundingMode.HALF_UP
        );
    }

    private static boolean cabeEnBase(BigDecimal valor) {
        return redondear(valor).precision() <= 24;
    }
}