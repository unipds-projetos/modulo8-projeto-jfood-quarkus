--liquibase formatted sql

--changeset jfood:8-taxa-entrega-not-null labels:schema
--comment: convertido de V8__taxa_entrega_not_null.sql
-- =============================================================================
-- V8 — passo 3 de 3: a taxa de entrega vira obrigatoria
--
-- Agora o SET NOT NULL passa, porque a V7 nao deixou nenhuma linha nula para
-- tras. Em uma tabela grande este comando faz uma varredura completa segurando
-- um lock de tabela -- e vale rodar em janela de baixo trafego.
-- =============================================================================

ALTER TABLE pedido
    ALTER COLUMN taxa_entrega SET NOT NULL;

--rollback ALTER TABLE pedido ALTER COLUMN taxa_entrega DROP NOT NULL;
