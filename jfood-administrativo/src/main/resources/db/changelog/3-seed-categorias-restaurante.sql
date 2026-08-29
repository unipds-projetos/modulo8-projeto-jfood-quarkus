--liquibase formatted sql

--changeset jfood:3-seed-categorias-restaurante labels:seed
--comment: convertido de V3__seed_categorias_restaurante.sql
-- =============================================================================
-- V3 — as categorias de cozinha
--
-- Dado de dominio, e nao dado de teste: sem estas cinco linhas a aplicacao nao
-- consegue cadastrar restaurante nenhum. Por isso vive numa migracao, e nao num
-- script de seed que alguem roda a mao.
-- =============================================================================

INSERT INTO categoria_restaurante (nome) VALUES
    ('Italiana'),
    ('Japonesa'),
    ('Brasileira'),
    ('Árabe'),
    ('Vegana');

--rollback DELETE FROM categoria_restaurante
--rollback  WHERE nome IN ('Italiana','Japonesa','Brasileira','Árabe','Vegana');
