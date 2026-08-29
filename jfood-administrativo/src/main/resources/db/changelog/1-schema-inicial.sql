--liquibase formatted sql

--changeset jfood:1-schema-inicial labels:schema
--comment: convertido de V1__schema_inicial.sql
-- =============================================================================
-- V1 — o esquema inicial do JFood
--
-- E o DDL da Etapa 2, com uma diferenca de estilo: as FKs sao declaradas no FIM,
-- por ALTER TABLE. Assim a ordem dos CREATE TABLE deixa de importar, e a
-- migracao fica legivel de cima para baixo -- primeiro o que existe, depois como
-- as coisas se ligam.
--
-- A coluna taxa_entrega NAO esta aqui: ela entra na V2, para que a linha do
-- tempo registre que ela nasceu depois.
-- =============================================================================

CREATE TABLE usuario (
    id          BIGSERIAL     PRIMARY KEY,
    nome        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL UNIQUE,
    senha_hash  VARCHAR(60)   NOT NULL,
    telefone    VARCHAR(20),
    criado_em   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE cliente (
    usuario_id  BIGINT       PRIMARY KEY,
    cpf         VARCHAR(11)  NOT NULL UNIQUE
);

CREATE TABLE entregador (
    usuario_id    BIGINT       PRIMARY KEY,
    cnh           VARCHAR(11)  NOT NULL UNIQUE,
    tipo_veiculo  VARCHAR(20)  NOT NULL
                  CHECK (tipo_veiculo IN ('MOTO','BICICLETA','CARRO','A_PE')),
    disponivel    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE dono_restaurante (
    usuario_id  BIGINT       PRIMARY KEY,
    cnpj        VARCHAR(14)  NOT NULL UNIQUE
);

CREATE TABLE endereco_entrega (
    id           BIGSERIAL     PRIMARY KEY,
    cliente_id   BIGINT        NOT NULL,
    apelido      VARCHAR(30)   NOT NULL,
    cep          VARCHAR(8)    NOT NULL,
    logradouro   VARCHAR(150)  NOT NULL,
    numero       VARCHAR(10)   NOT NULL,
    complemento  VARCHAR(60),
    bairro       VARCHAR(100)  NOT NULL,
    cidade       VARCHAR(100)  NOT NULL,
    uf           CHAR(2)       NOT NULL,
    UNIQUE (cliente_id, apelido)
);

CREATE TABLE categoria_restaurante (
    id    SERIAL       PRIMARY KEY,
    nome  VARCHAR(50)  NOT NULL UNIQUE
);

CREATE TABLE restaurante (
    id            BIGSERIAL     PRIMARY KEY,
    nome          VARCHAR(120)  NOT NULL,
    categoria_id  INT           NOT NULL,
    dono_id       BIGINT        NOT NULL,
    cep           VARCHAR(8)    NOT NULL,
    criado_em     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE item_cardapio (
    id              BIGSERIAL      PRIMARY KEY,
    restaurante_id  BIGINT         NOT NULL,
    nome            VARCHAR(120)   NOT NULL,
    descricao       VARCHAR(255),
    preco           NUMERIC(10,2)  NOT NULL CHECK (preco >= 0),
    disponivel      BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE pedido (
    id                   BIGSERIAL      PRIMARY KEY,
    cliente_id           BIGINT         NOT NULL,
    restaurante_id       BIGINT         NOT NULL,
    endereco_entrega_id  BIGINT         NOT NULL,
    entregador_id        BIGINT,
    status               VARCHAR(20)    NOT NULL DEFAULT 'CRIADO'
                         CHECK (status IN ('CRIADO','CONFIRMADO','EM_PREPARO',
                                           'A_CAMINHO','ENTREGUE','CANCELADO')),
    data_pedido          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    valor_total          NUMERIC(10,2)  NOT NULL DEFAULT 0,
    observacao           VARCHAR(255)
);

CREATE TABLE item_pedido (
    id                BIGSERIAL      PRIMARY KEY,
    pedido_id         BIGINT         NOT NULL,
    item_cardapio_id  BIGINT         NOT NULL,
    quantidade        INT            NOT NULL CHECK (quantidade > 0),
    preco_unitario    NUMERIC(10,2)  NOT NULL CHECK (preco_unitario >= 0),
    UNIQUE (pedido_id, item_cardapio_id)
);

CREATE TABLE pagamento (
    id         BIGSERIAL      PRIMARY KEY,
    pedido_id  BIGINT         NOT NULL UNIQUE,
    metodo     VARCHAR(20)    NOT NULL
               CHECK (metodo IN ('CARTAO_CREDITO','CARTAO_DEBITO','PIX','DINHEIRO')),
    valor      NUMERIC(10,2)  NOT NULL CHECK (valor >= 0),
    status     VARCHAR(20)    NOT NULL
               CHECK (status IN ('PENDENTE','APROVADO','RECUSADO','ESTORNADO')),
    pago_em    TIMESTAMPTZ
);

CREATE TABLE avaliacao (
    id              BIGSERIAL     PRIMARY KEY,
    pedido_id       BIGINT        NOT NULL UNIQUE,
    cliente_id      BIGINT        NOT NULL,
    restaurante_id  BIGINT        NOT NULL,
    nota            SMALLINT      NOT NULL CHECK (nota BETWEEN 1 AND 5),
    comentario      VARCHAR(500),
    criado_em       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- As chaves estrangeiras, todas juntas no fim
-- -----------------------------------------------------------------------------

ALTER TABLE cliente
    ADD CONSTRAINT fk_cliente_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id);
ALTER TABLE entregador
    ADD CONSTRAINT fk_entregador_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id);
ALTER TABLE dono_restaurante
    ADD CONSTRAINT fk_dono_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id);

ALTER TABLE endereco_entrega
    ADD CONSTRAINT fk_endereco_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(usuario_id);

ALTER TABLE restaurante
    ADD CONSTRAINT fk_restaurante_categoria FOREIGN KEY (categoria_id) REFERENCES categoria_restaurante(id);
ALTER TABLE restaurante
    ADD CONSTRAINT fk_restaurante_dono FOREIGN KEY (dono_id) REFERENCES dono_restaurante(usuario_id);

ALTER TABLE item_cardapio
    ADD CONSTRAINT fk_item_cardapio_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante(id);

ALTER TABLE pedido
    ADD CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(usuario_id);
ALTER TABLE pedido
    ADD CONSTRAINT fk_pedido_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante(id);
ALTER TABLE pedido
    ADD CONSTRAINT fk_pedido_endereco FOREIGN KEY (endereco_entrega_id) REFERENCES endereco_entrega(id);
ALTER TABLE pedido
    ADD CONSTRAINT fk_pedido_entregador FOREIGN KEY (entregador_id) REFERENCES entregador(usuario_id);

ALTER TABLE item_pedido
    ADD CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id);
ALTER TABLE item_pedido
    ADD CONSTRAINT fk_item_pedido_cardapio FOREIGN KEY (item_cardapio_id) REFERENCES item_cardapio(id);

ALTER TABLE pagamento
    ADD CONSTRAINT fk_pagamento_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id);

ALTER TABLE avaliacao
    ADD CONSTRAINT fk_avaliacao_pedido FOREIGN KEY (pedido_id) REFERENCES pedido(id);
ALTER TABLE avaliacao
    ADD CONSTRAINT fk_avaliacao_cliente FOREIGN KEY (cliente_id) REFERENCES cliente(usuario_id);
ALTER TABLE avaliacao
    ADD CONSTRAINT fk_avaliacao_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurante(id);

--rollback DROP TABLE IF EXISTS avaliacao CASCADE;
--rollback DROP TABLE IF EXISTS pagamento CASCADE;
--rollback DROP TABLE IF EXISTS item_pedido CASCADE;
--rollback DROP TABLE IF EXISTS pedido CASCADE;
--rollback DROP TABLE IF EXISTS item_cardapio CASCADE;
--rollback DROP TABLE IF EXISTS restaurante CASCADE;
--rollback DROP TABLE IF EXISTS categoria_restaurante CASCADE;
--rollback DROP TABLE IF EXISTS endereco_entrega CASCADE;
--rollback DROP TABLE IF EXISTS dono_restaurante CASCADE;
--rollback DROP TABLE IF EXISTS entregador CASCADE;
--rollback DROP TABLE IF EXISTS cliente CASCADE;
--rollback DROP TABLE IF EXISTS usuario CASCADE;
