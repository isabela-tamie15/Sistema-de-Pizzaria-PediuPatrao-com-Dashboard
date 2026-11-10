package com.umc.pediupatrao.dto;

// RF03: uma fatia do gráfico Entrega x Retirada (GET /api/dashboard/entrega-retirada)
// tipo é "Entrega" ou "Retirada"
public record EntregaRetiradaDTO(
        String tipo,
        long quantidade
) {
}
