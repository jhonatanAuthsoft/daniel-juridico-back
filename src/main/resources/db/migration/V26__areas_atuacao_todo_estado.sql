-- Cobertura de todo o estado: uma linha por UF, sem município.
ALTER TABLE areas_atuacao_advogado
  ADD COLUMN IF NOT EXISTS todo_estado BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE areas_atuacao_advogado
  ALTER COLUMN cidade DROP NOT NULL;

DROP INDEX IF EXISTS uk_areas_atuacao_advogado_cidade;

CREATE UNIQUE INDEX IF NOT EXISTS uk_areas_atuacao_advogado_cidade
  ON areas_atuacao_advogado(advogado_id, estado, cidade)
  WHERE cidade IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_areas_atuacao_advogado_todo_estado
  ON areas_atuacao_advogado(advogado_id, estado)
  WHERE todo_estado = TRUE;

ALTER TABLE areas_atuacao_advogado
  DROP CONSTRAINT IF EXISTS ck_areas_atuacao_todo_estado;

ALTER TABLE areas_atuacao_advogado
  ADD CONSTRAINT ck_areas_atuacao_todo_estado
  CHECK (
    (todo_estado = TRUE AND cidade IS NULL)
    OR (todo_estado = FALSE AND cidade IS NOT NULL)
  );
