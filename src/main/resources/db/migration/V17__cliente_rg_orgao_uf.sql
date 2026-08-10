-- Órgão emissor e UF do RG no cadastro de cliente (PF)
ALTER TABLE clientes
    ADD COLUMN rg_orgao_emissor VARCHAR(20),
    ADD COLUMN rg_uf VARCHAR(2);
