// =============================================================================
// JFood — Etapa 11: enriquecimento de DEMONSTRACAO
//
//   docker exec -i jfood-neo4j cypher-shell -u neo4j -p neo4jpassword \
//     < cypher/02-demo-avaliacoes-e-vinculos.cypher
//
// ATENCAO: isto NAO faz parte do pipeline. O grafo real vem de
// scripts/sincronizar-grafo.py, que le o PostgreSQL e o MongoDB.
//
// O seed da Etapa 2 tem 12 avaliacoes entre 10 clientes -- suficiente para o
// esquema, insuficiente para uma filtragem colaborativa mostrar do que e capaz.
// Este script acrescenta avaliacoes e vinculos DETERMINISTICOS para que a
// consulta de tres saltos tenha vizinhanca de verdade.
//
// Tudo com MERGE: rodar duas vezes nao duplica nada.
// =============================================================================

// -----------------------------------------------------------------------------
// Mais avaliacoes -- a vizinhanca da Ana (cliente 10)
// -----------------------------------------------------------------------------

UNWIND [
  {cliente: 11, restaurante: 'Sabor da Roça',    nota: 5},
  {cliente: 11, restaurante: 'Esfiha do Khalil', nota: 4},
  {cliente: 12, restaurante: 'Esfiha do Khalil', nota: 5},
  {cliente: 12, restaurante: 'Verde & Cia',      nota: 4},
  {cliente: 19, restaurante: 'Sabor da Roça',    nota: 5},
  {cliente: 19, restaurante: 'Verde & Cia',      nota: 5},
  {cliente: 16, restaurante: 'Sushi Yuki',       nota: 4},
  {cliente: 16, restaurante: 'Esfiha do Khalil', nota: 5},
  {cliente: 13, restaurante: 'Cantina do Zé',    nota: 4},
  {cliente: 13, restaurante: 'Forno & Pizza',    nota: 5},
  {cliente: 14, restaurante: 'Cantina do Zé',    nota: 5},
  {cliente: 14, restaurante: 'Forno & Pizza',    nota: 4},
  {cliente: 15, restaurante: 'Sushi Yuki',       nota: 5},
  {cliente: 15, restaurante: 'Verde & Cia',      nota: 5},
  {cliente: 17, restaurante: 'Pizzaria Bella Napoli', nota: 5},
  {cliente: 18, restaurante: 'Cantina do Zé',    nota: 4},
  {cliente: 18, restaurante: 'Sabor da Roça',    nota: 3}
] AS av
MATCH (c:Cliente {id: av.cliente}), (r:Restaurante {nome: av.restaurante})
MERGE (c)-[a:AVALIOU]->(r)
  ON CREATE SET a.nota = av.nota, a.data = date('2026-08-01');

// -----------------------------------------------------------------------------
// SEGUE -- a rede social entre clientes
// -----------------------------------------------------------------------------

UNWIND [[10, 12], [10, 16], [11, 10], [12, 19], [13, 14], [16, 15], [19, 11]] AS par
MATCH (a:Cliente {id: par[0]}), (b:Cliente {id: par[1]})
MERGE (a)-[:SEGUE]->(b);

// -----------------------------------------------------------------------------
// Vinculos de risco -- para a consulta de deteccao de fraude
//
// Cartao e dispositivo viram NOS, e nao propriedades. E a decisao que torna a
// pergunta respondivel: "quais contas compartilham o mesmo cartao" e uma
// travessia de dois saltos, e nao um GROUP BY sobre uma coluna.
//
// Os clientes 13, 14 e 18 formam um componente conectado suspeito.
// -----------------------------------------------------------------------------

UNWIND [
  {cliente: 10, cartao: 'tok_aaa111', dispositivo: 'dev-ana-iphone'},
  {cliente: 11, cartao: 'tok_bbb222', dispositivo: 'dev-bruno-android'},
  {cliente: 12, cartao: 'tok_ccc333', dispositivo: 'dev-carla-iphone'},
  {cliente: 13, cartao: 'tok_zzz999', dispositivo: 'dev-suspeito-01'},
  {cliente: 14, cartao: 'tok_zzz999', dispositivo: 'dev-suspeito-01'},
  {cliente: 18, cartao: 'tok_zzz999', dispositivo: 'dev-suspeito-02'},
  {cliente: 15, cartao: 'tok_ddd444', dispositivo: 'dev-elisa-android'},
  {cliente: 16, cartao: 'tok_eee555', dispositivo: 'dev-fabio-iphone'},
  {cliente: 17, cartao: 'tok_fff666', dispositivo: 'dev-gabriela-iphone'},
  {cliente: 19, cartao: 'tok_ggg777', dispositivo: 'dev-isabela-android'}
] AS v
MATCH (c:Cliente {id: v.cliente})
MERGE (cartao:Cartao {token: v.cartao})
MERGE (disp:Dispositivo {id: v.dispositivo})
MERGE (c)-[:PAGA_COM]->(cartao)
MERGE (c)-[:ACESSA_DE]->(disp);
