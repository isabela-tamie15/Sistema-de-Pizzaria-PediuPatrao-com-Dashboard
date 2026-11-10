package com.umc.pediupatrao;

import com.umc.pediupatrao.dto.EntregaRetiradaDTO;
import com.umc.pediupatrao.dto.ProdutoVendidoDTO;
import com.umc.pediupatrao.dto.ResumoVendasDTO;
import com.umc.pediupatrao.dto.StatusQuantidadeDTO;
import com.umc.pediupatrao.dto.VendaDiaSemanaDTO;
import com.umc.pediupatrao.dto.VendaPorHoraDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraDeNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Testes dos cálculos do Dashboard (RF01 a RF05 e RF07), sem MongoDB
// O PedidoRepository é um mock que devolve uma lista de pedidos montada aqui no teste
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final LocalDate DIA_1 = LocalDate.of(2026, 11, 1); // domingo
    private static final LocalDate DIA_3 = LocalDate.of(2026, 11, 3); // terça

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @BeforeEach
    void preparar() {
        // Pedidos de exemplo:
        // dia 1 (domingo): finalizado R$ 100 (2 Calabresa), que saiu para entrega + cancelado R$ 50 (1 Mussarela)
        // dia 3 (terça): em preparação R$ 90 com desconto de R$ 10 (1 Calabresa + 2 Refrigerante)
        List<Pedido> pedidos = new ArrayList<>();
        Pedido entregue = pedido("p1", DIA_1, StatusPedido.FINALIZADO, 100.0, 0.0,
                item("Pizza Calabresa", 2, 50.0));
        entregue.setTipoSaida(StatusPedido.SAIU_PARA_ENTREGA);
        pedidos.add(entregue);
        pedidos.add(pedido("p2", DIA_1, StatusPedido.CANCELADO, 50.0, 0.0,
                item("Pizza Mussarela", 1, 50.0)));
        pedidos.add(pedido("p3", DIA_3, StatusPedido.EM_PREPARACAO, 90.0, 10.0,
                item("Pizza Calabresa", 1, 50.0), item("Refrigerante", 2, 25.0)));

        lenient().when(pedidoRepository.buscarPorPeriodo(any(), any())).thenReturn(pedidos);
    }

    // ===== RF01 =====

    @Test
    void resumoCalculaIndicadoresIgnorandoCancelados() {
        ResumoVendasDTO resumo = dashboardService.resumo(DIA_1, DIA_3);

        assertEquals(2, resumo.pedidosRealizados());
        assertEquals(1, resumo.pedidosCancelados());
        assertEquals(190.0, resumo.faturamento()); // 100 + 90 (o cancelado de R$ 50 não entra)
        assertEquals(95.0, resumo.ticketMedio());  // 190 / 2 pedidos realizados
    }

    @Test
    void resumoSemPedidosDevolveZero() {
        when(pedidoRepository.buscarPorPeriodo(any(), any())).thenReturn(List.of());

        ResumoVendasDTO resumo = dashboardService.resumo(DIA_1, DIA_3);

        assertEquals(0, resumo.pedidosRealizados());
        assertEquals(0, resumo.pedidosCancelados());
        assertEquals(0.0, resumo.faturamento());
        assertEquals(0.0, resumo.ticketMedio()); // sem pedidos, não divide por zero
    }

    // ===== RF02 =====

    @Test
    void vendasPorDiaDaSemanaTrazOsSeteDias() {
        List<VendaDiaSemanaDTO> vendas = dashboardService.vendasPorDiaDaSemana(DIA_1, DIA_3);

        assertEquals(7, vendas.size());                      // de segunda a domingo
        assertEquals("Segunda", vendas.get(0).diaSemana());
        assertEquals(0, vendas.get(0).quantidadePedidos());  // segunda sem vendas
        assertEquals("Terça", vendas.get(1).diaSemana());
        assertEquals(90.0, vendas.get(1).faturamento());
        assertEquals("Domingo", vendas.get(6).diaSemana());
        assertEquals(1, vendas.get(6).quantidadePedidos());  // o cancelado de domingo não conta
        assertEquals(100.0, vendas.get(6).faturamento());
    }

    @Test
    void vendasPorHoraAgrupaPeloHorarioDeEntrada() {
        // Todos os pedidos de exemplo entraram às 19:00
        List<VendaPorHoraDTO> vendas = dashboardService.vendasPorHora(DIA_1, DIA_3);

        assertEquals(24, vendas.size());                   // uma posição para cada hora do dia
        assertEquals(19, vendas.get(19).hora());
        assertEquals(2, vendas.get(19).quantidadePedidos()); // o cancelado não conta
        assertEquals(190.0, vendas.get(19).faturamento());
        assertEquals(0, vendas.get(12).quantidadePedidos()); // meio-dia sem vendas
    }

    // ===== RF03 =====

    @Test
    void pedidosPorStatusTrazOsSeteStatus() {
        List<StatusQuantidadeDTO> status = dashboardService.pedidosPorStatus(DIA_1, DIA_3);

        assertEquals(7, status.size());
        assertEquals("RECEBIDO", status.get(0).status());
        assertEquals(0, status.get(0).quantidade());
        assertEquals(1, quantidadeDe(status, "CANCELADO"));
        assertEquals(1, quantidadeDe(status, "FINALIZADO"));
    }

    @Test
    void entregaRetiradaContaComoOPedidoSaiu() {
        // Lista própria: um pedido entregue, um pedido antigo (sem tipoSaida) que está RETIRADO
        // e um pedido que ainda está na cozinha
        Pedido entregue = pedido("p1", DIA_1, StatusPedido.FINALIZADO, 100.0, 0.0);
        entregue.setTipoSaida(StatusPedido.SAIU_PARA_ENTREGA);
        Pedido retiradoAntigo = pedido("p6", DIA_1, StatusPedido.RETIRADO, 40.0, 0.0);
        Pedido aindaNaCozinha = pedido("p7", DIA_1, StatusPedido.EM_PREPARACAO, 60.0, 0.0);
        when(pedidoRepository.buscarPorPeriodo(any(), any()))
                .thenReturn(List.of(entregue, retiradoAntigo, aindaNaCozinha));

        List<EntregaRetiradaDTO> saidas = dashboardService.entregaRetirada(DIA_1, DIA_1);

        assertEquals("Entrega", saidas.get(0).tipo());
        assertEquals(1, saidas.get(0).quantidade());
        assertEquals("Retirada", saidas.get(1).tipo());
        assertEquals(1, saidas.get(1).quantidade()); // o pedido em preparação ainda não saiu
    }

    // ===== RF04 =====

    @Test
    void produtosMaisVendidosSomaQuantidadesSemCancelados() {
        List<ProdutoVendidoDTO> ranking = dashboardService.produtosMaisVendidos(DIA_1, DIA_3, 5);

        // Calabresa: 2 + 1 = 3 | Refrigerante: 2 | Mussarela só aparece no cancelado, então fica fora
        assertEquals(2, ranking.size());
        assertEquals("Pizza Calabresa", ranking.get(0).produto());
        assertEquals(3, ranking.get(0).quantidade());
        assertEquals(150.0, ranking.get(0).faturamento());
        assertEquals("Refrigerante", ranking.get(1).produto());
    }

    // ===== RF05 - validação dos filtros =====

    @Test
    void dataInicialDepoisDaFinalERecusada() {
        assertThrows(RegraDeNegocioException.class, () -> dashboardService.resumo(DIA_3, DIA_1));
    }

    @Test
    void periodoMaiorQueUmAnoERecusado() {
        assertThrows(RegraDeNegocioException.class,
                () -> dashboardService.resumo(DIA_1.minusYears(2), DIA_1));
    }

    @Test
    void limiteForaDaFaixaERecusado() {
        assertThrows(RegraDeNegocioException.class,
                () -> dashboardService.produtosMaisVendidos(DIA_1, DIA_3, 0));
        assertThrows(RegraDeNegocioException.class,
                () -> dashboardService.produtosMaisVendidos(DIA_1, DIA_3, 51));
    }

    @Test
    void semDatasUsaOsUltimosTrintaDias() {
        ResumoVendasDTO resumo = dashboardService.resumo(null, null);

        assertEquals(LocalDate.now(), resumo.dataFim());
        assertEquals(LocalDate.now().minusDays(29), resumo.dataInicio());
    }

    // ===== RF07 - tratamento de dados incompletos =====

    @Test
    void pedidoSemValorOuSemDataNaoQuebraOCalculo() {
        Pedido semValor = pedido("p4", DIA_1, StatusPedido.RECEBIDO, null, null);
        Pedido semData = pedido("p5", DIA_1, StatusPedido.RECEBIDO, 30.0, 0.0);
        semData.setDataPedido(null);
        when(pedidoRepository.buscarPorPeriodo(any(), any())).thenReturn(List.of(semValor, semData));

        ResumoVendasDTO resumo = dashboardService.resumo(DIA_1, DIA_1);

        assertEquals(1, resumo.pedidosRealizados()); // o pedido sem data é descartado
        assertEquals(0.0, resumo.faturamento());  // valor vazio vira zero
    }

    // ===== Métodos auxiliares =====

    private long quantidadeDe(List<StatusQuantidadeDTO> lista, String status) {
        return lista.stream().filter(s -> s.status().equals(status)).findFirst().orElseThrow().quantidade();
    }

    private Pedido pedido(String id, LocalDate dia, StatusPedido status, Double total, Double desconto,
                          ItemPedido... itens) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setNomeCliente("Cliente " + id);
        pedido.setDataPedido(LocalDateTime.of(dia, java.time.LocalTime.of(19, 0)));
        pedido.setStatus(status);
        pedido.setValorTotal(total);
        pedido.setValorDesconto(desconto);
        pedido.setItens(new ArrayList<>(List.of(itens)));
        return pedido;
    }

    private ItemPedido item(String nome, int quantidade, double preco) {
        ItemPedido item = new ItemPedido();
        item.setNomeProduto(nome);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(preco);
        return item;
    }
}
