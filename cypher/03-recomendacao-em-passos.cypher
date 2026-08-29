// =============================================================================
// JFood — Etapa 11: a consulta matadora, construida em passos
//
// Rode um bloco de cada vez no Neo4J Browser (http://localhost:7474) e veja o
// grafo crescer. E para isso que o Browser existe: em grafo, a visualizacao nao
// e enfeite -- e a ferramenta de diagnostico.
// =============================================================================

// --- Passo 1: ache a Ana ------------------------------------------------------
MATCH (ana:Cliente {nome: 'Ana Lima'})
RETURN ana;

// --- Passo 2: os restaurantes que ela avaliou com nota >= 4 -------------------
MATCH (ana:Cliente {nome: 'Ana Lima'})-[a:AVALIOU]->(r:Restaurante)
WHERE a.nota >= 4
RETURN ana, a, r;

// --- Passo 3: os outros clientes que tambem avaliaram bem esses restaurantes --
MATCH (ana:Cliente {nome: 'Ana Lima'})-[a1:AVALIOU]->(r:Restaurante)
      <-[a2:AVALIOU]-(vizinho:Cliente)
WHERE a1.nota >= 4 AND a2.nota >= 4 AND vizinho <> ana
RETURN ana, a1, r, a2, vizinho;

// --- Passo 4: o que ESSES clientes avaliaram bem ------------------------------
MATCH (ana:Cliente {nome: 'Ana Lima'})-[a1:AVALIOU]->(:Restaurante)
      <-[a2:AVALIOU]-(vizinho:Cliente)-[a3:AVALIOU]->(sugestao:Restaurante)
WHERE a1.nota >= 4 AND a2.nota >= 4 AND a3.nota >= 4 AND vizinho <> ana
RETURN sugestao.nome AS restaurante, count(DISTINCT vizinho) AS vizinhos
ORDER BY vizinhos DESC;

// --- Passo 5: exclua o que a Ana ja conhece -----------------------------------
// O WHERE NOT e uma clausula so. Em SQL, este mesmo filtro seria dois NOT EXISTS
// com subqueries correlacionadas.
MATCH (ana:Cliente {nome: 'Ana Lima'})-[a1:AVALIOU]->(:Restaurante)
      <-[a2:AVALIOU]-(vizinho:Cliente)-[a3:AVALIOU]->(sugestao:Restaurante)
WHERE a1.nota >= 4 AND a2.nota >= 4 AND a3.nota >= 4
  AND vizinho <> ana
  AND NOT (ana)-[:AVALIOU]->(sugestao)
  AND NOT (ana)-[:PEDIU]->(sugestao)
RETURN sugestao.nome AS restaurante, count(DISTINCT vizinho) AS forcaRecomendacao
ORDER BY forcaRecomendacao DESC
LIMIT 5;

// --- Passo 6: o ranking enriquecido -------------------------------------------
// Em vez de CONTAR vizinhos, soma as notas ponderadas -- e desempata por
// proximidade de categoria com o que a Ana ja gosta.
MATCH (ana:Cliente {nome: 'Ana Lima'})-[a1:AVALIOU]->(base:Restaurante)
      <-[a2:AVALIOU]-(vizinho:Cliente)-[a3:AVALIOU]->(sugestao:Restaurante)
WHERE a1.nota >= 4 AND a2.nota >= 4 AND a3.nota >= 4
  AND vizinho <> ana
  AND NOT (ana)-[:AVALIOU]->(sugestao)
  AND NOT (ana)-[:PEDIU]->(sugestao)
WITH sugestao,
     count(DISTINCT vizinho)                       AS vizinhos,
     sum((a2.nota - 3) * (a3.nota - 3))            AS pesoTotal,
     collect(DISTINCT base)                        AS bases
OPTIONAL MATCH (sugestao)-[:DO_TIPO]->(cat:Categoria)<-[:DO_TIPO]-(b:Restaurante)
WHERE b IN bases
WITH sugestao, vizinhos, pesoTotal, count(DISTINCT cat) AS categoriasEmComum
RETURN sugestao.nome AS restaurante, vizinhos, pesoTotal, categoriasEmComum
ORDER BY pesoTotal DESC, categoriasEmComum DESC, vizinhos DESC
LIMIT 5;

// --- Segunda consulta: deteccao de fraude -------------------------------------
// Contas diferentes que compartilham cartao ou dispositivo, formando um
// componente conectado suspeito. Em SQL seria um self-join sobre uma tabela de
// meios de pagamento; aqui e a forma natural da pergunta.
MATCH (c1:Cliente)-[:PAGA_COM|ACESSA_DE]->(vinculo)<-[:PAGA_COM|ACESSA_DE]-(c2:Cliente)
WHERE id(c1) < id(c2)
RETURN c1.nome AS conta_a,
       c2.nome AS conta_b,
       labels(vinculo)[0] AS tipo_de_vinculo,
       coalesce(vinculo.token, vinculo.id) AS vinculo
ORDER BY conta_a, conta_b;
