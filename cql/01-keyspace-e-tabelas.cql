-- =============================================================================
-- JFood — Etapa 10: o tracking de entregadores
--
--   docker exec -i jfood-cassandra cqlsh < cql/01-keyspace-e-tabelas.cql
--
-- MODELAGEM QUERY-FIRST: antes de escrever CQL, escrevemos as PERGUNTAS.
--
--   1. "onde esta o entregador X agora?"
--   2. "qual foi a rota completa da entrega Y?"
--   3. "por onde o entregador X passou no dia D?"
--
-- Elas NAO sao atendidas pela mesma tabela. No relacional, uma tabela serve a
-- muitas consultas porque o indice conserta o que a modelagem nao previu. Aqui a
-- particao decide em qual NO o dado mora -- e a consulta que nao respeita a
-- particao ou faz scatter-gather no anel inteiro, ou nao roda.
--
-- Entao: uma tabela por pergunta, e a duplicacao e assumida.
-- =============================================================================

CREATE KEYSPACE IF NOT EXISTS jfood_tracking
WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
-- SimpleStrategy com fator 1 serve APENAS para a maquina local. Em producao:
-- NetworkTopologyStrategy com fator 3, cada ponto gravado em 3 racks distintos.

USE jfood_tracking;

-- -----------------------------------------------------------------------------
-- Pergunta 3 — "por onde o entregador X passou no dia D?"
--
-- A tabela principal, e a que recebe as 40.000 escritas por segundo.
--
--   PRIMARY KEY ( (entregador_id, dia) , instante )
--                 ^^^^^^^^^^^^^^^^^^^    ^^^^^^^^
--                 Partition Key          Clustering Key
--                 (em qual no?)          (em que ordem no disco?)
--
-- O BUCKETING por dia esta nos parenteses internos, e a conta que o justifica
-- esta em docs/aula10.md: sem ele, a particao de um entregador cresce para
-- sempre.
--
-- CLUSTERING ORDER BY (instante DESC) grava o disco JA ORDENADO: "a ultima
-- posicao" e a primeira linha da particao, sem ORDER BY e sem sort.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS rota_por_entregador_dia (
    entregador_id  BIGINT,
    dia            DATE,
    instante       TIMESTAMP,
    latitude       DOUBLE,
    longitude      DOUBLE,
    velocidade_kmh DOUBLE,
    entrega_id     BIGINT,
    PRIMARY KEY ((entregador_id, dia), instante)
) WITH CLUSTERING ORDER BY (instante DESC);

-- -----------------------------------------------------------------------------
-- Pergunta 2 — "qual foi a rota completa da entrega Y?"
--
-- Particao pela ENTREGA: uma corrida dura de 15 a 60 minutos, entao a particao
-- nasce limitada (180 a 720 pontos) sem precisar de bucket.
--
-- Aqui a ordem e ASC: a rota e reconstruida do inicio ao fim para desenhar no
-- mapa e auditar tempo de entrega.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS rota_por_entrega (
    entrega_id     BIGINT,
    instante       TIMESTAMP,
    entregador_id  BIGINT,
    latitude       DOUBLE,
    longitude      DOUBLE,
    velocidade_kmh DOUBLE,
    PRIMARY KEY ((entrega_id), instante)
) WITH CLUSTERING ORDER BY (instante ASC);

-- -----------------------------------------------------------------------------
-- Pergunta 1 — "onde esta o entregador X agora?"
--
-- Uma linha por entregador, sobrescrita a cada ping. E a consulta mais frequente
-- do sistema -- o app do cliente a repete a cada poucos segundos -- e ela precisa
-- ler UMA linha, e nao a primeira de uma particao com milhares.
--
-- No Cassandra, UPDATE e INSERT sao a mesma operacao (upsert): sobrescrever nao
-- custa mais caro que inserir.
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS posicao_atual_entregador (
    entregador_id  BIGINT PRIMARY KEY,
    instante       TIMESTAMP,
    latitude       DOUBLE,
    longitude      DOUBLE,
    velocidade_kmh DOUBLE,
    entrega_id     BIGINT
);
