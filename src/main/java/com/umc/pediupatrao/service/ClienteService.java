package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.exception.RegraDeNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    // AuditoriaService para registrar operações em dados de clientes (RF05)
    @Autowired
    private AuditoriaService auditoriaService;

    // PARTE 2: 2ª camada de segurança (a 1ª são as regras por URL no SecurityConfig)
    // RF01: só o ATENDENTE cadastra e atualiza clientes
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente novoCliente(Cliente cliente) {
        cliente.setId(null);
        return salvar(cliente);
    }

    // ATENDENTE cadastra e GERENTE consulta clientes (RF01)
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Metodo para excluir cliente
    // PARTE 2: só GERENTE
    @PreAuthorize("hasRole('GERENTE')")
    public void excluir(String id) {
        // Busca o nome antes de excluir para registrar na auditoria
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado para exclusão."));
        clienteRepository.deleteById(id);
        // Registra auditoria de exclusão de cliente (RF05)
        auditoriaService.registrarAuditoria("CLIENTE_EXCLUIDO", "CLIENTE", id, null,
                "Cliente excluído: " + cliente.getNome(), cliente.getNome(), null, null);
    }

    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente cliente) {

        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            throw new RegraDeNegocioException("O nome do cliente é obrigatório.");
        }

        // Se o cliente não tem ID (novo cliente), salva como novo
        if (cliente.getId() == null || cliente.getId().isBlank()) {
            cliente.setId(null);
            Cliente clienteSalvo = clienteRepository.save(cliente);
            // Registra auditoria de criação de cliente (RF05)
            auditoriaService.registrarAuditoria("CLIENTE_CRIADO", "CLIENTE", clienteSalvo.getId(), null,
                    "Novo cliente cadastrado: " + clienteSalvo.getNome(), null, clienteSalvo.getNome(), null);
            return clienteSalvo;
        }

        // Se já tem ID (cliente existente), atualiza
        // Verifica se o cliente existe antes de atualizar
        // PARTE 2: além de verificar, buscamos a versão que está no banco ANTES de salvar
        // Na Parte 1 salvávamos direto e passávamos null como valor anterior, então a auditoria não sabia o que mudou
        Cliente clienteAntigo = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado para atualização."));

        // O campo endereco antigo não aparece no formulário novo
        // Se ele não veio na requisição, mantemos o valor que já estava no banco para não apagar o dado
        if (cliente.getEndereco() == null) {
            cliente.setEndereco(clienteAntigo.getEndereco());
        }

        Cliente clienteSalvo = clienteRepository.save(cliente);

        // Registra auditoria de atualização de cliente (RF05)
        // PARTE 2: compara cada campo do cliente antigo com o novo
        // Cada campo que mudou gera o seu próprio registro de auditoria, com valor anterior e novo
        // Exemplo: trocou telefone e logradouro → 2 registros
        String id = clienteSalvo.getId();
        String nome = clienteSalvo.getNome();
        auditarCampo(id, nome, "nome", clienteAntigo.getNome(), cliente.getNome());
        auditarCampo(id, nome, "telefone", clienteAntigo.getTelefone(), cliente.getTelefone());
        auditarCampo(id, nome, "cep", clienteAntigo.getCep(), cliente.getCep());
        auditarCampo(id, nome, "logradouro", clienteAntigo.getLogradouro(), cliente.getLogradouro());
        auditarCampo(id, nome, "numero", clienteAntigo.getNumero(), cliente.getNumero());
        auditarCampo(id, nome, "complemento", clienteAntigo.getComplemento(), cliente.getComplemento());
        auditarCampo(id, nome, "bairro", clienteAntigo.getBairro(), cliente.getBairro());
        auditarCampo(id, nome, "cidade", clienteAntigo.getCidade(), cliente.getCidade());
        auditarCampo(id, nome, "estado", clienteAntigo.getEstado(), cliente.getEstado());

        return clienteSalvo;
    }

    // Registra a auditoria de UM campo, só se o valor mudou
    // Se o usuário salvou sem mudar o campo, nada é registrado
    private void auditarCampo(String clienteId, String nomeCliente, String campo,
                              String valorAnterior, String valorNovo) {
        String anterior = normalizar(valorAnterior);
        String novo = normalizar(valorNovo);

        // Objects.equals compara dois textos aceitando null (null == null é considerado igual)
        if (!Objects.equals(anterior, novo)) {
            auditoriaService.registrarAuditoria(
                    "CLIENTE_ATUALIZADO",
                    "CLIENTE",
                    clienteId,
                    campo,
                    "Campo " + campo + " do cliente " + nomeCliente + " alterado",
                    anterior,
                    novo,
                    null
            );
        }
    }

    // Trata texto vazio e texto só com espaços como "sem valor"
    // Assim "" e null não são considerados uma alteração diferente
    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

}

// RF05: Toda alteração em dados de clientes é registrada na auditoria automaticamente.
// O AuditoriaService captura internamente o usuário autenticado via SecurityContextHolder.
// Os métodos de escrita (novoCliente, salvar, excluir) registram auditoria;
// Os métodos listarClientes e buscarPorId não registram pois são consultas.
// PARTE 2: na alteração, cada campo modificado (telefone, logradouro, número etc.) gera seu próprio
// registro com campoAlterado, valorAnterior e valorNovo, como no exemplo do enunciado:
// Usuário atendente01 | ALTERAÇÃO DE CLIENTE | logradouro | Rua das Flores → Av. Brasil
