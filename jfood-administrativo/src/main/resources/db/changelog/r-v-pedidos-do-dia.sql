--liquibase formatted sql

-- O equivalente da migracao REPETIVEL do Flyway.
--
-- No Flyway, o prefixo R__ marca o arquivo como repetivel por CONVENCAO DE NOME.
-- No Liquibase, e um atributo do changeset: runOnChange:true. O Liquibase
-- recalcula o checksum a cada subida e reaplica quando ele muda.
--
-- A diferenca pratica esta em onde o comportamento fica visivel: no Flyway, no
-- nome do arquivo; no Liquibase, na declaracao do changeset -- e portanto no
-- diff do code review.
--
-- runOnChange NAO dispensa o CREATE OR REPLACE: a segunda execucao continua
-- rodando o mesmo SQL, e um CREATE VIEW puro falharia.

--changeset jfood:r-v-pedidos-do-dia labels:schema runOnChange:true
--comment: view do painel operacional, reaplicada sempre que o arquivo muda

CREATE OR REPLACE VIEW v_pedidos_do_dia AS
SELECT p.id                AS pedido_id,
       p.status,
       p.data_pedido,
       p.valor_total,
       p.taxa_entrega,
       uc.nome             AS cliente,
       r.nome              AS restaurante,
       ue.nome             AS entregador,
       pg.metodo           AS metodo_pagamento
  FROM pedido p
 INNER JOIN cliente c      ON c.usuario_id = p.cliente_id
 INNER JOIN usuario uc     ON uc.id = c.usuario_id
 INNER JOIN restaurante r  ON r.id = p.restaurante_id
  LEFT JOIN entregador e   ON e.usuario_id = p.entregador_id
  LEFT JOIN usuario ue     ON ue.id = e.usuario_id
  LEFT JOIN pagamento pg   ON pg.pedido_id = p.id
 WHERE p.data_pedido >= CURRENT_DATE;

--rollback DROP VIEW IF EXISTS v_pedidos_do_dia;
