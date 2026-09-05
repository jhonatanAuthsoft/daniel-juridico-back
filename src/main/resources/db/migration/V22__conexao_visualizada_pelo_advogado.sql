-- Marca a primeira abertura da solicitação pelo advogado. Alimenta a faixa
-- lateral de "nunca aberta" no card da home do advogado.
ALTER TABLE conexoes
    ADD COLUMN visualizada_em TIMESTAMP;
