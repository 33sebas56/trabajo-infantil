-- Permite registrar una carga validada
-- aunque todavía no haya sido procesada ni publicada.

ALTER TABLE carga_datos
    DROP CONSTRAINT ck_carga_estado;

ALTER TABLE carga_datos
    ADD CONSTRAINT ck_carga_estado
    CHECK (
        estado IN (
            'RECIBIDA',
            'VALIDANDO',
            'VALIDADA',
            'PROCESANDO',
            'COMPLETADA',
            'FALLIDA'
        )
    );