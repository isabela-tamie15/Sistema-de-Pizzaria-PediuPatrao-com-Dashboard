package com.umc.pediupatrao.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Regras de negócio do Dashboard Administrativo
// O controller só recebe os filtros e devolve o JSON; todos os cálculos ficam aqui
// @PreAuthorize na classe: só ADMIN e GERENTE (responsáveis pelo estabelecimento) consultam os números
@Service
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardService {

    // Período máximo de consulta, para a busca no banco não ficar pesada
    private static final int PERIODO_MAXIMO_DIAS = 366;

    // Quantidade máxima de produtos no ranking
    private static final int LIMITE_MAXIMO = 50;

    // Nomes dos dias na mesma ordem do Java: getDayOfWeek().getValue() vai de 1 (segunda) a 7 (domingo)
    private static final String[] DIAS_DA_SEMANA = {"Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"};

    @Autowired
    private PedidoRepository pedidoRepository;

    // ===== RF01 - Indicadores gerais de vendas =====
    // Pedidos realizados, pedidos cancelados, faturamento (soma só dos realizados) e ticket médio
    public ResumoVendasDTO resumo(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = definirFim(dataFim);
        LocalDate inicio = definirInicio(dataInicio, fim);
        validarPeriodo(inicio, fim);

        long realizados = 0;
        long cancelados = 0;
        double faturamento = 0.0;

        for (Pedido pedido : buscarPedidos(inicio, fim)) {
            if (pedido.getStatus() == StatusPedido.CANCELADO) {
                cancelados++;
            } else {
                realizados++;
                // Pedido cancelado não é venda, então só os realizados entram no faturamento
                faturamento += valor(pedido.getValorTotal());
            }
        }

        // Ticket médio = quanto cada pedido vale, em média
        // Se não houver pedidos, fica zero (evita dividir por zero)
        double ticketMedio = 0.0;
        if (realizados > 0) {
            ticketMedio = faturamento / realizados;
        }

        return new ResumoVendasDTO(inicio, fim, realizados, cancelados,
                arredondar(faturamento), arredondar(ticketMedio));
    }

    // ===== RF02 - Vendas por dia da semana =====
    // Mostra em quais dias da semana saem mais pedidos (de segunda a domingo)
    // Ajuda a decidir, por exemplo, em quais dias fazer promoção ou reforçar a equipe
    public List<VendaDiaSemanaDTO> vendasPorDiaDaSemana(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = definirFim(dataFim);
        LocalDate inicio = definirInicio(dataInicio, fim);
        validarPeriodo(inicio, fim);

        List<Pedido> pedidos = buscarPedidos(inicio, fim);
        List<VendaDiaSemanaDTO> resultado = new ArrayList<>();

        // Percorre os 7 dias da semana: posição 0 = segunda ... posição 6 = domingo
        for (int i = 0; i < 7; i++) {
            int numeroDoDia = i + 1; // 1 = segunda ... 7 = domingo, igual ao Java
            long quantidade = 0;
            double faturamentoDoDia = 0.0;

            for (Pedido pedido : pedidos) {
                // getDayOfWeek().getValue() pega o dia da semana da data de entrada (ex: 2026-11-09 → 1, segunda)
                boolean mesmoDia = pedido.getDataPedido().getDayOfWeek().getValue() == numeroDoDia;
                if (mesmoDia && pedido.getStatus() != StatusPedido.CANCELADO) {
                    quantidade++;
                    faturamentoDoDia += valor(pedido.getValorTotal());
                }
            }

            resultado.add(new VendaDiaSemanaDTO(DIAS_DA_SEMANA[i], quantidade, arredondar(faturamentoDoDia)));
        }
        return resultado;
    }

    // ===== RF02 - Vendas por horário =====
    // Mostra em quais horários do dia saem mais pedidos (de 0h a 23h)
    // Ajuda a decidir, por exemplo, quantos funcionários colocar em cada horário
    public List<VendaPorHoraDTO> vendasPorHora(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = definirFim(dataFim);
        LocalDate inicio = definirInicio(dataInicio, fim);
        validarPeriodo(inicio, fim);

        List<Pedido> pedidos = buscarPedidos(inicio, fim);
        List<VendaPorHoraDTO> resultado = new ArrayList<>();

        // Percorre as 24 horas do dia
        for (int hora = 0; hora < 24; hora++) {
            long quantidade = 0;
            double faturamentoDaHora = 0.0;

            for (Pedido pedido : pedidos) {
                // getHour() pega só a hora da data de entrada (ex: 19:45 → 19)
                boolean mesmaHora = pedido.getDataPedido().getHour() == hora;
                if (mesmaHora && pedido.getStatus() != StatusPedido.CANCELADO) {
                    quantidade++;
                    faturamentoDaHora += valor(pedido.getValorTotal());
                }
            }

            resultado.add(new VendaPorHoraDTO(hora, quantidade, arredondar(faturamentoDaHora)));
        }
        return resultado;
    }

    // ===== RF03 - Situação dos pedidos (quantidade por status) =====
    // Todos os 7 status aparecem, mesmo com zero, na ordem do fluxo do pedido
    public List<StatusQuantidadeDTO> pedidosPorStatus(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = definirFim(dataFim);
        LocalDate inicio = definirInicio(dataInicio, fim);
        validarPeriodo(inicio, fim);

        List<Pedido> pedidos = buscarPedidos(inicio, fim);
        List<StatusQuantidadeDTO> resultado = new ArrayList<>();

        // StatusPedido.values() devolve os 7 status do enum, na ordem em que foram escritos
        for (StatusPedido status : StatusPedido.values()) {
            long quantidade = 0;
            for (Pedido pedido : pedidos) {
                if (pedido.getStatus() == status) {
                    quantidade++;
                }
            }
            resultado.add(new StatusQuantidadeDTO(status.name(), descricao(status), quantidade));
        }
        return resultado;
    }

    // ===== RF03 - Entrega x retirada =====
    // Conta quantos pedidos saíram para entrega e quantos foram retirados no balcão
    // Pedidos que ainda não saíram (recebido, em preparação, pronto) e cancelados não entram
    public List<EntregaRetiradaDTO> entregaRetirada(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = definirFim(dataFim);
        LocalDate inicio = definirInicio(dataInicio, fim);
        validarPeriodo(inicio, fim);

        long entregas = 0;
        long retiradas = 0;

        for (Pedido pedido : buscarPedidos(inicio, fim)) {
            StatusPedido saida = tipoSaida(pedido);
            if (saida == StatusPedido.SAIU_PARA_ENTREGA) {
                entregas++;
            } else if (saida == StatusPedido.RETIRADO) {
                retiradas++;
            }
        }

        List<EntregaRetiradaDTO> resultado = new ArrayList<>();
        resultado.add(new EntregaRetiradaDTO("Entrega", entregas));
        resultado.add(new EntregaRetiradaDTO("Retirada", retiradas));
        return resultado;
    }

    // ===== RF04 - Produtos mais vendidos =====
    // Soma as quantidades de cada produto nos pedidos válidos (cancelados não contam)
    public List<ProdutoVendidoDTO> produtosMaisVendidos(LocalDate dataInicio, LocalDate dataFim, int limite) {
        LocalDate fim = definirFim(dataFim);
        LocalDate inicio = definirInicio(dataInicio, fim);
        validarPeriodo(inicio, fim);
        validarLimite(limite);

        // Map funciona como uma tabela: nome do produto → total acumulado
        Map<String, Long> quantidadePorProduto = new HashMap<>();
        Map<String, Double> faturamentoPorProduto = new HashMap<>();

        // 1. Soma quantidade e valor de cada produto
        for (Pedido pedido : buscarPedidos(inicio, fim)) {
            if (pedido.getStatus() == StatusPedido.CANCELADO || pedido.getItens() == null) {
                continue;
            }
            for (ItemPedido item : pedido.getItens()) {
                String nome = item.getNomeProduto() != null ? item.getNomeProduto() : "Produto sem nome";
                double subtotal = item.getQuantidade() * item.getPrecoUnitario();

                // getOrDefault: se o produto ainda não está na tabela, começa do zero
                quantidadePorProduto.put(nome, quantidadePorProduto.getOrDefault(nome, 0L) + item.getQuantidade());
                faturamentoPorProduto.put(nome, faturamentoPorProduto.getOrDefault(nome, 0.0) + subtotal);
            }
        }

        // 2. Transforma a tabela em lista
        List<ProdutoVendidoDTO> ranking = new ArrayList<>();
        for (String nome : quantidadePorProduto.keySet()) {
            ranking.add(new ProdutoVendidoDTO(nome, quantidadePorProduto.get(nome),
                    arredondar(faturamentoPorProduto.get(nome))));
        }

        // 3. Ordena do mais vendido para o menos vendido
        ranking.sort((a, b) -> Long.compare(b.quantidade(), a.quantidade()));

        // 4. Devolve só as primeiras posições (o "top 5", por exemplo)
        if (ranking.size() > limite) {
            return ranking.subList(0, limite);
        }
        return ranking;
    }

    // ===== Métodos auxiliares =====

    // Se a data final não vier, usa hoje
    private LocalDate definirFim(LocalDate dataFim) {
        return dataFim != null ? dataFim : LocalDate.now();
    }

    // Se a data inicial não vier, usa os últimos 30 dias (o fim e mais 29 dias para trás)
    private LocalDate definirInicio(LocalDate dataInicio, LocalDate fim) {
        return dataInicio != null ? dataInicio : fim.minusDays(29);
    }

    // RF05 / RF07: valida os filtros de data antes de consultar o banco
    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("A data inicial não pode ser depois da data final.");
        }
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        if (dias > PERIODO_MAXIMO_DIAS) {
            throw new RegraDeNegocioException("O período máximo de consulta é de " + PERIODO_MAXIMO_DIAS + " dias.");
        }
    }

    private void validarLimite(int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new RegraDeNegocioException("O limite deve ser entre 1 e " + LIMITE_MAXIMO + ".");
        }
    }

    // Busca no MongoDB os pedidos do período:
    // do início do primeiro dia (00:00) até antes da meia-noite do dia seguinte ao fim
    // RF07: pedidos sem data de entrada (dados antigos ou incompletos) são descartados
    private List<Pedido> buscarPedidos(LocalDate inicio, LocalDate fim) {
        List<Pedido> doBanco = pedidoRepository.buscarPorPeriodo(inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay());

        List<Pedido> pedidos = new ArrayList<>();
        for (Pedido pedido : doBanco) {
            if (pedido.getDataPedido() != null) {
                pedidos.add(pedido);
            }
        }
        return pedidos;
    }

    // Descobre como o pedido saiu: entrega ou retirada
    // Pedidos novos têm o campo tipoSaida, gravado pelo PedidoService na saída
    // RF07: pedidos antigos (criados antes desse campo) não têm tipoSaida, então usamos o status atual
    private StatusPedido tipoSaida(Pedido pedido) {
        if (pedido.getTipoSaida() != null) {
            return pedido.getTipoSaida();
        }
        return pedido.getStatus();
    }

    // RF07: valores vazios no banco são tratados como zero, em vez de causar NullPointerException
    private double valor(Double numero) {
        return numero != null ? numero : 0.0;
    }

    // Arredonda para 2 casas decimais (ex: 44.999999 → 45.0)
    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    // Texto de cada status para mostrar no painel
    private String descricao(StatusPedido status) {
        if (status == null) {
            return "Sem status";
        }
        switch (status) {
            case RECEBIDO:
                return "Recebido";
            case EM_PREPARACAO:
                return "Em preparação";
            case PRONTO:
                return "Pronto";
            case SAIU_PARA_ENTREGA:
                return "Saiu para entrega";
            case RETIRADO:
                return "Retirado";
            case FINALIZADO:
                return "Finalizado";
            default:
                return "Cancelado";
        }
    }
}
