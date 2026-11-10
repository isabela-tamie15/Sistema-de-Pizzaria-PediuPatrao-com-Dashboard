package com.umc.pediupatrao.dto;

// RF02: um ponto do gráfico de vendas por horário (GET /api/dashboard/vendas-por-hora)
// hora vai de 0 a 23 (ex: 19 = das 19:00 às 19:59)
public record VendaPorHoraDTO(
        int hora,
        long quantidadePedidos,
        double faturamento
) {
}
