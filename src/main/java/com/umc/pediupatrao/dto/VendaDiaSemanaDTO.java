package com.umc.pediupatrao.dto;

// RF02: uma barra do gráfico de pedidos por dia da semana (GET /api/dashboard/vendas-por-dia-da-semana)
// diaSemana vai de "Segunda" a "Domingo"
public record VendaDiaSemanaDTO(
        String diaSemana,
        long quantidadePedidos,
        double faturamento
) {
}
