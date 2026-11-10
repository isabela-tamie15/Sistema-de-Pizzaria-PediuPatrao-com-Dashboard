package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraDeNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    // PARTE 2: ProdutoRepository injetado para buscar o preço oficial de cada produto no cadastro
    // Na Parte 1 o preço vinha do formulário e podia ser alterado pelo usuário antes de enviar
    @Autowired
    private ProdutoRepository produtoRepository;

    // AuditoriaService injetado com @Autowired para registrar cada ação importante no histórico
    // PedidoService não acessa AuditoriaRepository diretamente, delega para AuditoriaService
    @Autowired
    private AuditoriaService auditoriaService;

    // Cria o pedido com status inicial RECEBIDO e data/hora automáticos
    // RF02: valorTotal calculado no servidor a partir dos itens (não aceita valor do formulário)
    // RF02: responsavelPeloRegistro capturado do usuário autenticado via SecurityContextHolder
    // PARTE 2: o preço de cada item agora vem do cadastro de produtos, e não do formulário
    // PARTE 2: @PreAuthorize é a 2ª camada de segurança. Mesmo que uma rota nova esqueça a regra
    // no SecurityConfig, o método recusa quem não for ATENDENTE ou GERENTE
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public Pedido criarPedido(Pedido pedido) {

        // PARTE 2: como o pedido chega de um formulário, alguém poderia enviar campos a mais na requisição
        // Por isso zeramos tudo que só o sistema pode definir (id, status, desconto, saída)
        // Sem isso, enviar um "id" de outro pedido faria o save() sobrescrever aquele pedido
        pedido.setId(null);
        pedido.setDesconto(null);
        pedido.setResponsavelSaida(null);
        pedido.setDataHoraSaida(null);

        if (pedido.getNomeCliente() == null || pedido.getNomeCliente().isBlank()) {
            throw new RegraDeNegocioException("Selecione o cliente do pedido.");
        }

        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new RegraDeNegocioException("O pedido precisa ter pelo menos um item.");
        }

        // Calcula o valorTotal a partir dos itens no servidor (RF02)
        // O formulário não deve definir o valor arbitrariamente, a regra de negócio é do backend
        // PARTE 2: para cada item, buscamos o produto no banco e usamos o preço cadastrado
        // Na Parte 1 o cálculo era feito no servidor, mas com o preço que vinha do formulário
        double valorBruto = 0.0;
        for (ItemPedido item : pedido.getItens()) {

            if (item.getNomeProduto() == null || item.getNomeProduto().isBlank()) {
                throw new RegraDeNegocioException("Selecione o produto de todos os itens do pedido.");
            }

            if (item.getQuantidade() < 1) {
                throw new RegraDeNegocioException("A quantidade de cada item deve ser de pelo menos 1.");
            }

            Produto produto = produtoRepository.findFirstByNome(item.getNomeProduto())
                    .orElseThrow(() -> new RegraDeNegocioException(
                            "Produto não encontrado no cadastro: " + item.getNomeProduto()));

            // Boolean.FALSE.equals evita NullPointerException quando o campo ativo estiver vazio no banco
            if (Boolean.FALSE.equals(produto.getAtivo())) {
                throw new RegraDeNegocioException("O produto " + produto.getNome() + " está inativo e não pode ser vendido.");
            }

            if (produto.getPreco() == null || produto.getPreco() <= 0) {
                throw new RegraDeNegocioException("O produto " + produto.getNome() + " está sem preço válido no cadastro.");
            }

            // Sobrescreve o preço que veio do formulário pelo preço oficial do cadastro
            item.setPrecoUnitario(produto.getPreco());
            valorBruto += produto.getPreco() * item.getQuantidade();
        }

        pedido.setValorBruto(arredondar(valorBruto));
        pedido.setValorDesconto(0.0);
        pedido.setValorTotal(arredondar(valorBruto));

        pedido.setDataPedido(LocalDateTime.now());
        pedido.setStatus(StatusPedido.RECEBIDO);

        // Captura o usuário autenticado para registrar como responsável pelo pedido (RF02)
        pedido.setResponsavelPeloRegistro(usuarioLogado());

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // 1º parâmetro: nome da ação
        // 2º parâmetro: tipo da entidade afetada (PARTE 2)
        // 3º parâmetro: id do pedido afetado (entidadeId)
        // 4º parâmetro: campo alterado (PARTE 2)
        // 5º parâmetro: descrição legível da ação
        // 6º parâmetro: valorAnterior = null (pedido não existia antes)
        // 7º parâmetro: valorNovo = status inicial do pedido
        // 8º parâmetro: justificativa = null (não exigida na criação)
        auditoriaService.registrarAuditoria(
                "PEDIDO_CRIADO",
                "PEDIDO",
                pedidoSalvo.getId(),
                "status",
                "Pedido criado para: " + pedido.getNomeCliente()
                        + " | Itens: " + pedido.getItens().size()
                        + " | Total: R$ " + formatar(pedido.getValorTotal()),
                null,
                StatusPedido.RECEBIDO.name(),
                null
        );
        return pedidoSalvo;
    }

    // Busca um pedido pelo id ou lança exceção personalizada se não encontrar
    // Usado pelo PedidoController nos métodos de detalhe, status, cancelamento e desconto
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Pedido não encontrado com o id: " + id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    // Atualiza o status seguindo o fluxo obrigatório definido no PDF (RF03)
    // Antes de salvar, chama validarTransicaoDeStatus para garantir a sequência correta
    // valorAnterior e valorNovo são gravados na auditoria para rastreabilidade completa
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public Pedido atualizarStatus(String id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);

        StatusPedido statusAtual = pedido.getStatus();

        // Lança RegraDeNegocioException se a transição não for permitida pelo fluxo do PDF
        validarTransicaoDeStatus(statusAtual, novoStatus);

        // Guarda o status anterior antes de sobrescrever, para registrar na auditoria
        String valorAnterior = statusAtual.name();
        String descricao = "Status do pedido alterado";

        // RF03: registra data/hora e responsável quando pedido sai para entrega ou é retirado
        // PARTE 2: agora gravamos o responsável e a data/hora NO PRÓPRIO PEDIDO
        // Na Parte 1 isso só aparecia no texto da auditoria e não era persistido no pedido
        if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA || novoStatus == StatusPedido.RETIRADO) {
            pedido.setResponsavelSaida(usuarioLogado());
            pedido.setDataHoraSaida(LocalDateTime.now());
            // Dashboard: guarda se foi entrega ou retirada, porque depois o status vira FINALIZADO
            pedido.setTipoSaida(novoStatus);
            descricao = "Saída registrada por: " + pedido.getResponsavelSaida();
        }

        pedido.setStatus(novoStatus);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        auditoriaService.registrarAuditoria(
                "STATUS_ATUALIZADO",
                "PEDIDO",
                id,
                "status",
                descricao,
                valorAnterior,
                novoStatus.name(),
                null
        );
        return pedidoSalvo;
    }

    // Valida se a transição de status segue o fluxo obrigatório do PDF (RF03):
    // RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA ou RETIRADO → FINALIZADO
    // CANCELADO e FINALIZADO são estados terminais: nenhuma transição é permitida a partir deles
    // CANCELADO não aparece como destino aqui porque só pode ser feito pelo cancelarPedido (exclusivo do GERENTE)
    private void validarTransicaoDeStatus(StatusPedido statusAtual, StatusPedido novoStatus) {
        boolean transicaoValida = false;

        // Cada case representa os status que são destino válido a partir do status atual
        switch (statusAtual) {
            case RECEBIDO:
                transicaoValida = novoStatus == StatusPedido.EM_PREPARACAO;
                break;
            case EM_PREPARACAO:
                transicaoValida = novoStatus == StatusPedido.PRONTO;
                break;
            case PRONTO:
                // Do PRONTO pode ir para entrega (motoboy) ou retirada (balcão)
                transicaoValida = novoStatus == StatusPedido.SAIU_PARA_ENTREGA
                        || novoStatus == StatusPedido.RETIRADO;
                break;
            case SAIU_PARA_ENTREGA:
                transicaoValida = novoStatus == StatusPedido.FINALIZADO;
                break;
            case RETIRADO:
                transicaoValida = novoStatus == StatusPedido.FINALIZADO;
                break;
            case FINALIZADO:
            case CANCELADO:
                // Estados terminais: não permitem nenhuma transição
                transicaoValida = false;
                break;
        }

        if (!transicaoValida) {
            throw new RegraDeNegocioException(
                    "Transição de status inválida: " + statusAtual + " → " + novoStatus
                            + ". Verifique o fluxo obrigatório."
            );
        }
    }

    // Cancela um pedido, tarefa exclusiva do GERENTE (RF04)
    // Só é permitido antes do pedido sair para entrega ou ser retirado
    // justificativa é obrigatória e gravada na auditoria (exigência do PDF)
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelarPedido(String id, String justificativa) {
        Pedido pedido = buscarPorId(id);

        StatusPedido statusAtual = pedido.getStatus();

        // Pedidos em trânsito, retirados, finalizados ou já cancelados não podem ser cancelados
        if (pedidoJaSaiuOuEncerrou(statusAtual)) {
            throw new RegraDeNegocioException(
                    "Não é possível cancelar um pedido com status: " + statusAtual
            );
        }

        // justificativa é campo obrigatório para cancelamento conforme PDF RF04
        if (justificativa == null || justificativa.trim().isEmpty()) {
            throw new RegraDeNegocioException("A justificativa é obrigatória para cancelar um pedido.");
        }

        // O pedido não é apagado: só o status muda, preservando itens, valores e responsáveis (dados históricos)
        String valorAnterior = statusAtual.name();
        pedido.setStatus(StatusPedido.CANCELADO);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        auditoriaService.registrarAuditoria(
                "PEDIDO_CANCELADO",
                "PEDIDO",
                id,
                "status",
                "Pedido cancelado pelo GERENTE",
                valorAnterior,
                StatusPedido.CANCELADO.name(),
                // justificativa é passada como 8º parâmetro, único método que usa este campo
                justificativa.trim()
        );
        return pedidoSalvo;
    }

    // Aplica desconto ao pedido, tarefa exclusiva do GERENTE, máximo de 20% (RF04)
    // valorAnterior = valor original antes do desconto
    // valorNovo = valor final após o desconto aplicado
    // PARTE 2: o desconto agora é sempre calculado sobre o valor ORIGINAL dos itens (valorBruto)
    // Se o gerente aplicar um novo desconto, ele SUBSTITUI o anterior, não soma.
    // Exemplo: pedido de R$ 100,00 com 10% → R$ 90,00. Gerente muda para 15% → R$ 85,00 (e não R$ 76,50)
    // Na Parte 1 o desconto era calculado sobre o valorTotal já descontado: 20% + 20% virava 36%
    // PARTE 2: também bloqueia o desconto depois da saída para entrega ou retirada
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, Double percentualDesconto) {
        Pedido pedido = buscarPorId(id);

        // PARTE 2: o desconto só pode acontecer ANTES da saída para entrega ou retirada (RF04)
        // Na Parte 1 essa verificação não existia no backend, só o botão era escondido na tela
        if (pedidoJaSaiuOuEncerrou(pedido.getStatus())) {
            throw new RegraDeNegocioException(
                    "Não é possível aplicar desconto em um pedido com status: " + pedido.getStatus()
            );
        }

        // Limite máximo de 20% conforme regra de negócio do PDF RF04
        if (percentualDesconto == null || percentualDesconto <= 0 || percentualDesconto > 20) {
            throw new RegraDeNegocioException(
                    "Desconto inválido. O valor deve ser entre 1% e 20%."
            );
        }

        Double valorBase = calcularValorOriginal(pedido);
        if (valorBase == null) {
            throw new RegraDeNegocioException("O pedido não tem valor calculado para aplicar desconto.");
        }

        // Guarda a situação anterior para a auditoria (percentual e valor final antes da mudança)
        Double percentualAnterior = pedido.getDesconto() != null ? pedido.getDesconto() : 0.0;
        Double valorAnterior = pedido.getValorTotal();

        Double valorDesconto = arredondar(valorBase * (percentualDesconto / 100));
        Double valorComDesconto = arredondar(valorBase - valorDesconto);

        // Atualiza os campos no pedido antes de salvar
        pedido.setValorBruto(valorBase);
        pedido.setDesconto(percentualDesconto);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(valorComDesconto);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        String descricao = percentualAnterior > 0
                ? "Desconto alterado pelo GERENTE (substitui o desconto anterior)"
                : "Desconto aplicado pelo GERENTE";

        auditoriaService.registrarAuditoria(
                "DESCONTO_APLICADO",
                "PEDIDO",
                id,
                "desconto",
                descricao,
                formatar(percentualAnterior) + "% | Total R$ " + formatar(valorAnterior),
                formatar(percentualDesconto) + "% | Total R$ " + formatar(valorComDesconto),
                null
        );
        return pedidoSalvo;
    }

    // Retorna o valor dos itens SEM desconto, base para calcular o novo desconto
    // Pedidos novos já guardam esse valor em valorBruto
    // Pedidos criados na Parte 1 não têm valorBruto: nesse caso, desfazemos o desconto antigo do valorTotal
    // Exemplo: valorTotal R$ 90,00 com desconto de 10% → valor original = 90 / 0,90 = R$ 100,00
    private Double calcularValorOriginal(Pedido pedido) {
        if (pedido.getValorBruto() != null) {
            return pedido.getValorBruto();
        }
        if (pedido.getValorTotal() == null) {
            return null;
        }
        if (pedido.getDesconto() != null && pedido.getDesconto() > 0) {
            return arredondar(pedido.getValorTotal() / (1 - pedido.getDesconto() / 100));
        }
        return pedido.getValorTotal();
    }

    // Status em que o pedido já saiu da pizzaria ou já foi encerrado
    // Usado para bloquear cancelamento e desconto depois da saída (RF04)
    private boolean pedidoJaSaiuOuEncerrou(StatusPedido status) {
        return status == StatusPedido.SAIU_PARA_ENTREGA
                || status == StatusPedido.RETIRADO
                || status == StatusPedido.FINALIZADO
                || status == StatusPedido.CANCELADO;
    }

    // Retorna o login do usuário autenticado no Spring Security
    private String usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SISTEMA";
    }

    // Arredonda para 2 casas decimais, evitando valores como 44.999999 nos cálculos com double
    private Double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    // Formata o número com 2 casas decimais para os textos da auditoria (ex: 45.5 → "45,50")
    private String formatar(Double valor) {
        return valor == null ? "-" : String.format("%.2f", valor).replace(".", ",");
    }

}
