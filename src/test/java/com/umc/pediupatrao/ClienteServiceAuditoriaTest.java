package com.umc.pediupatrao;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Testes da correção 4: a auditoria de clientes registra valor anterior e novo de cada campo alterado
@ExtendWith(MockitoExtension.class)
class ClienteServiceAuditoriaTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void alteracaoDeTelefoneEEnderecoRegistraValorAnteriorENovo() {
        Cliente antigo = cliente("c1", "11 1111-1111", "Rua das Flores");
        Cliente novo = cliente("c1", "11 2222-2222", "Av. Brasil");

        when(clienteRepository.findById("c1")).thenReturn(Optional.of(antigo));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(chamada -> chamada.getArgument(0));

        clienteService.salvar(novo);

        // Um registro para o telefone, com o valor antigo e o novo
        verify(auditoriaService).registrarAuditoria(eq("CLIENTE_ATUALIZADO"), eq("CLIENTE"), eq("c1"),
                eq("telefone"), anyString(), eq("11 1111-1111"), eq("11 2222-2222"), isNull());

        // Um registro para o logradouro, igual ao exemplo do enunciado
        verify(auditoriaService).registrarAuditoria(eq("CLIENTE_ATUALIZADO"), eq("CLIENTE"), eq("c1"),
                eq("logradouro"), anyString(), eq("Rua das Flores"), eq("Av. Brasil"), isNull());

        // Os campos que não mudaram não geram registro: só esses 2
        verify(auditoriaService, times(2)).registrarAuditoria(anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), any(), any());
    }

    @Test
    void salvarSemAlterarNadaNaoGeraAuditoria() {
        Cliente antigo = cliente("c1", "11 1111-1111", "Rua das Flores");
        Cliente igual = cliente("c1", "11 1111-1111", "Rua das Flores");

        when(clienteRepository.findById("c1")).thenReturn(Optional.of(antigo));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(chamada -> chamada.getArgument(0));

        clienteService.salvar(igual);

        verifyNoInteractions(auditoriaService);
    }

    private Cliente cliente(String id, String telefone, String logradouro) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNome("Maria Silva");
        cliente.setTelefone(telefone);
        cliente.setCep("08780-000");
        cliente.setLogradouro(logradouro);
        cliente.setNumero("100");
        cliente.setBairro("Centro");
        cliente.setCidade("Mogi das Cruzes");
        cliente.setEstado("SP");
        return cliente;
    }
}
