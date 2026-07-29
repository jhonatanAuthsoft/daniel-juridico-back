-- OAB: N imagens (keys S3) em vez de frente/verso fixos
CREATE TABLE oab_fotos (
    oab_id UUID NOT NULL REFERENCES oabs (id) ON DELETE CASCADE,
    object_key VARCHAR(500) NOT NULL,
    ordem INT NOT NULL,
    PRIMARY KEY (oab_id, ordem)
);

INSERT INTO oab_fotos (oab_id, object_key, ordem)
SELECT id, foto_frente_url, 0
FROM oabs
WHERE foto_frente_url IS NOT NULL
  AND btrim(foto_frente_url) <> '';

INSERT INTO oab_fotos (oab_id, object_key, ordem)
SELECT
    id,
    foto_verso_url,
    CASE
        WHEN foto_frente_url IS NOT NULL AND btrim(foto_frente_url) <> '' THEN 1
        ELSE 0
    END
FROM oabs
WHERE foto_verso_url IS NOT NULL
  AND btrim(foto_verso_url) <> '';

ALTER TABLE oabs
    DROP COLUMN foto_frente_url,
    DROP COLUMN foto_verso_url;
