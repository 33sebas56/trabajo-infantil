-- Agrega contexto sobre el número de respuestas que intervienen
-- en cada resultado. Los resultados y cargas anteriores se conservan.

ALTER TABLE resultado
    ADD COLUMN muestra_numerador INTEGER,
    ADD COLUMN muestra_denominador INTEGER;

ALTER TABLE resultado
    ADD CONSTRAINT ck_resultado_muestra_numerador
        CHECK (
            muestra_numerador IS NULL
            OR muestra_numerador >= 0
        ),

    ADD CONSTRAINT ck_resultado_muestra_denominador
        CHECK (
            muestra_denominador IS NULL
            OR muestra_denominador >= 0
        ),

    ADD CONSTRAINT ck_resultado_muestras_coherentes
        CHECK (
            muestra_numerador IS NULL
            OR muestra_denominador IS NULL
            OR muestra_numerador <= muestra_denominador
        );

-- Facilita consultar los resultados de una carga por grupo.

CREATE INDEX idx_resultado_consulta_grupo
    ON resultado (
        carga_id,
        criterio_agrupacion,
        grupo,
        indicador_id
    );