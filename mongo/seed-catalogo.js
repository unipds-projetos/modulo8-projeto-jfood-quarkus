// =============================================================================
// JFood — Etapa 8: o catalogo no MongoDB
//
//   docker exec -i jfood-mongodb mongosh -u admin -p adminpassword \
//     --authenticationDatabase admin jfood_catalogo < mongo/seed-catalogo.js
//
// Os restaurantes sao os mesmos do PostgreSQL da Etapa 2 -- de proposito: o
// mesmo dominio, guardado de dois jeitos diferentes, e o que torna a comparacao
// possivel.
//
// Repare no que MUDA em relacao ao relacional: cada item carrega uma lista de
// OPCIONAIS com formato proprio. Tamanho de pizza tem 3 escolhas; ponto da
// carne tem 5; adicionais permitem escolher varios. Em tabela, isso viraria EAV
// ou quarenta colunas nulas.
// =============================================================================

db.restaurantes.drop();
db.itens_cardapio.drop();

const tamanhoPizza = {
  nome: "Tamanho", obrigatorio: true, minimo_escolhas: 1, maximo_escolhas: 1,
  escolhas: [
    { nome: "Broto",  acrescimo: NumberDecimal("0.00") },
    { nome: "Media",  acrescimo: NumberDecimal("8.00") },
    { nome: "Grande", acrescimo: NumberDecimal("16.00") }
  ]
};

const bordaPizza = {
  nome: "Borda recheada", obrigatorio: false, minimo_escolhas: 0, maximo_escolhas: 1,
  escolhas: [
    { nome: "Sem borda",  acrescimo: NumberDecimal("0.00") },
    { nome: "Catupiry",   acrescimo: NumberDecimal("9.90") },
    { nome: "Cheddar",    acrescimo: NumberDecimal("9.90") }
  ]
};

const pontoCarne = {
  nome: "Ponto da carne", obrigatorio: true, minimo_escolhas: 1, maximo_escolhas: 1,
  escolhas: [
    { nome: "Mal passada",   acrescimo: NumberDecimal("0.00") },
    { nome: "Ao ponto",      acrescimo: NumberDecimal("0.00") },
    { nome: "Bem passada",   acrescimo: NumberDecimal("0.00") }
  ]
};

const acompanhamentos = {
  nome: "Acompanhamentos", obrigatorio: false, minimo_escolhas: 0, maximo_escolhas: 3,
  escolhas: [
    { nome: "Farofa",    acrescimo: NumberDecimal("4.00") },
    { nome: "Vinagrete", acrescimo: NumberDecimal("3.50") },
    { nome: "Couve",     acrescimo: NumberDecimal("5.00") }
  ]
};

const restaurantes = [
  {
    nome: "Cantina do Zé", categoria: "Italiana",
    foto_url: "https://cdn.jfood.example/rest/cantina-do-ze.jpg",
    nota_media: NumberDecimal("4.7"), taxa_entrega: NumberDecimal("7.90"),
    aberto: true, abre_as: "18:00", fecha_as: "23:30", visualizacoes: NumberLong("0"),
    cardapio: [
      { nome: "Pizzas", ordem: 1, itens: [
        { nome: "Pizza Margherita", descricao: "Molho de tomate, muçarela de búfala e manjericão",
          preco: NumberDecimal("59.29"), foto_url: "https://cdn.jfood.example/item/margherita.jpg",
          tags: ["vegetariano"], disponivel: true, opcionais: [tamanhoPizza, bordaPizza] },
        { nome: "Pizza Calabresa", descricao: "Calabresa artesanal e cebola roxa",
          preco: NumberDecimal("53.89"), foto_url: "https://cdn.jfood.example/item/calabresa.jpg",
          tags: ["picante"], disponivel: true, opcionais: [tamanhoPizza, bordaPizza] }
      ]},
      { nome: "Massas", ordem: 2, itens: [
        { nome: "Lasanha à Bolonhesa", descricao: "Massa fresca e ragu de carne",
          preco: NumberDecimal("66.96"), foto_url: "https://cdn.jfood.example/item/lasanha.jpg",
          tags: [], disponivel: true, opcionais: [] }
      ]},
      { nome: "Bebidas", ordem: 3, itens: [
        { nome: "Refrigerante Lata", descricao: "Lata 350ml", preco: NumberDecimal("8.64"),
          foto_url: "https://cdn.jfood.example/item/refri.jpg",
          tags: [], disponivel: true, opcionais: [] }
      ]}
    ]
  },
  {
    nome: "Pizzaria Bella Napoli", categoria: "Italiana",
    foto_url: "https://cdn.jfood.example/rest/bella-napoli.jpg",
    nota_media: NumberDecimal("4.5"), taxa_entrega: NumberDecimal("6.90"),
    aberto: true, abre_as: "18:30", fecha_as: "00:00", visualizacoes: NumberLong("0"),
    cardapio: [
      { nome: "Pizzas", ordem: 1, itens: [
        { nome: "Pizza Quatro Queijos", descricao: "Muçarela, gorgonzola, parmesão e catupiry",
          preco: NumberDecimal("64.69"), foto_url: "https://cdn.jfood.example/item/4queijos.jpg",
          tags: ["vegetariano"], disponivel: true, opcionais: [tamanhoPizza, bordaPizza] },
        { nome: "Pizza Portuguesa", descricao: "Presunto, ovo, cebola e ervilha",
          preco: NumberDecimal("57.13"), foto_url: "https://cdn.jfood.example/item/portuguesa.jpg",
          tags: [], disponivel: true, opcionais: [tamanhoPizza, bordaPizza] }
      ]},
      { nome: "Bebidas", ordem: 2, itens: [
        { nome: "Água Mineral", descricao: "Garrafa 500ml", preco: NumberDecimal("6.48"),
          foto_url: "https://cdn.jfood.example/item/agua.jpg",
          tags: ["vegano", "sem gluten"], disponivel: true, opcionais: [] }
      ]}
    ]
  },
  {
    nome: "Sushi Yuki", categoria: "Japonesa",
    foto_url: "https://cdn.jfood.example/rest/sushi-yuki.jpg",
    nota_media: NumberDecimal("4.9"), taxa_entrega: NumberDecimal("11.90"),
    aberto: true, abre_as: "19:00", fecha_as: "23:00", visualizacoes: NumberLong("0"),
    cardapio: [
      { nome: "Combinados", ordem: 1, itens: [
        { nome: "Combinado 20 peças", descricao: "Sashimi, niguiri e uramaki",
          preco: NumberDecimal("89.90"), foto_url: "https://cdn.jfood.example/item/combinado20.jpg",
          tags: ["sem gluten"], disponivel: true, opcionais: [] }
      ]},
      { nome: "Temakis", ordem: 2, itens: [
        { nome: "Temaki Salmão", descricao: "Cone de alga com salmão e cream cheese",
          preco: NumberDecimal("34.90"), foto_url: "https://cdn.jfood.example/item/temaki.jpg",
          tags: [], disponivel: true, opcionais: [] },
        { nome: "Hot Roll", descricao: "8 peças empanadas", preco: NumberDecimal("29.90"),
          foto_url: "https://cdn.jfood.example/item/hotroll.jpg",
          tags: [], disponivel: true, opcionais: [] }
      ]}
    ]
  },
  {
    nome: "Sabor da Roça", categoria: "Brasileira",
    foto_url: "https://cdn.jfood.example/rest/sabor-da-roca.jpg",
    nota_media: NumberDecimal("4.6"), taxa_entrega: NumberDecimal("9.90"),
    aberto: true, abre_as: "11:00", fecha_as: "15:00", visualizacoes: NumberLong("0"),
    cardapio: [
      { nome: "Pratos", ordem: 1, itens: [
        { nome: "Feijoada Individual", descricao: "Com couve, farofa e laranja",
          preco: NumberDecimal("46.50"), foto_url: "https://cdn.jfood.example/item/feijoada.jpg",
          tags: [], disponivel: true, opcionais: [acompanhamentos] },
        { nome: "Picanha na Chapa", descricao: "Acompanha arroz, fritas e vinagrete",
          preco: NumberDecimal("78.00"), foto_url: "https://cdn.jfood.example/item/picanha.jpg",
          tags: [], disponivel: true, opcionais: [pontoCarne, acompanhamentos] }
      ]},
      { nome: "Bebidas", ordem: 2, itens: [
        { nome: "Suco de Laranja", descricao: "Natural, 500ml", preco: NumberDecimal("12.00"),
          foto_url: "https://cdn.jfood.example/item/suco.jpg",
          tags: ["vegano", "sem gluten"], disponivel: true, opcionais: [] }
      ]}
    ]
  },
  {
    nome: "Esfiha do Khalil", categoria: "Árabe",
    foto_url: "https://cdn.jfood.example/rest/esfiha-khalil.jpg",
    nota_media: NumberDecimal("4.4"), taxa_entrega: NumberDecimal("5.90"),
    aberto: true, abre_as: "17:00", fecha_as: "23:00", visualizacoes: NumberLong("0"),
    cardapio: [
      { nome: "Esfihas", ordem: 1, itens: [
        { nome: "Esfiha de Carne", descricao: "Unidade, massa aberta", preco: NumberDecimal("7.50"),
          foto_url: "https://cdn.jfood.example/item/esfiha-carne.jpg",
          tags: [], disponivel: true, opcionais: [] },
        { nome: "Esfiha de Queijo", descricao: "Unidade, massa fechada", preco: NumberDecimal("8.50"),
          foto_url: "https://cdn.jfood.example/item/esfiha-queijo.jpg",
          tags: ["vegetariano"], disponivel: true, opcionais: [] }
      ]},
      { nome: "Pratos", ordem: 2, itens: [
        { nome: "Kibe Frito", descricao: "Unidade recheada com coalhada", preco: NumberDecimal("9.90"),
          foto_url: "https://cdn.jfood.example/item/kibe.jpg",
          tags: [], disponivel: true, opcionais: [] },
        { nome: "Kafta no Espeto", descricao: "Duas unidades com pão sírio",
          preco: NumberDecimal("32.00"), foto_url: "https://cdn.jfood.example/item/kafta.jpg",
          tags: ["picante"], disponivel: true, opcionais: [pontoCarne] }
      ]}
    ]
  },
  {
    nome: "Verde & Cia", categoria: "Vegana",
    foto_url: "https://cdn.jfood.example/rest/verde-e-cia.jpg",
    nota_media: NumberDecimal("4.8"), taxa_entrega: NumberDecimal("8.90"),
    aberto: true, abre_as: "11:30", fecha_as: "21:00", visualizacoes: NumberLong("0"),
    cardapio: [
      { nome: "Pratos", ordem: 1, itens: [
        { nome: "Bowl de Grãos", descricao: "Quinoa, grão-de-bico e legumes assados",
          preco: NumberDecimal("42.00"), foto_url: "https://cdn.jfood.example/item/bowl.jpg",
          tags: ["vegano", "sem gluten"], disponivel: true, opcionais: [] },
        { nome: "Hambúrguer de Jaca", descricao: "Pão australiano e maionese vegana",
          preco: NumberDecimal("39.90"), foto_url: "https://cdn.jfood.example/item/jaca.jpg",
          tags: ["vegano"], disponivel: true, opcionais: [] }
      ]}
    ]
  },
  {
    // Fechado: entra na listagem, mas fica FORA do relatorio de preco medio.
    nome: "Forno & Pizza", categoria: "Italiana",
    foto_url: "https://cdn.jfood.example/rest/forno-e-pizza.jpg",
    nota_media: NumberDecimal("4.1"), taxa_entrega: NumberDecimal("7.50"),
    aberto: false, abre_as: "19:00", fecha_as: "23:00", visualizacoes: NumberLong("0"),
    cardapio: [
      { nome: "Pizzas", ordem: 1, itens: [
        { nome: "Pizza Vegetariana", descricao: "Abobrinha, berinjela e pimentão",
          preco: NumberDecimal("51.90"), foto_url: "https://cdn.jfood.example/item/vegetariana.jpg",
          tags: ["vegetariano"], disponivel: true, opcionais: [tamanhoPizza] },
        { nome: "Pizza Marguerita Zero", descricao: "Sem lactose, massa integral",
          preco: NumberDecimal("56.90"), foto_url: "https://cdn.jfood.example/item/zero.jpg",
          tags: ["vegano"], disponivel: true, opcionais: [tamanhoPizza] }
      ]}
    ]
  }
];

db.restaurantes.insertMany(restaurantes);

// -----------------------------------------------------------------------------
// SUBSET PATTERN: a colecao itens_cardapio
//
// Cada item vira um documento proprio e carrega DENTRO dele o subset do
// restaurante que a tela de resultado de busca precisa -- _id, nome, foto, nota
// e taxa de entrega. E o item embutido no restaurante ganha o item_id de volta,
// fechando os dois sentidos.
// -----------------------------------------------------------------------------

db.restaurantes.find().forEach(function (r) {
  const subset = {
    restaurante_id: r._id,
    nome: r.nome,
    foto_url: r.foto_url,
    nota_media: r.nota_media,
    taxa_entrega: r.taxa_entrega
  };

  r.cardapio.forEach(function (secao) {
    secao.itens.forEach(function (item) {
      const doc = {
        nome: item.nome,
        descricao: item.descricao,
        preco: item.preco,
        foto_url: item.foto_url,
        tags: item.tags,
        secao: secao.nome,
        disponivel: item.disponivel,
        opcionais: item.opcionais,
        restaurante: subset
      };
      const resultado = db.itens_cardapio.insertOne(doc);
      item.item_id = resultado.insertedId;
    });
  });

  db.restaurantes.updateOne({ _id: r._id }, { $set: { cardapio: r.cardapio } });
});

print("restaurantes:   " + db.restaurantes.countDocuments());
print("itens_cardapio: " + db.itens_cardapio.countDocuments());
print("veganos (notacao de ponto): " +
      db.restaurantes.countDocuments({ "cardapio.itens.tags": "vegano" }));
