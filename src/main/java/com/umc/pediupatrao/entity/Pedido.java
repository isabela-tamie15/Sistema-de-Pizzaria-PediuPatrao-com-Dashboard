package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//Removi o @Entity e seu import, pois só serve para usar JPA em banco sql, e aqui é banco nosql
//@Entity é uma anotação do JPA (Java Persistence API) para bancos relacionais
//@Document é a anotação que gera a coleção em banco noSQL como MongoDB
//@Entity é a anotação equivalente que gera tabela em banco sql como PostgreSQL

@Document(collection = "pedidos")
public class Pedido {

    @Id
    private String id;

    private String nomeCliente;

    // ItemPedido é atributo de Pedido, por isso não tem repository e nem coleção própria no banco
    private List<ItemPedido> itens = new ArrayList<>();

    private StatusPedido status;

    // Data e hora de ENTRADA do pedido (RF02 e RF03), preenchida pelo PedidoService na criação
    private LocalDateTime dataPedido;

    // PARTE 2: valor dos itens antes do desconto (soma de quantidade x preço do cadastro de produtos)
    // Guardamos separado para o desconto ser sempre calculado sobre o valor original,
    // e para mostrar na tela quanto foi concedido de desconto
    private Double valorBruto;

    // PARTE 2: valor em reais que foi descontado (ex: 10% de R$ 50,00 = R$ 5,00)
    private Double valorDesconto;

    // Tipo Double (com D maiúsculo) para aceitar null antes do desconto ser calculado
    // double minúsculo não aceita null, causaria NullPointerException no PedidoService
    // valorTotal é o valor final do pedido: valorBruto - valorDesconto
    private Double valorTotal;

    private String observacoes;

    // Desconto aplicado pelo GERENTE (máximo 20%)
    // Double com D maiúsculo para aceitar null quando nenhum desconto foi aplicado ainda
    private Double desconto;

    // Usuário que registrou o pedido (RF02 - responsável pelo registro)
    // Preenchido automaticamente pelo PedidoService a partir do usuário autenticado
    private String responsavelPeloRegistro;

    // PARTE 2 (RF03): quem registrou a SAÍDA do pedido (SAIU_PARA_ENTREGA ou RETIRADO)
    // Na Parte 1 essa informação só ia para a auditoria e não ficava salva no pedido
    private String responsavelSaida;

    // PARTE 2 (RF03): data e hora em que o pedido saiu para entrega ou foi retirado
    private LocalDateTime dataHoraSaida;

    // Getters e Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public LocalDateTime getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(LocalDateTime dataPedido) {
        this.dataPedido = dataPedido;
    }

    public Double getValorBruto() {
        return valorBruto;
    }

    public void setValorBruto(Double valorBruto) {
        this.valorBruto = valorBruto;
    }

    public Double getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(Double valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    // Desconto do Gerente
    public Double getDesconto() {
        return desconto;
    }

    public void setDesconto(Double desconto) {
        this.desconto = desconto;
    }

    // Responsável pelo registro (RF02)
    public String getResponsavelPeloRegistro() {
        return responsavelPeloRegistro;
    }

    public void setResponsavelPeloRegistro(String responsavelPeloRegistro) {
        this.responsavelPeloRegistro = responsavelPeloRegistro;
    }

    // Responsável e horário de saída (RF03)
    public String getResponsavelSaida() {
        return responsavelSaida;
    }

    public void setResponsavelSaida(String responsavelSaida) {
        this.responsavelSaida = responsavelSaida;
    }

    public LocalDateTime getDataHoraSaida() {
        return dataHoraSaida;
    }

    public void setDataHoraSaida(LocalDateTime dataHoraSaida) {
        this.dataHoraSaida = dataHoraSaida;
    }

}


//CORREÇÕES nos atributos:

//Antes eram 3 atributos soltos (tipoPizza + sabor + quantidade)
//Agora são 3 atributos interligados (nome, quantidade e preço), que compõem 1 itemPedido
//Mas um pedido realista tem lista de itens, por isso representamos assim List<ItemPedido> itens

//antes era clienteId, agora é nomeCliente, porque id sozinho não auxilia na tela, melhor ter o nome do cliente

//antes era String status, um texto livre, no qual o atendente podia digitar qualquer coisa
//agora é StatusPedido status, o enum, que só aceita os 5 valores predefinidos, vai virar um <select> no formulário

//ACRÉSCIMOS nos atributos:

//dataPedido foi adicionado para ter data e hora que o pedido foi feito
//valorTotal foi adicionado para ter cálculo de valor total do pedido (Double com D maiúsculo aceita null)
//observacoes foi adicionado para ter campo livre para o cliente pedir personalização do pedido
//desconto foi adicionado para registrar o percentual de desconto do GERENTE (RF04, máximo 20%)
//responsavelPeloRegistro foi adicionado para cumprir RF02: registrar quem criou o pedido

//ACRÉSCIMOS da PARTE 2 (correções apontadas pelo professor):

//valorBruto e valorDesconto: separam o valor original dos itens do valor descontado
//responsavelSaida e dataHoraSaida: agora a saída fica salva no próprio pedido, e não só na auditoria
