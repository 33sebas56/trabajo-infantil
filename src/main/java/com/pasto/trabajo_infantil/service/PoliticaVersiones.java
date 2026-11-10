
package com.pasto.trabajo_infantil.service;

import java.util.Set;


public final class PoliticaVersiones {

    public static final String VERSION_COMUN = "EXP-COMUN-1";
    public static final String AVISO_CAMBIO_PREGUNTA =
            "La pregunta sobre actividad cambió de redacción en 2024. "
                    + "Esta comparación necesita revisión; puedes consultar los periodos por separado.";

    private static final Set<String> INDICADORES_SIN_PREGUNTA_LABORAL = Set.of(
            "POBLACION_EXPANDIDA_5_17",
            "SABE_LEER_ESCRIBIR",
            "ASISTE_INSTITUCION",
            "COCINA_PLATOS_MESA_HOGAR"
    );

    private PoliticaVersiones() {
    }

    public static String versionParaCarga(int anio, int trimestre) {
        if (trimestre == 4 && anio >= 2022 && anio <= 2025) {
            return VERSION_COMUN;
        }
        return versionDePeriodo(anio, trimestre);
    }

    public static boolean publicable(int anio, int trimestre, String version) {
        if (anio < 2022 || trimestre < 1 || trimestre > 4 || version == null) {
            return false;
        }
        return comparable(anio, trimestre, version)
                || versionDePeriodo(anio, trimestre).equals(version);
    }


    public static boolean comparable(int anio, int trimestre, String version) {
        if (trimestre != 4 || anio < 2022 || anio > 2025) {
            return false;
        }
        return VERSION_COMUN.equals(version)
                || (anio == 2022 && versionDePeriodo(2022, 4).equals(version));
    }


    public static boolean requiereRevisionEntreGrupos(
            String codigo, boolean tiene2022O2023, boolean tiene2024O2025
    ) {
        return codigo != null && !INDICADORES_SIN_PREGUNTA_LABORAL.contains(codigo)
                && tiene2022O2023 && tiene2024O2025;
    }

    private static String versionDePeriodo(int anio, int trimestre) {
        return "REV-" + anio + "-T" + trimestre;
    }
}



