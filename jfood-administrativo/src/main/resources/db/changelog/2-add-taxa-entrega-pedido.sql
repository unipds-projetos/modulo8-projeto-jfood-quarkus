--liquibase formatted sql

--changeset jfood:2-add-taxa-entrega-pedido labels:schema
--comment: convertido de V2__add_taxa_entrega_pedido.sql
-- =============================================================================
-- V2 — a taxa de entrega
--
-- Nasce NULLABLE, e nao por descuido: quando esta coluna e adicionada, a tabela
-- pedido ja tem linhas em producao. Um NOT NULL sem DEFAULT seria recusado na
-- hora ("column contains null values"), e um DEFAULT 0.00 seria uma afirmacao
-- sobre o passado -- dizer que todo pedido antigo teve frete gratis.
--
-- O caminho correto tem tres passos, e os outros dois estao nas V7 e V8.
-- =============================================================================

ALTER TABLE pedido
    ADD COLUMN taxa_entrega NUMERIC(10,2);

--rollback ALTER TABLE pedido DROP COLUMN taxa_entrega;
