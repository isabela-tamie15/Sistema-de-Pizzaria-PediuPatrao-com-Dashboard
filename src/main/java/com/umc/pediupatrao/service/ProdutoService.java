package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    // PARTE 2: 2ª camada de segurança (a 1ª são as regras por URL no SecurityConfig)
    // ADMIN e GERENTE mantêm o cardápio, porque o preço do produto define o valor dos pedidos
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public Produto novoProduto(Produto produto) {
        return produtoRepository.save(produto);
    }

    // Qualquer usuário logado consulta o cardápio (o ATENDENTE precisa dele para registrar pedidos)
    @PreAuthorize("isAuthenticated()")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    // Método para excluir produto (antes o comentário dizia "cliente", corrigido na Parte 2)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public void excluir(String id) {
        produtoRepository.deleteById(id);
    }


    // Método para salvar um novo produto ou atualizar um produto existente (antes o comentário dizia "cliente")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public Produto salvar(Produto produto) {
        // Se o cliente não tem ID (novo cliente), salva como novo
        if (produto.getId() == null) {
            return produtoRepository.save(produto);  // Cria um novo cliente
        } // Se já tem ID (cliente existente), atualiza
        else {
            // Verifica se o cliente existe antes de atualizar
            if (produtoRepository.existsById(produto.getId())) {
                return produtoRepository.save(produto);  // Atualiza o cliente existente
            } else {
                throw new IllegalArgumentException("Cliente não encontrado para atualização.");
            }
        }
    }
}
