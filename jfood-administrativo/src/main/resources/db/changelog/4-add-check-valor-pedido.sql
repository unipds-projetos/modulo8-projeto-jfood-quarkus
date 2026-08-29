--liquibase formatted sql

--changeset jfood:4-add-check-valor-pedido labels:schema
--comment: convertido de V4__add_check_valor_pedido.sql
-- =============================================================================
-- V4 — dinheiro nao pode ser negativo
--
-- A regra ja existia na cabeca do time; agora existe no banco. Um CHECK e a
-- forma mais barata de garantir invariante: custa uma comparacao por escrita e
-- vale para TODO caminho de escrita, inclusive o DBeaver as duas da manha.
-- =============================================================================

ALTER TABLE pedido
    ADD CONSTRAINT ck_pedido_valor_total_nao_negativo CHECK (valor_total >= 0);

ALTER TABLE pedido
    ADD CONSTRAINT ck_pedido_taxa_entrega_nao_negativa CHECK (taxa_entrega >= 0);

--rollback ALTER TABLE pedido DROP CONSTRAINT ck_pedido_valor_total_nao_negativo;
--rollback ALTER TABLE pedido DROP CONSTRAINT ck_pedido_taxa_entrega_nao_negativa;
