package com.umc.pediupatrao.dto;

import java.time.LocalDate;

// RF01: indicadores gerais de vendas do período, devolvidos por GET /api/dashboard/resumo
// record é uma classe Java só de dados: o Java cria construtor, getters e toString sozinho
// O Spring transforma cada campo em uma chave do JSON de resposta
public record ResumoVendasDTO(
        LocalDate dataInicio,
        LocalDate dataFim,
        long pedidosRealizados,  // pedidos que não foram cancelados
        long pedidosCancelados,  // pedidos com status CANCELADO
        double faturamento,      // soma do valor final dos pedidos realizados (já com desconto)
        double ticketMedio       // faturamento dividido pela quantidade de pedidos realizados
) {
}
