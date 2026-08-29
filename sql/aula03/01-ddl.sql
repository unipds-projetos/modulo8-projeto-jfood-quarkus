-- =============================================================================
-- JFood em Quarkus — o esquema de partida
--
-- E o mesmo esquema da Etapa 2, ja com as duas colunas que la foram acrescentadas
-- por ALTER TABLE (pedido.observacao e pedido.taxa_entrega): quando a Etapa 3
-- comeca, elas ja existem.
--
-- A partir da Etapa 4 este arquivo nao e mais usado -- o Liquibase assume o
-- esquema, e o changelog o substitui.
--
-- As tabelas sao criadas na ordem de dependencia: quem nao depende de ninguem
-- vem primeiro, e cada FK so e declarada depois que a tabela referenciada existe.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Passo 1 — tabelas sem dependencia
-- -----------------------------------------------------------------------------

CREATE TABLE usuario (
    id          BIGSERIAL     PRIMARY KEY,
    nome        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL UNIQUE,
    senha_hash  VARCHAR(60)   NOT NULL,
    telefone    VARCHAR(20),
    criado_em   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE categoria_restaurante (
    id    SERIAL       PRIMARY KEY,
    nome  VARCHAR(50)  NOT NULL UNIQUE
);

-- -----------------------------------------------------------------------------
-- Passo 2 — as especializacoes de usuario
--
-- A PK e a mesma do usuario: e isso que impede um usuario de virar dois clientes.
-- -----------------------------------------------------------------------------

CREATE TABLE cliente (
    usuario_id  BIGINT       PRIMARY KEY REFERENCES usuario(id),
    cpf         VARCHAR(11)  NOT NULL UNIQUE
);

CREATE TABLE entregador (
    usuario_id    BIGINT       PRIMARY KEY REFERENCES usuario(id),
    cnh           VARCHAR(11)  NOT NULL UNIQUE,
    tipo_veiculo  VARCHAR(20)  NOT NULL
                  CHECK (tipo_veiculo IN ('MOTO','BICICLETA','CARRO','A_PE')),
    disponivel    BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE dono_restaurante (
    usuario_id  BIGINT       PRIMARY KEY REFERENCES usuario(id),
    cnpj        VARCHAR(14)  NOT NULL UNIQUE
);

-- -----------------------------------------------------------------------------
-- Passo 3 — tabelas com chave estrangeira
-- -----------------------------------------------------------------------------

-- O apelido e unico DENTRO do cliente: "Casa" pode se repetir entre clientes.
CREATE TABLE endereco_entrega (
    id           BIGSERIAL     PRIMARY KEY,
    cliente_id   BIGINT        NOT NULL REFERENCES cliente(usuario_id),
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

CREATE TABLE restaurante (
    id            BIGSERIAL     PRIMARY KEY,
    nome          VARCHAR(120)  NOT NULL,
    categoria_id  INT           NOT NULL REFERENCES categoria_restaurante(id),
    dono_id       BIGINT        NOT NULL REFERENCES dono_restaurante(usuario_id),
    cep           VARCHAR(8)    NOT NULL,
    criado_em     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE item_cardapio (
    id              BIGSERIAL      PRIMARY KEY,
    restaurante_id  BIGINT         NOT NULL REFERENCES restaurante(id),
    nome            VARCHAR(120)   NOT NULL,
    descricao       VARCHAR(255),
    preco           NUMERIC(10,2)  NOT NULL CHECK (preco >= 0),
    disponivel      BOOLEAN        NOT NULL DEFAULT TRUE
);

-- -----------------------------------------------------------------------------
-- Passo 4 — o pedido e seus dependentes
--
-- O CHECK do status espelha, no banco, o enum que a aplicacao vai declarar na
-- Aula 3. Sem ele, um UPDATE feito direto no cliente SQL grava 'ENTREGE'.
-- -----------------------------------------------------------------------------

CREATE TABLE pedido (
    id                   BIGSERIAL      PRIMARY KEY,
    cliente_id           BIGINT         NOT NULL REFERENCES cliente(usuario_id),
    restaurante_id       BIGINT         NOT NULL REFERENCES restaurante(id),
    endereco_entrega_id  BIGINT         NOT NULL REFERENCES endereco_entrega(id),
    entregador_id        BIGINT         REFERENCES entregador(usuario_id),
    status               VARCHAR(20)    NOT NULL DEFAULT 'CRIADO'
                         CHECK (status IN ('CRIADO','CONFIRMADO','EM_PREPARO',
                                           'A_CAMINHO','ENTREGUE','CANCELADO')),
    data_pedido          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    valor_total          NUMERIC(10,2)  NOT NULL DEFAULT 0,
    observacao           VARCHAR(255),
    taxa_entrega         NUMERIC(10,2)
);

-- Entidade fraca: "2 pizzas a R$ 44,90" nao significa nada fora do pedido 137.
CREATE TABLE item_pedido (
    id                BIGSERIAL      PRIMARY KEY,
    pedido_id         BIGINT         NOT NULL REFERENCES pedido(id),
    item_cardapio_id  BIGINT         NOT NULL REFERENCES item_cardapio(id),
    quantidade        INT            NOT NULL CHECK (quantidade > 0),
    -- preco copiado no momento da compra: fato historico, nao redundancia
    preco_unitario    NUMERIC(10,2)  NOT NULL CHECK (preco_unitario >= 0),
    UNIQUE (pedido_id, item_cardapio_id)
);

CREATE TABLE pagamento (
    id         BIGSERIAL      PRIMARY KEY,
    pedido_id  BIGINT         NOT NULL UNIQUE REFERENCES pedido(id),
    metodo     VARCHAR(20)    NOT NULL
               CHECK (metodo IN ('CARTAO_CREDITO','CARTAO_DEBITO','PIX','DINHEIRO')),
    valor      NUMERIC(10,2)  NOT NULL CHECK (valor >= 0),
    status     VARCHAR(20)    NOT NULL
               CHECK (status IN ('PENDENTE','APROVADO','RECUSADO','ESTORNADO')),
    pago_em    TIMESTAMPTZ
);

CREATE TABLE avaliacao (
    id              BIGSERIAL     PRIMARY KEY,
    pedido_id       BIGINT        NOT NULL UNIQUE REFERENCES pedido(id),
    cliente_id      BIGINT        NOT NULL REFERENCES cliente(usuario_id),
    restaurante_id  BIGINT        NOT NULL REFERENCES restaurante(id),
    nota            SMALLINT      NOT NULL CHECK (nota BETWEEN 1 AND 5),
    comentario      VARCHAR(500),
    criado_em       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
