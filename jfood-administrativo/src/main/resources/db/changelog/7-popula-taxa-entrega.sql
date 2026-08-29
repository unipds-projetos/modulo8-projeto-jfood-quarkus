--liquibase formatted sql

--changeset jfood:7-popula-taxa-entrega labels:seed
--comment: convertido de V7__popula_taxa_entrega_pedidos_antigos.sql
-- =============================================================================
-- V7 — passo 2 de 3 da obrigatoriedade da taxa de entrega
--
-- Passo 1 (V2): a coluna nasceu nullable.
-- Passo 2 (aqui): as linhas antigas ganham valor.
-- Passo 3 (V8): a coluna vira NOT NULL.
--
-- Por que tres migracoes e nao uma: entre a V2 e a V8 a aplicacao continuou
-- rodando em producao. Se o NOT NULL viesse junto com o UPDATE, a janela entre
-- os dois comandos -- ainda que de milissegundos -- seria suficiente para um
-- INSERT sem taxa entrar e derrubar a migracao inteira.
--
-- O 0.00 e uma decisao de negocio assinada: para o pedido importado do sistema
-- legado, o JFood assume que nao houve cobranca de frete.
-- =============================================================================

UPDATE pedido
   SET taxa_entrega = 0.00
 WHERE taxa_entrega IS NULL;

-- TAMBEM NAO E REVERSIVEL, e por um motivo diferente do anterior.
--
-- O UPDATE preencheu com 0.00 as linhas que estavam NULAS. Depois dele, nao ha
-- como distinguir um pedido que era nulo de um que ja tinha 0.00 -- a
-- informacao de quais linhas foram afetadas foi embora com o proprio UPDATE.
--
-- Escrever "UPDATE pedido SET taxa_entrega = NULL WHERE taxa_entrega = 0" seria
-- pior do que nao ter rollback: apagaria taxas legitimamente zeradas.
--rollback empty
