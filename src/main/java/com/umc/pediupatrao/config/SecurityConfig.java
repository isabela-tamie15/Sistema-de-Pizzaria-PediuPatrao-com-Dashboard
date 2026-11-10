package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // CORREÇÃO: sem isso, @PreAuthorize nos controllers era ignorado
public class SecurityConfig {

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORREÇÃO: linha csrf desabilitada foi removida
                // Thymeleaf precisa de CSRF ativo — ele injeta o token automaticamente nos formulários
                // PARTE 1: /api/** usava fetch() sem token CSRF, então desabilitamos só para essas rotas
                // PARTE 2: CSRF ativo em TODAS as rotas, inclusive /api/**
                // O layout.html coloca o token em <meta> e o JavaScript das telas envia esse token
                // no cabeçalho de cada fetch(), então não é mais preciso desabilitar nas APIs
                .csrf(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Rotas públicas: login, arquivos estáticos e páginas de erro
                        .requestMatchers("/login", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/acesso-negado", "/error").permitAll()

                        // Gestão de usuários: só ADMIN
                        // Proteção para evitar que um usuário ultrapasse sua permissão ao criar ou alterar usuários
                        .requestMatchers("/usuarios/**", "/api/usuarios/**").hasRole("ADMIN")

                        // Auditoria: só ADMIN e GERENTE podem consultar
                        .requestMatchers("/auditoria/**").hasAnyRole("ADMIN", "GERENTE")

                        // PEDIDOS
                        // Cancelamento e desconto: exclusivo do GERENTE
                        .requestMatchers(HttpMethod.POST, "/pedidos/*/cancelar", "/pedidos/*/desconto").hasRole("GERENTE")
                        // Registrar pedido: ATENDENTE (registra) e GERENTE (gerencia)
                        .requestMatchers("/pedidos/novo", "/pedidos/salvar").hasAnyRole("ATENDENTE", "GERENTE")
                        // Mudar status: ATENDENTE (acompanha o andamento) e GERENTE (gerencia)
                        .requestMatchers(HttpMethod.POST, "/pedidos/*/status").hasAnyRole("ATENDENTE", "GERENTE")
                        // Consultar pedidos: os 3 perfis (o ADMIN só consulta)
                        .requestMatchers(HttpMethod.GET, "/pedidos", "/pedidos/**").hasAnyRole("ADMIN", "GERENTE", "ATENDENTE")

                        // CLIENTES
                        // PARTE 2: na Parte 1 qualquer usuário logado acessava /api/clientes
                        // Cadastrar e atualizar: ATENDENTE (RF01)
                        .requestMatchers("/clientes/novo", "/clientes/salvar", "/clientes/editar/**").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.POST, "/api/clientes").hasRole("ATENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/clientes/**").hasRole("ATENDENTE")
                        // Excluir: só GERENTE, pois apaga o cadastro (fica registrado na auditoria)
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("GERENTE")
                        // Consultar: ATENDENTE e GERENTE (RF01: gerente consulta clientes)
                        .requestMatchers(HttpMethod.GET, "/clientes", "/api/clientes/**").hasAnyRole("ATENDENTE", "GERENTE")

                        // PRODUTOS
                        // PARTE 2: na Parte 1 qualquer usuário logado podia criar, alterar e excluir produtos
                        // Consultar: qualquer usuário logado (o atendente precisa ver o cardápio)
                        .requestMatchers(HttpMethod.GET, "/produtos", "/api/produtos/**").authenticated()
                        // Criar, alterar e excluir: ADMIN e GERENTE, porque o preço do produto define o valor dos pedidos
                        .requestMatchers("/api/produtos/**").hasAnyRole("ADMIN", "GERENTE")

                        // MINHA PIZZARIA e páginas administrativas
                        .requestMatchers("/funcionarios", "/entregadores", "/horarios", "/configuracoes").hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/admin", "/teste").hasRole("ADMIN")

                        // Qualquer outra rota exige apenas login
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        // PARTE 2: API chamada sem login devolve 401, em vez de redirecionar para a tela de login
                        // (quem chama uma API espera um código de erro, e não uma página HTML)
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**"))
                        // Redireciona para tela de acesso negado quando perfil não tem permissão
                        .accessDeniedPage("/acesso-negado")
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/"); // Redireciona para a página inicial após login bem-sucedido
        };
    }
}

// Ordem das regras: o Spring Security testa de cima para baixo e usa a PRIMEIRA que combinar.
// Por isso as regras mais específicas (ex: /pedidos/novo) vêm antes das mais gerais (ex: GET /pedidos/**).
// Esconder botões no HTML (sec:authorize) é só visual: quem garante a permissão são as regras acima.
// PARTE 2: a segurança tem DUAS camadas no backend:
// 1ª camada: as regras por URL aqui no SecurityConfig (barram a requisição antes de chegar no controller)
// 2ª camada: @PreAuthorize nos métodos dos services (barram mesmo se uma rota nova ficar sem regra aqui)
// O @EnableMethodSecurity lá em cima é o que liga a 2ª camada.
