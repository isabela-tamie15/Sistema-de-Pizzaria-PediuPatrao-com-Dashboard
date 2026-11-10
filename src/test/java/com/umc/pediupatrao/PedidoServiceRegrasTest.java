package com.umc.pediupatrao;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraDeNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.PedidoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Testes de unidade das regras de negócio do PedidoService (Parte 2)
// @Mock cria repositórios "de mentira", então os testes rodam sem conectar no MongoDB
// @InjectMocks cria o PedidoService de verdade e coloca os mocks dentro dele
@ExtendWith(MockitoExtension.class)
class PedidoServiceRegrasTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PedidoService pedidoService;

    @BeforeEach
    void prepararUsuarioLogado() {
        // Simula o GERENTE logado no Spring Security
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("gerente", null, "ROLE_GERENTE"));

        // save() devolve o próprio pedido recebido, como o MongoDB faria
        lenient().when(pedidoRepository.save(any(Pedido.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @AfterEach
    void limparUsuarioLogado() {
        SecurityContextHolder.clearContext();
    }

    // ===== Correção 1: preço vem do cadastro de produtos =====

    @Test
    void criarPedidoUsaPrecoDoCadastroEIgnoraPrecoDoFormulario() {
        when(produtoRepository.findFirstByNome("Pizza Calabresa"))
                .thenReturn(Optional.of(produto("Pizza Calabresa", 50.0, true)));

        // O formulário tentou mandar o preço de R$ 1,00
        Pedido pedido = pedidoComItem("Pizza Calabresa", 2, 1.0);

        Pedido salvo = pedidoService.criarPedido(pedido);

        assertEquals(50.0, salvo.getItens().get(0).getPrecoUnitario());
        assertEquals(100.0, salvo.getValorBruto());
        assertEquals(100.0, salvo.getValorTotal());
        assertEquals(StatusPedido.RECEBIDO, salvo.getStatus());
        assertEquals("gerente", salvo.getResponsavelPeloRegistro());
        assertNotNull(salvo.getDataPedido());
    }

    @Test
    void criarPedidoRecusaProdutoQueNaoExisteNoCadastro() {
        when(produtoRepository.findFirstByNome("Produto Inventado")).thenReturn(Optional.empty());

        Pedido pedido = pedidoComItem("Produto Inventado", 1, 10.0);

        assertThrows(RegraDeNegocioException.class, () -> pedidoService.criarPedido(pedido));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void criarPedidoRecusaProdutoInativo() {
        when(produtoRepository.findFirstByNome("Esfiha"))
                .thenReturn(Optional.of(produto("Esfiha", 8.0, false)));

        assertThrows(RegraDeNegocioException.class,
                () -> pedidoService.criarPedido(pedidoComItem("Esfiha", 1, 8.0)));
    }

    @Test
    void criarPedidoRecusaQuantidadeZero() {
        assertThrows(RegraDeNegocioException.class,
                () -> pedidoService.criarPedido(pedidoComItem("Pizza Calabresa", 0, 50.0)));
    }

    // ===== Correção 2: desconto único, até 20% e antes da saída =====

    @Test
    void descontoCalculadoSobreOValorBruto() {
        Pedido pedido = pedidoSalvo(StatusPedido.EM_PREPARACAO, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        Pedido resultado = pedidoService.aplicarDesconto("p1", 10.0);

        assertEquals(10.0, resultado.getDesconto());
        assertEquals(10.0, resultado.getValorDesconto());
        assertEquals(90.0, resultado.getValorTotal());
    }

    @Test
    void novoDescontoSubstituiOAnteriorENaoAcumula() {
        Pedido pedido = pedidoSalvo(StatusPedido.EM_PREPARACAO, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        // 1º desconto: 20% de R$ 100,00 → R$ 80,00
        pedidoService.aplicarDesconto("p1", 20.0);
        // 2º desconto: 20% de novo. Se acumulasse, iria para R$ 64,00 (36% no total)
        Pedido resultado = pedidoService.aplicarDesconto("p1", 20.0);

        assertEquals(20.0, resultado.getDesconto());
        assertEquals(80.0, resultado.getValorTotal());
    }

    @Test
    void trocarDescontoRecalculaSobreOValorDosItens() {
        Pedido pedido = pedidoSalvo(StatusPedido.RECEBIDO, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        pedidoService.aplicarDesconto("p1", 10.0);
        Pedido resultado = pedidoService.aplicarDesconto("p1", 15.0);

        // 15% sobre os R$ 100,00 originais, e não sobre os R$ 90,00 que já tinham desconto
        assertEquals(15.0, resultado.getDesconto());
        assertEquals(85.0, resultado.getValorTotal());
    }

    @Test
    void pedidoAntigoSemValorBrutoTemDescontoAnteriorDesfeito() {
        // Pedido criado na Parte 1: só tem valorTotal (R$ 90,00) e desconto de 10%
        Pedido pedido = new Pedido();
        pedido.setId("p1");
        pedido.setStatus(StatusPedido.PRONTO);
        pedido.setValorTotal(90.0);
        pedido.setDesconto(10.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        Pedido resultado = pedidoService.aplicarDesconto("p1", 20.0);

        // Valor original = 90 / 0,90 = R$ 100,00 → com 20% fica R$ 80,00
        assertEquals(100.0, resultado.getValorBruto());
        assertEquals(80.0, resultado.getValorTotal());
    }

    @Test
    void descontoBloqueadoDepoisQueOPedidoSaiu() {
        Pedido pedido = pedidoSalvo(StatusPedido.SAIU_PARA_ENTREGA, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        assertThrows(RegraDeNegocioException.class, () -> pedidoService.aplicarDesconto("p1", 10.0));
    }

    @Test
    void descontoAcimaDeVintePorCentoERecusado() {
        Pedido pedido = pedidoSalvo(StatusPedido.RECEBIDO, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        assertThrows(RegraDeNegocioException.class, () -> pedidoService.aplicarDesconto("p1", 21.0));
    }

    // ===== Correção 3: responsável e horário de saída gravados no pedido =====

    @Test
    void saidaParaEntregaGravaResponsavelEHorarioNoPedido() {
        Pedido pedido = pedidoSalvo(StatusPedido.PRONTO, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        Pedido resultado = pedidoService.atualizarStatus("p1", StatusPedido.SAIU_PARA_ENTREGA);

        assertEquals(StatusPedido.SAIU_PARA_ENTREGA, resultado.getStatus());
        assertEquals("gerente", resultado.getResponsavelSaida());
        assertNotNull(resultado.getDataHoraSaida());
        // Dashboard: o tipo de saída fica guardado para o gráfico Entrega x Retirada
        assertEquals(StatusPedido.SAIU_PARA_ENTREGA, resultado.getTipoSaida());
    }

    // ===== Regras da Parte 1 que continuam valendo =====

    @Test
    void transicaoInvalidaERecusada() {
        Pedido pedido = pedidoSalvo(StatusPedido.EM_PREPARACAO, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        // Finalizar um pedido que ainda está em preparação não é permitido
        assertThrows(RegraDeNegocioException.class,
                () -> pedidoService.atualizarStatus("p1", StatusPedido.FINALIZADO));
    }

    @Test
    void cancelamentoSemJustificativaERecusado() {
        Pedido pedido = pedidoSalvo(StatusPedido.RECEBIDO, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        assertThrows(RegraDeNegocioException.class, () -> pedidoService.cancelarPedido("p1", "  "));
    }

    @Test
    void cancelamentoDepoisDaRetiradaERecusado() {
        Pedido pedido = pedidoSalvo(StatusPedido.RETIRADO, 100.0);
        when(pedidoRepository.findById("p1")).thenReturn(Optional.of(pedido));

        assertThrows(RegraDeNegocioException.class,
                () -> pedidoService.cancelarPedido("p1", "Cliente desistiu"));
    }

    // ===== Métodos auxiliares para montar os dados dos testes =====

    private Produto produto(String nome, Double preco, Boolean ativo) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(ativo);
        return produto;
    }

    private Pedido pedidoComItem(String nomeProduto, int quantidade, double precoDoFormulario) {
        ItemPedido item = new ItemPedido();
        item.setNomeProduto(nomeProduto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(precoDoFormulario);

        Pedido pedido = new Pedido();
        pedido.setNomeCliente("Maria");
        pedido.setItens(new java.util.ArrayList<>(List.of(item)));
        return pedido;
    }

    private Pedido pedidoSalvo(StatusPedido status, Double valor) {
        Pedido pedido = new Pedido();
        pedido.setId("p1");
        pedido.setStatus(status);
        pedido.setValorBruto(valor);
        pedido.setValorTotal(valor);
        return pedido;
    }
}
