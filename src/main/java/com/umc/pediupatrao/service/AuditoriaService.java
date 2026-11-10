package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    // Registra uma nova ação no histórico de auditoria com todos os campos exigidos
    // PARTE 2: método completo, com o tipo da entidade e o campo alterado
    // entidade: PEDIDO, CLIENTE ou USUARIO
    // campoAlterado: nome do campo que mudou (ex: telefone). Pode ser null quando a ação não altera um campo específico
    // valorAnterior e valorNovo podem ser null quando não há campo específico alterado (ex: criação)
    // justificativa pode ser null quando não é exigida pela regra de negócio
    public void registrarAuditoria(String acao, String entidade, String entidadeId, String campoAlterado,
                                   String descricao, String valorAnterior, String valorNovo,
                                   String justificativa) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Se por algum motivo não houver usuário logado, registramos como SISTEMA em vez de quebrar a operação
        String usuarioResponsavel = "SISTEMA";
        String perfilUsuario = "SISTEMA";
        if (auth != null && auth.isAuthenticated()) {
            usuarioResponsavel = auth.getName();
            // Pega o primeiro perfil do usuário logado (ex: ROLE_GERENTE e remove o prefixo ROLE_)
            if (!auth.getAuthorities().isEmpty()) {
                perfilUsuario = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
            }
        }

        Auditoria auditoria = new Auditoria();
        auditoria.setAcao(acao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setCampoAlterado(campoAlterado);
        auditoria.setUsuarioResponsavel(usuarioResponsavel);
        auditoria.setPerfilUsuario(perfilUsuario);
        auditoria.setDescricao(descricao);
        auditoria.setValorAnterior(valorAnterior);
        auditoria.setValorNovo(valorNovo);
        auditoria.setJustificativa(justificativa);
        auditoria.setDataHora(LocalDateTime.now());
        auditoriaRepository.save(auditoria);
    }

    // Método da Parte 1, mantido para não quebrar chamadas antigas
    // Ele apenas repassa para o método completo, sem entidade e sem campo
    public void registrarAuditoria(String acao, String entidadeId, String descricao,
                                   String valorAnterior, String valorNovo, String justificativa) {
        registrarAuditoria(acao, null, entidadeId, null, descricao, valorAnterior, valorNovo, justificativa);
    }

    // Retorna todo o historico de auditoria do sistema
    // PARTE 2: ordenado do mais recente para o mais antigo
    // PARTE 2: consulta da auditoria só para ADMIN e GERENTE (RF05)
    // registrarAuditoria NÃO tem @PreAuthorize, pois é chamado pelas ações de todos os perfis
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<Auditoria> listarTodos() {
        return auditoriaRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora"));
    }

    // Retorna apenas as ações de um usuário específico
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<Auditoria> listarPorUsuario(String usuarioResponsavel) {
        return auditoriaRepository.findByUsuarioResponsavel(usuarioResponsavel);
    }

    // Retorna apenas as ações de um tipo específico (ex: "PEDIDO_CRIADO")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public List<Auditoria> listarPorAcao(String acao) {
        return auditoriaRepository.findByAcao(acao);
    }
}
// AuditoriaService não faz validação, só registra o que aconteceu.
// O metodo registrarAuditoria() é chamado pelo PedidoService e ClienteService sempre que uma ação importante ocorre.
// PARTE 2: também é chamado pelo UsuarioService (criação, exclusão e mudança de perfil, sem registrar a senha).
// usuarioResponsavel e perfilUsuario são capturados automaticamente do Spring Security, não precisam ser passados como parâmetro.
// dataHora é preenchida automaticamente pelo sistema com LocalDateTime.now().
// Não existe método de alterar ou excluir auditoria: o registro é só de inclusão e consulta (RF05).
