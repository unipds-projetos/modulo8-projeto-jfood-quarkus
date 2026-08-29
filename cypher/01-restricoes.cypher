// =============================================================================
// JFood — Etapa 11: restricoes e indices do grafo
//
//   docker exec -i jfood-neo4j cypher-shell -u neo4j -p neo4jpassword \
//     < cypher/01-restricoes.cypher
//
// O grafo REFERENCIA os outros bancos; nao os duplica. Cliente.id vem do
// PostgreSQL, Restaurante.id vem do MongoDB. Por isso a constraint de unicidade
// e sobre o id de origem: ela e o que impede o batch de criar um segundo no
// para o mesmo cliente.
// =============================================================================

CREATE CONSTRAINT cliente_id_unico IF NOT EXISTS
FOR (c:Cliente) REQUIRE c.id IS UNIQUE;

CREATE CONSTRAINT restaurante_id_unico IF NOT EXISTS
FOR (r:Restaurante) REQUIRE r.id IS UNIQUE;

CREATE CONSTRAINT categoria_nome_unico IF NOT EXISTS
FOR (cat:Categoria) REQUIRE cat.nome IS UNIQUE;

CREATE CONSTRAINT prato_id_unico IF NOT EXISTS
FOR (p:Prato) REQUIRE p.id IS UNIQUE;
