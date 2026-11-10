package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Produto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProdutoRepository extends MongoRepository<Produto, String> {

    // PARTE 2: busca o produto pelo nome para o PedidoService pegar o preço oficial do cadastro
    // O formulário de pedido envia o nome do produto, e o preço passa a vir daqui, não do formulário
    // Optional porque o nome enviado pode não existir no cadastro (produto apagado ou nome alterado na requisição)
    Optional<Produto> findFirstByNome(String nome);
}
