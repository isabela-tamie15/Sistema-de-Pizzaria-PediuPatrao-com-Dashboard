package com.umc.pediupatrao.dto;

// RF03: quantidade de pedidos em cada status (GET /api/dashboard/pedidos-por-status)
// status: valor do enum (ex: EM_PREPARACAO) | descricao: texto para a tela (ex: Em preparação)
public record StatusQuantidadeDTO(
        String status,
        String descricao,
        long quantidade
) {
}
