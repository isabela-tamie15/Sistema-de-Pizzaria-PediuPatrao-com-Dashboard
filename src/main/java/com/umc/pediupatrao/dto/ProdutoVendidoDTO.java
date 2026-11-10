package com.umc.pediupatrao.dto;

// RF04: uma linha do ranking de produtos mais vendidos (GET /api/dashboard/produtos-mais-vendidos)
// faturamento aqui é o valor dos itens (quantidade x preço), antes do desconto do pedido
public record ProdutoVendidoDTO(
        String produto,
        long quantidade,
        double faturamento
) {
}
