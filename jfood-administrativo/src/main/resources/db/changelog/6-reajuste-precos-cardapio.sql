--liquibase formatted sql

--changeset jfood:6-reajuste-precos-cardapio labels:seed
--comment: convertido de V6__reajuste_precos_cardapio.sql
-- =============================================================================
-- V6 — reajuste de precos do cardapio
--
-- Esta migracao existe por causa de um erro: a vontade era editar uma migracao
-- ja aplicada. Nao da -- o Flyway guarda o checksum de cada script e para na
-- proxima subida se ele mudar.
--
-- E o erro esconde uma verdade: um reajuste NAO e correcao do passado, e um fato
-- de negocio que aconteceu depois. Ele merece o proprio ponto na linha do tempo.
--
-- Repare no efeito colateral que NAO acontece: item_pedido.preco_unitario nao se
-- mexe. Os pedidos ja feitos continuam valendo o que foram cobrados -- e por isso
-- o preco foi copiado la atras, na Etapa 1.
-- =============================================================================

UPDATE item_cardapio
   SET preco = ROUND(preco * 1.08, 2)
 WHERE restaurante_id IN (
           SELECT id FROM restaurante WHERE nome IN ('Cantina do Zé', 'Pizzaria Bella Napoli')
       );

-- NAO E REVERSIVEL, e declarar isso e melhor do que fingir que e.
--
-- O inverso aritmetico seria dividir por 1.08 -- mas o ROUND(x, 2) do reajuste
-- descartou centavos que nao voltam. Um preco de 54.90 virou 59.29; dividir
-- 59.29 por 1.08 da 54.898..., que arredonda para 54.90 por sorte, e nao por
-- garantia. Em outros valores a volta erra um centavo, e um centavo errado em
-- preco de cardapio e divergencia contabil.
--
-- O `empty` diz ao Liquibase que este changeset nao tem desfazer. O rollback de
-- uma tag anterior a ele vai FALHAR e avisar -- que e o comportamento correto.
--rollback empty
