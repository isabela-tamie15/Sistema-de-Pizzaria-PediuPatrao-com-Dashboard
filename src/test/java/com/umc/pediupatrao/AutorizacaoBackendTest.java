package com.umc.pediupatrao;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.controller.ClienteController;
import com.umc.pediupatrao.controller.PedidoController;
import com.umc.pediupatrao.controller.ProdutoController;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// PARTE 2: testes que simulam requisições HTTP com cada perfil, para provar que as permissões
// estão no BACKEND e não só escondendo botões na tela (exigência da "condição de validação" do enunciado)
// @WebMvcTest sobe só a camada web (controllers + Spring Security), sem MongoDB
// @MockitoBean substitui os services por versões "de mentira", então nada é gravado no banco
// @WithMockUser simula um usuário logado com o perfil informado
@WebMvcTest(controllers = {PedidoController.class, ClienteController.class, ProdutoController.class})
@Import(SecurityConfig.class)
class AutorizacaoBackendTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private ProdutoService produtoService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    // ===== Sem login =====

    @Test
    void apiSemLoginRecebe401() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void telaSemLoginRedirecionaParaOLogin() throws Exception {
        // accept(TEXT_HTML) simula o navegador pedindo uma página (e não uma API)
        mockMvc.perform(get("/pedidos").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ===== Desconto e cancelamento: exclusivos do GERENTE (RF04) =====

    @Test
    @WithMockUser(username = "atendente", roles = "ATENDENTE")
    void atendenteNaoPodeAplicarDesconto() throws Exception {
        mockMvc.perform(post("/pedidos/p1/desconto").param("percentualDesconto", "10").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pedidoService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminNaoPodeAplicarDesconto() throws Exception {
        mockMvc.perform(post("/pedidos/p1/desconto").param("percentualDesconto", "10").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pedidoService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminNaoPodeCancelarPedido() throws Exception {
        mockMvc.perform(post("/pedidos/p1/cancelar").param("justificativa", "Teste").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pedidoService);
    }

    @Test
    @WithMockUser(username = "gerente", roles = "GERENTE")
    void gerentePodeAplicarDesconto() throws Exception {
        mockMvc.perform(post("/pedidos/p1/desconto").param("percentualDesconto", "10").with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(pedidoService).aplicarDesconto("p1", 10.0);
    }

    // ===== CSRF =====

    @Test
    @WithMockUser(username = "gerente", roles = "GERENTE")
    void requisicaoSemTokenCsrfERecusada() throws Exception {
        // Mesmo sendo GERENTE, sem o token CSRF a requisição é bloqueada
        mockMvc.perform(post("/pedidos/p1/desconto").param("percentualDesconto", "10"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pedidoService);
    }

    @Test
    @WithMockUser(username = "atendente", roles = "ATENDENTE")
    void apiSemTokenCsrfERecusada() throws Exception {
        // Na Parte 1 o CSRF era ignorado em /api/**, então esta chamada passava
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Maria\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(clienteService);
    }

    // ===== Permissões da API por perfil =====

    @Test
    @WithMockUser(username = "atendente", roles = "ATENDENTE")
    void atendenteNaoPodeExcluirClientePelaApi() throws Exception {
        mockMvc.perform(delete("/api/clientes/c1").with(csrf()))
                .andExpect(status().isForbidden());

        verify(clienteService, never()).excluir(anyString());
    }

    @Test
    @WithMockUser(username = "atendente", roles = "ATENDENTE")
    void atendenteNaoPodeCadastrarProdutoPelaApi() throws Exception {
        mockMvc.perform(post("/api/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Pizza\", \"preco\": 1.0}")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(produtoService, never()).salvar(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminNaoPodeCriarPedido() throws Exception {
        mockMvc.perform(post("/pedidos/salvar").param("nomeCliente", "Maria").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pedidoService);
    }
}
