#!/usr/bin/env python3
"""
JFood — Etapa 11: o pipeline que mantem o grafo atualizado.

    python3 scripts/sincronizar-grafo.py | \
      docker exec -i jfood-neo4j cypher-shell -u neo4j -p neo4jpassword

O grafo do JFood e um BANCO DERIVADO: nada nasce nele. Clientes e avaliacoes vem
do PostgreSQL; restaurantes, categorias e pratos vem do MongoDB. Este script le
os dois e emite o Cypher correspondente.

Ele usa MERGE, e nao CREATE: rodar duas vezes produz o mesmo grafo que rodar uma.
Sem isso, um batch reexecutado depois de uma falha duplicaria tudo.

Os ids sao os das origens -- o grafo REFERENCIA, nao copia o dominio. Nome e
categoria vem junto so para tornar o Neo4J Browser legivel; nenhuma decisao de
negocio e tomada a partir deles.
"""

import json
import subprocess
import sys


def postgres(sql):
    saida = subprocess.run(
        ["docker", "exec", "-i", "jfood-postgres", "psql", "-U", "postgres",
         "-d", "jfood-db", "-t", "-A", "-F", "\t", "-c", sql],
        capture_output=True, text=True, check=True).stdout
    return [linha.split("\t") for linha in saida.strip().splitlines() if linha.strip()]


def mongo(js):
    saida = subprocess.run(
        ["docker", "exec", "jfood-mongodb", "mongosh", "--quiet",
         "-u", "admin", "-p", "adminpassword", "--authenticationDatabase", "admin",
         "jfood_catalogo", "--eval", js],
        capture_output=True, text=True, check=True).stdout
    return json.loads(saida)


def texto(valor):
    return "'" + (valor or "").replace("\\", "\\\\").replace("'", "\\'") + "'"


def main():
    saida = sys.stdout
    print("// gerado por scripts/sincronizar-grafo.py -- nao edite a mao", file=saida)

    # ---- MongoDB: categorias, restaurantes e pratos --------------------------
    restaurantes = mongo(
        "JSON.stringify(db.restaurantes.find({}, "
        "{nome:1, categoria:1, nota_media:1, taxa_entrega:1, 'cardapio.itens.item_id':1, "
        " 'cardapio.itens.nome':1, 'cardapio.itens.tags':1}).toArray())")

    categorias = sorted({r["categoria"] for r in restaurantes})
    for categoria in categorias:
        print(f"MERGE (:Categoria {{nome: {texto(categoria)}}});", file=saida)

    for r in restaurantes:
        rid = r["_id"]["$oid"] if isinstance(r["_id"], dict) else str(r["_id"])
        print(
            f"MERGE (r:Restaurante {{id: {texto(rid)}}}) "
            f"SET r.nome = {texto(r['nome'])};", file=saida)
        print(
            f"MATCH (r:Restaurante {{id: {texto(rid)}}}), "
            f"(c:Categoria {{nome: {texto(r['categoria'])}}}) "
            f"MERGE (r)-[:DO_TIPO]->(c);", file=saida)

        for secao in r.get("cardapio", []):
            for item in secao.get("itens", []):
                item_id = item.get("item_id")
                if not item_id:
                    continue
                pid = item_id["$oid"] if isinstance(item_id, dict) else str(item_id)
                tags = "[" + ", ".join(texto(t) for t in item.get("tags", [])) + "]"
                print(
                    f"MERGE (p:Prato {{id: {texto(pid)}}}) "
                    f"SET p.nome = {texto(item['nome'])}, p.tags = {tags};", file=saida)
                print(
                    f"MATCH (p:Prato {{id: {texto(pid)}}}), "
                    f"(r:Restaurante {{id: {texto(rid)}}}) "
                    f"MERGE (r)-[:SERVE]->(p);", file=saida)

    # ---- PostgreSQL: clientes -----------------------------------------------
    for cliente_id, nome in postgres(
            "SELECT c.usuario_id, u.nome FROM cliente c JOIN usuario u ON u.id = c.usuario_id"):
        print(f"MERGE (c:Cliente {{id: {cliente_id}}}) SET c.nome = {texto(nome)};", file=saida)

    # ---- PostgreSQL: PEDIU {vezes} ------------------------------------------
    # O nome do restaurante e a ponte entre os dois bancos: no PostgreSQL ele e
    # dado de negocio, no MongoDB e a chave do documento. Um pipeline de
    # producao usaria um id compartilhado, definido na criacao do restaurante.
    nome_para_id = {}
    for r in restaurantes:
        rid = r["_id"]["$oid"] if isinstance(r["_id"], dict) else str(r["_id"])
        nome_para_id[r["nome"]] = rid

    for cliente_id, nome_restaurante, vezes in postgres(
            "SELECT p.cliente_id, r.nome, COUNT(*) "
            "FROM pedido p JOIN restaurante r ON r.id = p.restaurante_id "
            "WHERE p.status <> 'CANCELADO' GROUP BY p.cliente_id, r.nome"):
        rid = nome_para_id.get(nome_restaurante)
        if not rid:
            continue
        print(
            f"MATCH (c:Cliente {{id: {cliente_id}}}), (r:Restaurante {{id: {texto(rid)}}}) "
            f"MERGE (c)-[pe:PEDIU]->(r) SET pe.vezes = {vezes};", file=saida)

    # ---- PostgreSQL: AVALIOU {nota, data} -----------------------------------
    for cliente_id, nome_restaurante, nota, data in postgres(
            "SELECT a.cliente_id, r.nome, a.nota, a.criado_em::date "
            "FROM avaliacao a JOIN restaurante r ON r.id = a.restaurante_id"):
        rid = nome_para_id.get(nome_restaurante)
        if not rid:
            continue
        print(
            f"MATCH (c:Cliente {{id: {cliente_id}}}), (r:Restaurante {{id: {texto(rid)}}}) "
            f"MERGE (c)-[av:AVALIOU]->(r) SET av.nota = {nota}, av.data = date({texto(data)});",
            file=saida)


if __name__ == "__main__":
    main()
