package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraDeNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

// PARTE 2: @PreAuthorize em cima da classe vale para TODOS os métodos dela
// Só o ADMIN administra usuários e perfis (RF01)
// O login não passa por aqui: ele usa o UsuarioDetailsService, que lê o UsuarioRepository direto
@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {

    // PARTE 2: únicos perfis aceitos pelo sistema (RF01)
    // Na Parte 1 o formulário oferecia o perfil USER, que não existe nas regras,
    // e a API aceitava qualquer texto como perfil
    private static final Set<String> PERFIS_VALIDOS = Set.of("ADMIN", "GERENTE", "ATENDENTE");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    // PARTE 2: valida username repetido, senha vazia e perfil inexistente antes de salvar
    public Usuario salvarUsuario(Usuario usuario) {
        // Zera o id para que um cadastro novo nunca sobrescreva um usuário existente
        usuario.setId(null);

        String username = usuario.getUsername() == null ? "" : usuario.getUsername().trim();
        if (username.isEmpty()) {
            throw new RegraDeNegocioException("O username é obrigatório.");
        }
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new RegraDeNegocioException("Já existe um usuário com o username: " + username);
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new RegraDeNegocioException("A senha é obrigatória para criar um usuário.");
        }

        usuario.setUsername(username);
        usuario.setRole(validarPerfil(usuario.getRole()));
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        Usuario salvo = usuarioRepository.save(usuario);

        // A senha nunca vai para a auditoria, só o username e o perfil
        auditoriaService.registrarAuditoria("USUARIO_CRIADO", "USUARIO", salvo.getId(), "role",
                "Usuário criado: " + salvo.getUsername(), null, salvo.getRole(), null);
        return salvo;
    }

    // Deleta um usuário pelo ID
    // PARTE 2: o usuário logado não pode excluir a própria conta (evita o ADMIN ficar sem acesso ao sistema)
    public void deletarUsuario(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));

        if (usuario.getUsername().equals(usuarioLogado())) {
            throw new RegraDeNegocioException("Você não pode excluir o seu próprio usuário.");
        }

        usuarioRepository.deleteById(id);
        auditoriaService.registrarAuditoria("USUARIO_EXCLUIDO", "USUARIO", id, null,
                "Usuário excluído: " + usuario.getUsername(), usuario.getRole(), null, null);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário
    // PARTE 2: valida o perfil, impede username repetido e impede o usuário de trocar o próprio perfil
    public Usuario atualizarUsuario(String id, Usuario usuarioAtualizado) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));

        String perfilAnterior = usuarioExistente.getRole();
        String perfilNovo = validarPerfil(usuarioAtualizado.getRole());

        // Proteção contra autoelevação / autorrebaixamento: ninguém altera o próprio perfil
        // Assim o único ADMIN não consegue tirar o próprio acesso por engano
        if (usuarioExistente.getUsername().equals(usuarioLogado()) && !perfilNovo.equals(perfilAnterior)) {
            throw new RegraDeNegocioException("Você não pode alterar o seu próprio perfil.");
        }

        String usernameNovo = usuarioAtualizado.getUsername() == null ? "" : usuarioAtualizado.getUsername().trim();
        if (usernameNovo.isEmpty()) {
            throw new RegraDeNegocioException("O username é obrigatório.");
        }
        Optional<Usuario> mesmoUsername = usuarioRepository.findByUsername(usernameNovo);
        if (mesmoUsername.isPresent() && !mesmoUsername.get().getId().equals(id)) {
            throw new RegraDeNegocioException("Já existe um usuário com o username: " + usernameNovo);
        }

        usuarioExistente.setUsername(usernameNovo);
        usuarioExistente.setRole(perfilNovo);

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        Usuario salvo = usuarioRepository.save(usuarioExistente);

        // Mudança de perfil é registrada na auditoria (quem deu qual privilégio para quem)
        if (!perfilNovo.equals(perfilAnterior)) {
            auditoriaService.registrarAuditoria("PERFIL_ALTERADO", "USUARIO", id, "role",
                    "Perfil do usuário " + salvo.getUsername() + " alterado", perfilAnterior, perfilNovo, null);
        }
        return salvo;
    }

    // Confere se o perfil enviado é um dos 3 perfis do sistema
    private String validarPerfil(String perfil) {
        String perfilMaiusculo = perfil == null ? "" : perfil.trim().toUpperCase();
        if (!PERFIS_VALIDOS.contains(perfilMaiusculo)) {
            throw new RegraDeNegocioException("Perfil inválido. Use ADMIN, GERENTE ou ATENDENTE.");
        }
        return perfilMaiusculo;
    }

    private String usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }
}

// Este serviço só é acessado por rotas de ADMIN (/usuarios/** e /api/usuarios/**, protegidas no SecurityConfig).
// Mesmo assim, as regras ficam aqui no serviço, porque a tela pode ser burlada mas o backend não.
