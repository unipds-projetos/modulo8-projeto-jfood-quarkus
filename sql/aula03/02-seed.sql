-- =============================================================================
-- JFood em Quarkus — o seed de partida
--
-- Toda FK e resolvida por SUBQUERY sobre o nome, e nao por id chutado: o seed
-- continua valendo se a ordem de insercao mudar.
--
-- A excecao e pedido.id, inserido explicitamente. E deliberado: item_pedido,
-- pagamento e avaliacao precisam apontar para um pedido especifico, e pedido nao
-- tem chave natural (dois pedidos do mesmo cliente no mesmo restaurante sao
-- pedidos diferentes). Depois da carga, a sequence e reposicionada com setval --
-- esquecer essa linha faz o proximo INSERT da aplicacao estourar chave duplicada.
--
-- O script assume um banco recem-criado pelo 01-ddl.sql.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Categorias de cozinha
-- -----------------------------------------------------------------------------

INSERT INTO categoria_restaurante (nome) VALUES
    ('Italiana'), ('Japonesa'), ('Brasileira'), ('Árabe'), ('Vegana');

-- -----------------------------------------------------------------------------
-- Usuarios — a tabela base das tres especializacoes
-- -----------------------------------------------------------------------------

INSERT INTO usuario (nome, email, senha_hash, telefone) VALUES
    -- donos de restaurante
    ('José Carlos Bianchi', 'ze@cantinadoze.com.br',    '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedA', '11988880001'),
    ('Bruno Napoli',        'bruno@bellanapoli.com.br', '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedB', '11988880002'),
    ('Yuki Tanaka',         'yuki@sushiyuki.com.br',    '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedC', '11988880003'),
    ('Marta Souza',         'marta@sabordaroca.com.br', '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedD', '11988880004'),
    ('Khalil Nassar',       'khalil@esfihadokhalil.com.br', '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedE', '11988880005'),
    ('Paula Verde',         'paula@verdeecia.com.br',   '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedF', '11988880006'),
    -- entregadores
    ('Rafael Souza',        'rafael@entregas.jfood.com','$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedG', '11977770001'),
    ('Tiago Lima',          'tiago@entregas.jfood.com', '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedH', '11977770002'),
    ('Bruna Alves',         'bruna@entregas.jfood.com', '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseedI', '11977770003'),
    -- clientes
    ('Ana Lima',            'ana@email.com',            '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed1', '11966660001'),
    ('Bruno Castro',        'bruno.castro@email.com',   '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed2', '11966660002'),
    ('Carla Menezes',       'carla@email.com',          '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed3', '11966660003'),
    ('Diego Alves',         'diego@email.com',          '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed4', '11966660004'),
    ('Elisa Rocha',         'elisa@email.com',          '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed5', '11966660005'),
    ('Fábio Nunes',         'fabio@email.com',          '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed6', '11966660006'),
    ('Gabriela Dias',       'gabriela@email.com',       '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed7', '11966660007'),
    ('Henrique Sales',      'henrique@email.com',       '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed8', '11966660008'),
    ('Isabela Prado',       'isabela@email.com',        '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed9', '11966660009'),
    ('João Vitor Ramos',    'joao@email.com',           '$2a$10$seedseedseedseedseedseedseedseedseedseedseedseedseed0', '11966660010');

INSERT INTO dono_restaurante (usuario_id, cnpj)
SELECT u.id, d.cnpj
  FROM (VALUES
      ('José Carlos Bianchi', '11222333000101'),
      ('Bruno Napoli',        '11222333000102'),
      ('Yuki Tanaka',         '11222333000103'),
      ('Marta Souza',         '11222333000104'),
      ('Khalil Nassar',       '11222333000105'),
      ('Paula Verde',         '11222333000106')
  ) AS d(nome, cnpj)
  JOIN usuario u ON u.nome = d.nome;

INSERT INTO entregador (usuario_id, cnh, tipo_veiculo, disponivel)
SELECT u.id, e.cnh, e.tipo_veiculo, e.disponivel
  FROM (VALUES
      ('Rafael Souza', '01234567890', 'MOTO',      TRUE),
      ('Tiago Lima',   '01234567891', 'BICICLETA', TRUE),
      ('Bruna Alves',  '01234567892', 'MOTO',      FALSE)
  ) AS e(nome, cnh, tipo_veiculo, disponivel)
  JOIN usuario u ON u.nome = e.nome;

INSERT INTO cliente (usuario_id, cpf)
SELECT u.id, c.cpf
  FROM (VALUES
      ('Ana Lima',         '10000000001'),
      ('Bruno Castro',     '10000000002'),
      ('Carla Menezes',    '10000000003'),
      ('Diego Alves',      '10000000004'),
      ('Elisa Rocha',      '10000000005'),
      ('Fábio Nunes',      '10000000006'),
      ('Gabriela Dias',    '10000000007'),
      ('Henrique Sales',   '10000000008'),
      ('Isabela Prado',    '10000000009'),
      ('João Vitor Ramos', '10000000010')
  ) AS c(nome, cpf)
  JOIN usuario u ON u.nome = c.nome;

-- -----------------------------------------------------------------------------
-- Enderecos de entrega — o multivalorado da Etapa 1, agora em linhas
-- -----------------------------------------------------------------------------

INSERT INTO endereco_entrega (cliente_id, apelido, cep, logradouro, numero, complemento, bairro, cidade, uf)
SELECT c.usuario_id, e.apelido, e.cep, e.logradouro, e.numero, e.complemento, e.bairro, e.cidade, e.uf
  FROM (VALUES
      ('Ana Lima',         'Casa',      '01311000', 'Avenida Paulista',      '1000', 'Apto 52',  'Bela Vista',   'São Paulo', 'SP'),
      ('Ana Lima',         'Trabalho',  '04538133', 'Avenida Brigadeiro Faria Lima', '3477', '9º andar', 'Itaim Bibi', 'São Paulo', 'SP'),
      ('Bruno Castro',     'Casa',      '05407002', 'Rua Cardeal Arcoverde', '2365', NULL,       'Pinheiros',    'São Paulo', 'SP'),
      ('Carla Menezes',    'Casa',      '04094050', 'Rua Domingos de Morais','2187', 'Bloco B',  'Vila Mariana', 'São Paulo', 'SP'),
      ('Diego Alves',      'Casa',      '02011000', 'Rua Voluntários da Pátria', '1200', NULL,   'Santana',      'São Paulo', 'SP'),
      ('Elisa Rocha',      'Casa',      '03164200', 'Rua Padre Adelino',     '700',  'Casa 2',   'Belenzinho',   'São Paulo', 'SP'),
      ('Fábio Nunes',      'Casa',      '05014001', 'Rua Cardoso de Almeida','1200', NULL,       'Perdizes',     'São Paulo', 'SP'),
      ('Gabriela Dias',    'Casa',      '01415000', 'Rua Oscar Freire',      '900',  'Apto 101', 'Jardins',      'São Paulo', 'SP'),
      ('Henrique Sales',   'Casa',      '08090280', 'Avenida São Miguel',    '4500', NULL,       'Vila Nova Curuçá','São Paulo','SP'),
      ('Isabela Prado',    'Casa',      '04571010', 'Rua Funchal',           '500',  'Conj 21',  'Vila Olímpia', 'São Paulo', 'SP'),
      ('João Vitor Ramos', 'Casa',      '09541100', 'Rua Cesário Bastos',    '150',  NULL,       'Vila Bastos',  'Santo André','SP'),
      ('João Vitor Ramos', 'Trabalho',  '01310100', 'Avenida Paulista',      '2300', 'Sala 12',  'Cerqueira César','São Paulo','SP')
  ) AS e(cliente, apelido, cep, logradouro, numero, complemento, bairro, cidade, uf)
  JOIN usuario u ON u.nome = e.cliente
  JOIN cliente c ON c.usuario_id = u.id;

-- -----------------------------------------------------------------------------
-- Restaurantes
--
-- Dois deles nunca receberam pedido, de proposito:
--   Verde & Cia   — cadastrado ha 45 dias  -> aparece no anti-join
--   Forno & Pizza — cadastrado ha 10 dias  -> NAO aparece (menos de 30 dias)
-- -----------------------------------------------------------------------------

INSERT INTO restaurante (nome, categoria_id, dono_id, cep, criado_em)
SELECT r.nome, cat.id, u.id, r.cep, NOW() - (r.dias_atras || ' days')::INTERVAL
  FROM (VALUES
      ('Cantina do Zé',         'Italiana',   'José Carlos Bianchi', '01311000', 400),
      ('Pizzaria Bella Napoli', 'Italiana',   'Bruno Napoli',        '05407002', 320),
      ('Sushi Yuki',            'Japonesa',   'Yuki Tanaka',         '04538133', 260),
      ('Sabor da Roça',         'Brasileira', 'Marta Souza',         '02011000', 180),
      ('Esfiha do Khalil',      'Árabe',      'Khalil Nassar',       '03164200', 120),
      ('Verde & Cia',           'Vegana',     'Paula Verde',         '01415000', 45),
      ('Forno & Pizza',         'Italiana',   'Bruno Napoli',        '05014001', 10)
  ) AS r(nome, categoria, dono, cep, dias_atras)
  JOIN categoria_restaurante cat ON cat.nome = r.categoria
  JOIN usuario u ON u.nome = r.dono;

-- -----------------------------------------------------------------------------
-- Cardapios
-- -----------------------------------------------------------------------------

INSERT INTO item_cardapio (restaurante_id, nome, descricao, preco)
SELECT r.id, i.nome, i.descricao, i.preco
  FROM (VALUES
      ('Cantina do Zé',         'Pizza Margherita',      'Molho de tomate, muçarela de búfala e manjericão', 54.90),
      ('Cantina do Zé',         'Pizza Calabresa',       'Calabresa artesanal e cebola roxa',                49.90),
      ('Cantina do Zé',         'Lasanha à Bolonhesa',   'Massa fresca e ragu de carne',                     62.00),
      ('Cantina do Zé',         'Refrigerante Lata',     'Lata 350ml',                                        8.00),
      ('Pizzaria Bella Napoli', 'Pizza Quatro Queijos',  'Muçarela, gorgonzola, parmesão e catupiry',        59.90),
      ('Pizzaria Bella Napoli', 'Pizza Portuguesa',      'Presunto, ovo, cebola e ervilha',                  52.90),
      ('Pizzaria Bella Napoli', 'Água Mineral',          'Garrafa 500ml',                                     6.00),
      ('Sushi Yuki',            'Combinado 20 peças',    'Sashimi, niguiri e uramaki',                       89.90),
      ('Sushi Yuki',            'Temaki Salmão',         'Cone de alga com salmão e cream cheese',           34.90),
      ('Sushi Yuki',            'Hot Roll',              '8 peças empanadas',                                29.90),
      ('Sabor da Roça',         'Feijoada Individual',   'Com couve, farofa e laranja',                      46.50),
      ('Sabor da Roça',         'Picanha na Chapa',      'Acompanha arroz, fritas e vinagrete',              78.00),
      ('Sabor da Roça',         'Suco de Laranja',       'Natural, 500ml',                                   12.00),
      ('Esfiha do Khalil',      'Esfiha de Carne',       'Unidade, massa aberta',                             7.50),
      ('Esfiha do Khalil',      'Esfiha de Queijo',      'Unidade, massa fechada',                            8.50),
      ('Esfiha do Khalil',      'Kibe Frito',            'Unidade recheada com coalhada',                     9.90),
      ('Esfiha do Khalil',      'Kafta no Espeto',       'Duas unidades com pão sírio',                      32.00),
      ('Verde & Cia',           'Bowl de Grãos',         'Quinoa, grão-de-bico e legumes assados',           42.00),
      ('Verde & Cia',           'Hambúrguer de Jaca',    'Pão australiano e maionese vegana',                39.90),
      ('Forno & Pizza',         'Pizza Vegetariana',     'Abobrinha, berinjela e pimentão',                  51.90),
      ('Forno & Pizza',         'Pizza Marguerita Zero', 'Sem lactose, massa integral',                      56.90)
  ) AS i(restaurante, nome, descricao, preco)
  JOIN restaurante r ON r.nome = i.restaurante;

-- -----------------------------------------------------------------------------
-- Pedidos
--
-- Distribuicao proposital para as consultas do passo 5:
--   Cantina do Zé 6 | Bella Napoli 5 | Sushi Yuki 4 | Sabor da Roça 3 | Esfiha 2
-- Assim "mais de 3 pedidos" separa os tres primeiros dos dois ultimos.
-- -----------------------------------------------------------------------------

INSERT INTO pedido (id, cliente_id, restaurante_id, endereco_entrega_id, entregador_id,
                    status, data_pedido, taxa_entrega, observacao)
SELECT p.id, c.usuario_id, r.id, e.id, ent.usuario_id,
       p.status, NOW() - (p.dias_atras || ' days')::INTERVAL, p.taxa, p.observacao
  FROM (VALUES
      ( 1, 'Ana Lima',         'Cantina do Zé',         'Casa',     'Rafael Souza', 'ENTREGUE',   58,  7.90, 'Sem cebola'::VARCHAR),
      ( 2, 'Bruno Castro',     'Cantina do Zé',         'Casa',     'Tiago Lima',   'ENTREGUE',   52,  9.90, NULL),
      ( 3, 'Carla Menezes',    'Cantina do Zé',         'Casa',     'Rafael Souza', 'ENTREGUE',   45,  7.90, NULL),
      ( 4, 'Ana Lima',         'Cantina do Zé',         'Trabalho', 'Bruna Alves',  'ENTREGUE',   30, 12.00, 'Entregar na portaria'),
      ( 5, 'Diego Alves',      'Cantina do Zé',         'Casa',     'Tiago Lima',   'ENTREGUE',   12,  8.90, NULL),
      ( 6, 'Elisa Rocha',      'Cantina do Zé',         'Casa',      NULL,          'CANCELADO',   9,  7.90, 'Cliente desistiu'),
      ( 7, 'Bruno Castro',     'Pizzaria Bella Napoli', 'Casa',     'Rafael Souza', 'ENTREGUE',   40,  6.90, NULL),
      ( 8, 'Fábio Nunes',      'Pizzaria Bella Napoli', 'Casa',     'Tiago Lima',   'ENTREGUE',   33,  6.90, NULL),
      ( 9, 'Gabriela Dias',    'Pizzaria Bella Napoli', 'Casa',     'Bruna Alves',  'ENTREGUE',   21,  9.50, 'Caprichar no queijo'),
      (10, 'Henrique Sales',   'Pizzaria Bella Napoli', 'Casa',     'Rafael Souza', 'ENTREGUE',   14, 14.90, NULL),
      (11, 'Isabela Prado',    'Pizzaria Bella Napoli', 'Casa',      NULL,          'EM_PREPARO',  0,  8.90, NULL),
      (12, 'Ana Lima',         'Sushi Yuki',            'Casa',     'Tiago Lima',   'ENTREGUE',   47, 11.90, NULL),
      (13, 'Carla Menezes',    'Sushi Yuki',            'Casa',     'Rafael Souza', 'ENTREGUE',   26, 11.90, 'Sem wasabi'),
      (14, 'João Vitor Ramos', 'Sushi Yuki',            'Trabalho', 'Bruna Alves',  'ENTREGUE',   18, 13.50, NULL),
      (15, 'Gabriela Dias',    'Sushi Yuki',            'Casa',     'Rafael Souza', 'A_CAMINHO',   0, 11.90, NULL),
      (16, 'Diego Alves',      'Sabor da Roça',         'Casa',     'Tiago Lima',   'ENTREGUE',   36,  9.90, NULL),
      (17, 'Elisa Rocha',      'Sabor da Roça',         'Casa',     'Rafael Souza', 'ENTREGUE',   23,  9.90, NULL),
      (18, 'Henrique Sales',   'Sabor da Roça',         'Casa',      NULL,          'CRIADO',      0, 10.90, NULL),
      (19, 'Fábio Nunes',      'Esfiha do Khalil',      'Casa',     'Tiago Lima',   'ENTREGUE',   28,  5.90, NULL),
      (20, 'Isabela Prado',    'Esfiha do Khalil',      'Casa',     'Bruna Alves',  'ENTREGUE',    5,  5.90, 'Bem assada')
  ) AS p(id, cliente, restaurante, apelido, entregador, status, dias_atras, taxa, observacao)
  JOIN usuario uc ON uc.nome = p.cliente
  JOIN cliente c  ON c.usuario_id = uc.id
  JOIN restaurante r ON r.nome = p.restaurante
  JOIN endereco_entrega e ON e.cliente_id = c.usuario_id AND e.apelido = p.apelido
  LEFT JOIN usuario ue    ON ue.nome = p.entregador
  LEFT JOIN entregador ent ON ent.usuario_id = ue.id;

-- Sem esta linha, o proximo INSERT da aplicacao tenta o id 1 e estoura a PK.
SELECT setval('pedido_id_seq', (SELECT MAX(id) FROM pedido));

-- -----------------------------------------------------------------------------
-- Itens dos pedidos
--
-- O preco_unitario vem do cardapio no momento da carga -- e a partir daqui ele e
-- historico: reajustar item_cardapio.preco nao mexe em pedido nenhum.
-- -----------------------------------------------------------------------------

INSERT INTO item_pedido (pedido_id, item_cardapio_id, quantidade, preco_unitario)
SELECT ip.pedido_id, ic.id, ip.quantidade, ic.preco
  FROM (VALUES
      ( 1, 'Pizza Margherita',     2), ( 1, 'Refrigerante Lata',    2),
      ( 2, 'Pizza Calabresa',      1), ( 2, 'Lasanha à Bolonhesa',  1),
      ( 3, 'Lasanha à Bolonhesa',  2),
      ( 4, 'Pizza Margherita',     1), ( 4, 'Pizza Calabresa',      1), ( 4, 'Refrigerante Lata', 4),
      ( 5, 'Pizza Calabresa',      1),
      ( 6, 'Pizza Margherita',     1),
      ( 7, 'Pizza Quatro Queijos', 1), ( 7, 'Água Mineral',         2),
      ( 8, 'Pizza Portuguesa',     2),
      ( 9, 'Pizza Quatro Queijos', 3), ( 9, 'Água Mineral',         3),
      (10, 'Pizza Portuguesa',     1), (10, 'Pizza Quatro Queijos', 1),
      (11, 'Pizza Portuguesa',     1),
      (12, 'Combinado 20 peças',   1), (12, 'Temaki Salmão',        2),
      (13, 'Hot Roll',             2),
      (14, 'Combinado 20 peças',   2), (14, 'Hot Roll',             1),
      (15, 'Temaki Salmão',        1),
      (16, 'Feijoada Individual',  2), (16, 'Suco de Laranja',      2),
      (17, 'Picanha na Chapa',     1), (17, 'Suco de Laranja',      1),
      (18, 'Feijoada Individual',  1),
      (19, 'Esfiha de Carne',      6), (19, 'Esfiha de Queijo',     4), (19, 'Kibe Frito', 2),
      (20, 'Kafta no Espeto',      1), (20, 'Esfiha de Carne',      3)
  ) AS ip(pedido_id, item, quantidade)
  JOIN pedido p ON p.id = ip.pedido_id
  JOIN item_cardapio ic ON ic.restaurante_id = p.restaurante_id AND ic.nome = ip.item;

-- -----------------------------------------------------------------------------
-- valor_total: a coluna materializada, mantida por quem escreve os itens
--
-- Aqui e o seed que a mantem; na aplicacao (Aula 3) e o servico, na mesma
-- transacao dos itens. A Etapa 6 compara esta escolha com trigger e com view.
-- -----------------------------------------------------------------------------

UPDATE pedido p
   SET valor_total = COALESCE(p.taxa_entrega, 0) + (
           SELECT SUM(i.quantidade * i.preco_unitario)
             FROM item_pedido i
            WHERE i.pedido_id = p.id
       );

-- -----------------------------------------------------------------------------
-- Pagamentos e avaliacoes
-- -----------------------------------------------------------------------------

-- Pedido CRIADO ainda nao foi pago; CANCELADO teve o pagamento estornado.
INSERT INTO pagamento (pedido_id, metodo, valor, status, pago_em)
SELECT p.id,
       (ARRAY['PIX','CARTAO_CREDITO','CARTAO_DEBITO','DINHEIRO'])[1 + (p.id % 4)],
       p.valor_total,
       CASE WHEN p.status = 'CANCELADO' THEN 'ESTORNADO' ELSE 'APROVADO' END,
       p.data_pedido + INTERVAL '2 minutes'
  FROM pedido p
 WHERE p.status <> 'CRIADO';

INSERT INTO avaliacao (pedido_id, cliente_id, restaurante_id, nota, comentario, criado_em)
SELECT p.id, p.cliente_id, p.restaurante_id, a.nota, a.comentario,
       p.data_pedido + INTERVAL '1 day'
  FROM (VALUES
      ( 1, 5, 'Chegou quente e no prazo.'::VARCHAR),
      ( 2, 4, 'Boa lasanha, entrega um pouco lenta.'),
      ( 3, 5, NULL),
      ( 4, 5, 'Sempre impecável.'),
      ( 7, 4, NULL),
      ( 8, 3, 'A portuguesa veio com pouco recheio.'),
      ( 9, 5, 'Melhor quatro queijos da região.'),
      (12, 5, 'Peixe fresquíssimo.'),
      (13, 4, NULL),
      (14, 5, 'Combinado generoso.'),
      (16, 5, 'Feijoada de domingo em pleno dia útil.'),
      (17, 4, NULL),
      (19, 5, 'Esfiha na medida.')
  ) AS a(pedido_id, nota, comentario)
  JOIN pedido p ON p.id = a.pedido_id;
