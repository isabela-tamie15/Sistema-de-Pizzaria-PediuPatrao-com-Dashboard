package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByNomeCliente(String nomeCliente);

    // DASHBOARD: busca no banco só os pedidos que entraram dentro do período (RF05 - filtros)
    // $gte = maior ou igual ao início | $lt = menor que o fim (o fim é a meia-noite do dia seguinte)
    // ?0 e ?1 são o primeiro e o segundo parâmetro do método
    // Usamos @Query porque o nome derivado (findByDataPedidoGreaterThanEqualAndDataPedidoLessThan)
    // repete o mesmo campo duas vezes, e o MongoDB não aceita montar a consulta desse jeito
    @Query("{ 'dataPedido': { $gte: ?0, $lt: ?1 } }")
    List<Pedido> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fimExclusivo);
}
