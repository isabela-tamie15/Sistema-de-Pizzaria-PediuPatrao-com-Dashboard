package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.EntregaRetiradaDTO;
import com.umc.pediupatrao.dto.ProdutoVendidoDTO;
import com.umc.pediupatrao.dto.ResumoVendasDTO;
import com.umc.pediupatrao.dto.StatusQuantidadeDTO;
import com.umc.pediupatrao.dto.VendaDiaSemanaDTO;
import com.umc.pediupatrao.dto.VendaPorHoraDTO;
import com.umc.pediupatrao.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// API REST do Dashboard Administrativo (RF06)
// Todos os endpoints são GET (só consultam) e devolvem JSON
// Acesso: ADMIN e GERENTE (SecurityConfig: /api/dashboard/** + @PreAuthorize no DashboardService)
//
// Filtros comuns (RF05), todos opcionais:
//   dataInicio e dataFim no formato AAAA-MM-DD (ex: 2026-11-01)
//   Se não forem informados, o período é dos últimos 30 dias
@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    @Autowired
    private DashboardService dashboardService;

    // RF01 - indicadores gerais: pedidos realizados, pedidos cancelados, faturamento e ticket médio
    // Exemplo: GET /api/dashboard/resumo?dataInicio=2026-11-01&dataFim=2026-11-09
    @GetMapping("/resumo")
    public ResumoVendasDTO resumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return dashboardService.resumo(dataInicio, dataFim);
    }

    // RF02 - pedidos e faturamento em cada dia da semana (segunda a domingo)
    @GetMapping("/vendas-por-dia-da-semana")
    public List<VendaDiaSemanaDTO> vendasPorDiaDaSemana(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return dashboardService.vendasPorDiaDaSemana(dataInicio, dataFim);
    }

    // RF03 - quantidade de pedidos em cada status
    @GetMapping("/pedidos-por-status")
    public List<StatusQuantidadeDTO> pedidosPorStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return dashboardService.pedidosPorStatus(dataInicio, dataFim);
    }

    // RF02 - vendas por horário do dia (0 a 23), para ver os horários de maior movimento
    @GetMapping("/vendas-por-hora")
    public List<VendaPorHoraDTO> vendasPorHora(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return dashboardService.vendasPorHora(dataInicio, dataFim);
    }

    // RF03 - quantos pedidos foram entregues e quantos foram retirados
    @GetMapping("/entrega-retirada")
    public List<EntregaRetiradaDTO> entregaRetirada(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return dashboardService.entregaRetirada(dataInicio, dataFim);
    }

    // RF04 - ranking dos produtos mais vendidos
    // Exemplo: GET /api/dashboard/produtos-mais-vendidos?limite=5
    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoVendidoDTO> produtosMaisVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "5") int limite) {
        return dashboardService.produtosMaisVendidos(dataInicio, dataFim, limite);
    }
}
