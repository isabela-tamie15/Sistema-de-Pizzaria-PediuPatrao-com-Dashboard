# 🍕 Sistema de Pizzaria - PediuPatrão

> Sistema web de gerenciamento de pedidos para pizzaria, desenvolvido com **Spring Boot + MongoDB Atlas**, evoluído a partir do projeto base da disciplina. Tem controle de acesso por perfil, fluxo controlado de status dos pedidos, cancelamentos e descontos exclusivos do gerente, auditoria persistente das operações e **Dashboard Administrativo** com API REST própria.

---

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6-brightgreen?logo=springsecurity&logoColor=white)
![MongoDB Atlas](https://img.shields.io/badge/MongoDB-Atlas-green?logo=mongodb&logoColor=white)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3-005F0F?logo=thymeleaf&logoColor=white)
![Chart.js](https://img.shields.io/badge/Chart.js-2.9-FF6384?logo=chartdotjs&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven&logoColor=white)

</div>

---

Projeto desenvolvido para a disciplina de **Análise e Projeto de Software** da Universidade de Mogi das Cruzes (UMC).

| |                                   |
|---|-----------------------------------|
| 👩‍🎓 **Autora** | Isabela Tamie Shihara             |
| 🪪 **RGM** | 11231203887                       |
| 🏫 **Turma** | 8ºB - Engenharia de Software      |

---

## 📊 Dashboard Administrativo

Painel para o responsável pela pizzaria acompanhar as vendas, a situação dos pedidos e as tendências da operação. A tela busca todos os números em uma **API REST própria** (`/api/dashboard/**`), integrada ao MongoDB.

<div align="center">

![Dashboard Administrativo](docs/evidencias/06-atualizacao.png)

</div>

| 📌 O que o painel mostra | |
|---|---|
| **4 indicadores** | Pedidos realizados, pedidos cancelados, faturamento e ticket médio |
| **5 gráficos** | Pedidos por dia da semana, pedidos por horário, pedidos por status, entrega x retirada e produtos mais vendidos |
| **Filtros** | Data inicial e data final, com atalhos Hoje, 30 dias e Este ano |
| **Acesso** | Somente ADMIN e GERENTE |

### 📄 Documentação da atividade

| Entregável | Onde está |
|---|---|
| Requisitos funcionais e não funcionais | [Documentação do Dashboard, seções 2 e 3](docs/DOCUMENTACAO-DASHBOARD.md#-2-requisitos-funcionais) |
| Endpoints: parâmetros, exemplos de requisição e resposta, erros | [Documentação do Dashboard, seção 5](docs/DOCUMENTACAO-DASHBOARD.md#-5-documentação-dos-endpoints) |
| Evidências de testes | [Documentação do Dashboard, seção 6](docs/DOCUMENTACAO-DASHBOARD.md#-6-evidências-de-testes) e pasta [`docs/evidencias`](docs/evidencias) |
| Dashboard administrativo funcional | Print acima e `http://localhost:8080/dashboard` |
| Decisões de análise e projeto | [Documentação do Dashboard, seção 7](docs/DOCUMENTACAO-DASHBOARD.md#-7-decisões-de-análise-e-projeto) |
| Branch gitflow da API | [`feature/dashboard-api`](#-gitflow) |
| Vídeo de até 3 minutos | [Demonstração em vídeo](#-demonstração-em-vídeo) |

---

## 🎬 Demonstração em vídeo

<!-- ⚠️ TROCAR "COLE_AQUI_O_LINK_DO_VIDEO_DO_DASHBOARD" PELO LINK DO YOUTUBE -->

| Vídeo | Link | Conteúdo |
|---|---|---|
| 📊 **Dashboard Administrativo** | 🔗 **[Assistir no YouTube](COLE_AQUI_O_LINK_DO_VIDEO_DO_DASHBOARD)** | Dashboard funcionando, API e justificativa das decisões de análise e projeto (até 3 minutos) |
| 🍕 Perfis, pedidos e auditoria | 🔗 [Assistir no YouTube](https://youtu.be/dnC8DVx6CdE) | Login com os três perfis, restrições de acesso, fluxo de pedidos, desconto e cancelamento pelo GERENTE e auditoria |

---

## 📋 Sumário

- [Dashboard Administrativo](#-dashboard-administrativo)
- [Demonstração em vídeo](#-demonstração-em-vídeo)
- [Como executar](#️-como-executar)
- [Usuários de demonstração](#-usuários-de-demonstração)
- [Requisitos implementados](#-requisitos-implementados)
- [Matriz de permissões](#-matriz-de-permissões)
- [Rotas da aplicação](#️-rotas-da-aplicação)
- [Testes automatizados](#-testes-automatizados)
- [Testes manuais](#-testes-manuais)
- [Principais alterações em relação ao projeto base](#-principais-alterações-em-relação-ao-projeto-base)
- [Estrutura do projeto](#️-estrutura-do-projeto)
- [Gitflow](#-gitflow)

---

## ⚙️ Como executar

### Pré-requisitos

- **Java 21** (JDK)
- **Maven 3.9+** (ou use o wrapper `./mvnw` incluído no projeto)
- **Conta no MongoDB Atlas**: o banco de dados é na nuvem, não é necessário instalar o MongoDB localmente

> 💡 **Por que MongoDB Atlas em vez de Docker?**
> Esta versão usa o MongoDB Atlas (nuvem gratuita) no lugar do Docker Compose. Quem for clonar e rodar o projeto precisa criar sua própria conta no Atlas e colocar a sua connection string, como no passo 2.

---

### Passo a passo

**1. Clone o repositório:**
```bash
git clone https://github.com/isabela-tamie15/Sistema-de-Pizzaria-PediuPatrao-com-Dashboard.git
cd Sistema-de-Pizzaria-PediuPatrao-com-Dashboard
```

**2. Configure a conexão com o MongoDB Atlas:**

Abra `src/main/resources/application.properties` e substitua a linha abaixo pela sua própria connection string do Atlas:

```properties
# Substitua pela sua connection string do MongoDB Atlas
spring.data.mongodb.uri=mongodb+srv://SEU_USUARIO:SUA_SENHA@seucluster.mongodb.net/pizzaria?retryWrites=true&w=majority
spring.data.mongodb.database=pizzaria
```

> 📌 Para criar uma conta gratuita: [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
> Crie um cluster, um usuário de banco e libere seu IP no Network Access.

> 🔒 O usuário e a senha do banco nunca são enviados ao GitHub: no repositório, a linha fica com `SEU_USUARIO:SUA_SENHA`.

**3. Execute o projeto:**

Com Maven instalado:
```bash
mvn spring-boot:run
```

Ou com o wrapper do projeto (sem Maven instalado):
```bash
# Linux/Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

**4. Acesse no navegador:**

```
http://localhost:8080/login
```

O Dashboard Administrativo fica em `http://localhost:8080/dashboard` (perfis ADMIN e GERENTE).

> ✅ Na primeira inicialização, o `DataInitializer` cria automaticamente os três usuários de demonstração. Não é necessário cadastrar nada manualmente.

---

## 👥 Usuários de demonstração

Criados automaticamente pelo `DataInitializer` ao subir a aplicação, caso não existam:

| 👤 Usuário | 🔑 Senha | 🎭 Perfil |
|:---:|:---:|:---:|
| `admin` | `teste123` | ADMIN |
| `gerente` | `teste123` | GERENTE |
| `atendente` | `teste123` | ATENDENTE |

> As credenciais acima são apenas para demonstração acadêmica. Nunca use senhas simples em sistemas reais.

---

## 📌 Requisitos implementados

### 🍕 Parte 1: Sistema de pedidos

#### RF01: Controle de usuários e permissões

- Perfis `ADMIN`, `GERENTE` e `ATENDENTE` na entidade `Usuario`, carregados do MongoDB pelo `UsuarioDetailsService`
- Segurança em duas camadas: regras por URL e método HTTP no `SecurityConfig` e `@PreAuthorize` nos services
- Menus ocultados por perfil com `sec:authorize` no Thymeleaf; acessar a URL sem permissão leva para `/acesso-negado`
- Cancelamento e desconto restritos ao `GERENTE`; gestão de usuários restrita ao `ADMIN`
- CSRF ativo em todas as rotas: os formulários Thymeleaf recebem o token automaticamente e as chamadas `fetch()` enviam o token no cabeçalho
- API sem login responde `401` em vez de redirecionar para a tela de login
- Perfis aceitos no cadastro: apenas `ADMIN`, `GERENTE` e `ATENDENTE`; username único; ninguém altera o próprio perfil nem exclui a própria conta

#### RF02: Evolução do cadastro de pedidos

- Entidade `Pedido` com os campos `nomeCliente`, `itens` (lista de `ItemPedido`), `status`, `dataPedido`, `valorBruto`, `valorDesconto`, `valorTotal`, `observacoes`, `desconto`, `responsavelPeloRegistro`, `responsavelSaida`, `dataHoraSaida` e `tipoSaida`
- Preço de cada item buscado no cadastro de produtos e valor total calculado no servidor pelo `PedidoService`; o preço enviado pelo formulário é ignorado
- Status controlado pelo enum `StatusPedido`, com os únicos valores válidos do fluxo

#### RF03: Controle de entrada, saída e transições

Fluxo definido no enum `StatusPedido` e validado pelo `PedidoService`:

```
RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA
                                  ↘ RETIRADO           → FINALIZADO
```

```
(qualquer status antes da saída) → CANCELADO  ← exclusivo do GERENTE
```

- Transições fora do fluxo são bloqueadas pelo método `validarTransicaoDeStatus()` com exceção de negócio
- `CANCELADO` é estado final: nenhuma alteração posterior é aceita
- Responsável pelo registro capturado automaticamente pelo `SecurityContextHolder`
- Na saída (`SAIU_PARA_ENTREGA` ou `RETIRADO`), o responsável, a data/hora e o tipo de saída ficam gravados no próprio pedido

#### RF04: Cancelamentos e descontos

- **Cancelamento:** exclusivo do GERENTE, exige justificativa, bloqueado após a saída para entrega ou retirada
- **Desconto:** exclusivo do GERENTE, de 1% a 20%, calculado sempre sobre o valor original dos itens; um novo desconto substitui o anterior, e o desconto é bloqueado após a saída
- As duas operações geram registro de auditoria automaticamente

#### RF05: Auditoria

- Entidade `Auditoria` com a coleção `auditorias` no MongoDB
- Registra: usuário e perfil, ação, entidade (PEDIDO, CLIENTE, USUARIO) e seu id, campo alterado, valor anterior, valor novo, justificativa e data/hora
- A alteração de um cliente gera um registro por campo modificado (ex: `telefone`, `logradouro`), com o valor anterior e o novo
- Registros só podem ser incluídos e consultados: o `AuditoriaRepository` não tem métodos de alteração ou exclusão
- Operações auditadas automaticamente pelo `AuditoriaService`:

| 📋 Ação | ⏱️ Quando |
|---|---|
| `PEDIDO_CRIADO` | Criação de pedido |
| `STATUS_ATUALIZADO` | Cada mudança de status |
| `DESCONTO_APLICADO` | Aplicação de desconto pelo GERENTE |
| `PEDIDO_CANCELADO` | Cancelamento com justificativa |
| `CLIENTE_CRIADO` / `CLIENTE_ATUALIZADO` / `CLIENTE_EXCLUIDO` | Operações em clientes (atualização: um registro por campo) |
| `USUARIO_CRIADO` / `PERFIL_ALTERADO` / `USUARIO_EXCLUIDO` | Gestão de usuários (sem registrar senha) |

- Consulta exclusiva para `ADMIN` e `GERENTE` na tela **Auditoria**
- Filtros por `usuarioResponsavel` e `acao` com `@RequestParam(required = false)`: a URL funciona com ou sem parâmetros

### 📊 Parte 2: Dashboard Administrativo

| Requisito | Implementação |
|---|---|
| RF01: Indicadores gerais | 4 cards: pedidos realizados, pedidos cancelados, faturamento e ticket médio |
| RF02: Análise de vendas | Gráficos de pedidos por dia da semana e por horário (dias e horários de mais vendas) |
| RF03: Situação dos pedidos | Gráficos de pedidos por status e de entrega x retirada |
| RF04: Produtos mais vendidos | Gráfico com o top 5 de produtos |
| RF05: Filtros | Data inicial e data final, com atalhos Hoje / 30 dias / Este ano |
| RF06: Integração com a API | A tela consome `/api/dashboard/**` com `fetch()` |
| RF07: Atualização e tratamento | Botão Atualizar, horário da última atualização, mensagens de erro e tratamento de dados incompletos |

Requisitos não funcionais, endpoints, evidências e decisões em [docs/DOCUMENTACAO-DASHBOARD.md](docs/DOCUMENTACAO-DASHBOARD.md).

---

## 🔐 Matriz de permissões

| Funcionalidade | ADMIN | GERENTE | ATENDENTE |
|---|:---:|:---:|:---:|
| Administrar usuários e perfis | ✅ | ❌ | ❌ |
| Consultar pedidos | ✅ | ✅ | ✅ |
| Criar pedido e avançar status | ❌ | ✅ | ✅ |
| Aplicar desconto | ❌ | ✅ | ❌ |
| Cancelar pedido | ❌ | ✅ | ❌ |
| Consultar clientes | ❌ | ✅ | ✅ |
| Cadastrar e editar clientes | ❌ | ❌ | ✅ |
| Excluir cliente | ❌ | ✅ | ❌ |
| Consultar produtos (cardápio) | ✅ | ✅ | ✅ |
| Criar, editar e excluir produtos | ✅ | ✅ | ❌ |
| Consultar auditoria | ✅ | ✅ | ❌ |
| Dashboard Administrativo | ✅ | ✅ | ❌ |

> As permissões são aplicadas no backend em duas camadas: regras por URL e método HTTP no `SecurityConfig` e `@PreAuthorize` nos services. Os menus e botões escondidos com `sec:authorize` são apenas visuais.

---

## 🗺️ Rotas da aplicação

### Páginas (Thymeleaf MVC)

Todas as rotas abaixo exigem autenticação. O acesso sem login redireciona para `/login`.

| Método | Rota | Perfis permitidos | Descrição |
|---|---|---|---|
| GET | `/login` | Público | Tela de login |
| GET | `/` | Todos | Página inicial |
| GET | `/dashboard` | ADMIN, GERENTE | Dashboard Administrativo |
| GET | `/pedidos` | Todos | Lista de pedidos |
| GET | `/pedidos/novo` | GERENTE, ATENDENTE | Formulário de novo pedido |
| POST | `/pedidos/salvar` | GERENTE, ATENDENTE | Salva novo pedido |
| GET | `/pedidos/{id}` | Todos | Detalhe do pedido |
| POST | `/pedidos/{id}/status` | GERENTE, ATENDENTE | Atualiza status do pedido |
| POST | `/pedidos/{id}/cancelar` | GERENTE | Cancela pedido com justificativa |
| POST | `/pedidos/{id}/desconto` | GERENTE | Aplica desconto (máx. 20%) |
| GET | `/clientes` | GERENTE, ATENDENTE | Lista de clientes |
| GET | `/clientes/novo` | ATENDENTE | Formulário de novo cliente |
| POST | `/clientes/salvar` | ATENDENTE | Salva novo cliente |
| GET | `/clientes/editar/{id}` | ATENDENTE | Formulário de edição |
| POST | `/clientes/editar/{id}` | ATENDENTE | Salva edição do cliente |
| GET | `/produtos` | Todos | Lista do cardápio |
| GET | `/usuarios` | ADMIN | Lista de usuários |
| GET | `/usuarios/novo` | ADMIN | Formulário de novo usuário |
| POST | `/usuarios/salvar` | ADMIN | Salva novo usuário |
| GET | `/usuarios/editar/{id}` | ADMIN | Formulário de edição |
| POST | `/usuarios/editar/{id}` | ADMIN | Salva edição do usuário |
| POST | `/usuarios/deletar/{id}` | ADMIN | Remove usuário |
| GET | `/auditoria` | ADMIN, GERENTE | Lista de registros de auditoria |
| GET | `/auditoria?usuarioResponsavel=X` | ADMIN, GERENTE | Filtro por usuário |
| GET | `/auditoria?acao=X` | ADMIN, GERENTE | Filtro por tipo de ação |
| GET | `/acesso-negado` | Todos | Tela de acesso negado (403) |

### API REST

| Método | Rota | Perfis | Descrição |
|---|---|---|---|
| GET | `/api/clientes` | GERENTE, ATENDENTE | Lista clientes (JSON) |
| POST | `/api/clientes` | ATENDENTE | Cria cliente |
| PUT | `/api/clientes/{id}` | ATENDENTE | Atualiza cliente |
| DELETE | `/api/clientes/{id}` | GERENTE | Remove cliente |
| GET | `/api/produtos` | Todos | Lista produtos/cardápio (JSON) |
| POST | `/api/produtos` | ADMIN, GERENTE | Cria produto |
| PUT | `/api/produtos/{id}` | ADMIN, GERENTE | Atualiza produto |
| DELETE | `/api/produtos/{id}` | ADMIN, GERENTE | Remove produto |
| GET | `/api/usuarios` | ADMIN | Lista usuários (JSON) |
| GET | `/api/usuarios/{id}` | ADMIN | Detalha usuário |
| POST | `/api/usuarios` | ADMIN | Cria usuário |
| PUT | `/api/usuarios/{id}` | ADMIN | Atualiza usuário |
| DELETE | `/api/usuarios/{id}` | ADMIN | Remove usuário |
| GET | `/api/dashboard/resumo` | ADMIN, GERENTE | Indicadores gerais de vendas |
| GET | `/api/dashboard/vendas-por-dia-da-semana` | ADMIN, GERENTE | Pedidos e faturamento de segunda a domingo |
| GET | `/api/dashboard/vendas-por-hora` | ADMIN, GERENTE | Pedidos e faturamento por horário (0h a 23h) |
| GET | `/api/dashboard/pedidos-por-status` | ADMIN, GERENTE | Quantidade de pedidos por status |
| GET | `/api/dashboard/entrega-retirada` | ADMIN, GERENTE | Quantidade de pedidos entregues e retirados |
| GET | `/api/dashboard/produtos-mais-vendidos` | ADMIN, GERENTE | Ranking de produtos |

> Todas as rotas `/api/**` exigem o token CSRF nas requisições que alteram dados (POST, PUT, DELETE). Sem login respondem `401`, sem permissão `403`, e regras de negócio recusadas `400` com `{"erro": "mensagem"}`.

---

## 🤖 Testes automatizados

**46 testes**, todos aprovados. No IntelliJ: botão direito em `src/test/java` → **Run 'All Tests'**, ou no terminal `mvnw.cmd test`.

| Classe | O que testa |
|---|---|
| `DashboardServiceTest` | Cálculos, filtros e tratamento de dados do dashboard |
| `AutorizacaoBackendTest` | Requisições HTTP com cada perfil: `401` sem login, `403` sem permissão ou sem token CSRF, acesso permitido ao GERENTE e API do dashboard |
| `PedidoServiceRegrasTest` | Preço vindo do cadastro, produto inexistente ou inativo, desconto (limite, substituição, após a saída), saída gravada no pedido, transições e cancelamento |
| `ClienteServiceAuditoriaTest` | Auditoria de cliente com valor anterior e novo por campo |
| `PedidoServiceTest` | Inicialização da aplicação (exige o MongoDB configurado) |

---

## 🧪 Testes manuais

Roteiro para verificar as funcionalidades:

### 1. Autenticação e controle de acesso

| # | Ação | Resultado esperado |
|---|---|---|
| 1 | Acessar `localhost:8080` sem login | Redirecionado para `/login` |
| 2 | Login com `admin` / `teste123` | Acessa o dashboard, menu sem opções de pedido |
| 3 | Tentar acessar `/pedidos/novo` como `admin` | Redirecionado para `/acesso-negado` |
| 4 | Login com `atendente` / `teste123` | Acessa pedidos e clientes, sem ver auditoria nem dashboard |
| 5 | Tentar acessar `/auditoria` como `atendente` | Redirecionado para `/acesso-negado` |
| 6 | Login com `gerente` / `teste123` | Acessa pedidos, clientes, auditoria, cardápio e dashboard |

### 2. Fluxo de pedidos

| # | Ação | Resultado esperado |
|---|---|---|
| 7 | Criar pedido como `atendente` | Pedido salvo com status `RECEBIDO` e responsável preenchido |
| 8 | Avançar status para `EM_PREPARACAO` | Transição aceita |
| 9 | Tentar avançar direto para `FINALIZADO` | Erro: transição inválida |
| 10 | Avançar até `PRONTO` → `SAIU_PARA_ENTREGA` → `FINALIZADO` | Todas as transições aceitas em ordem |
| 11 | Abrir o detalhe de um pedido que já saiu | Mostra data/hora e responsável pela saída |

### 3. Cancelamento e desconto (apenas GERENTE)

| # | Ação | Resultado esperado |
|---|---|---|
| 12 | Criar pedido como `atendente`, logar como `gerente` e cancelar | Pedido com status `CANCELADO` |
| 13 | Tentar cancelar sem justificativa | Erro de validação |
| 14 | Aplicar desconto de 10% como `gerente` | Valor total recalculado |
| 15 | Tentar aplicar desconto de 25% | Erro: o limite é 20% |
| 16 | Aplicar 20% e depois 20% de novo no mesmo pedido | O total fica com 20% de desconto (não 36%) |
| 17 | Tentar aplicar desconto ou cancelar um pedido que já saiu | Operação bloqueada |

### 4. Auditoria

| # | Ação | Resultado esperado |
|---|---|---|
| 18 | Logar como `admin` e acessar `/auditoria` | Lista todos os eventos registrados |
| 19 | Filtrar por `usuarioResponsavel=gerente` | Apenas eventos gerados pelo gerente |
| 20 | Como `atendente`, trocar telefone e logradouro de um cliente | Auditoria mostra 2 registros, cada um com o campo, o valor anterior e o novo |

### 5. Gestão de usuários (apenas ADMIN)

| # | Ação | Resultado esperado |
|---|---|---|
| 21 | Logar como `admin` e criar novo usuário | Usuário salvo no banco com senha criptografada |
| 22 | Logar como `gerente` e tentar acessar `/usuarios` | Redirecionado para `/acesso-negado` |
| 23 | Como `admin`, tentar mudar o próprio perfil ou excluir a si mesmo | Operação recusada com mensagem |

### 6. Dashboard

O roteiro completo, com os prints, está em [docs/DOCUMENTACAO-DASHBOARD.md](docs/DOCUMENTACAO-DASHBOARD.md#-6-evidências-de-testes).

---

## 📝 Principais alterações em relação ao projeto base

| 🔧 Área | Projeto base | ✅ Esta versão |
|---|---|---|
| Perfis de acesso | Todos autenticados recebiam `USER` | `ADMIN`, `GERENTE` e `ATENDENTE` distintos, lidos do MongoDB |
| CSRF | Desabilitado | Ativo nos formulários e nas APIs `/api/**` |
| Status do pedido | Campo de texto livre, qualquer valor aceito | Enum `StatusPedido` com fluxo validado pelo `PedidoService` |
| Transições de status | Sem validação | `validarTransicaoDeStatus()` bloqueia transições inválidas |
| Preço dos itens | Enviado pelo formulário | Buscado no cadastro de produtos e calculado no servidor |
| Cancelamento | Inexistente | Exclusivo do GERENTE, exige justificativa, bloqueado após a saída |
| Desconto | Inexistente | Exclusivo do GERENTE, máximo 20%, sempre sobre o valor original |
| Auditoria | Inexistente | `AuditoriaService` registra cada operação relevante, com valor anterior e novo |
| Filtro de auditoria | Inexistente | `@RequestParam(required = false)` permite filtrar por usuário ou ação |
| Banco de dados | MongoDB local | MongoDB Atlas (nuvem), sem necessidade de instalar localmente |
| Usuários iniciais | Criados manualmente | `DataInitializer` cria os 3 perfis automaticamente no startup |
| Segurança | Sem regras detalhadas | `SecurityConfig` com regras por rota, método HTTP e perfil, mais `@PreAuthorize` nos services |
| Erros de regra de negócio | Erro 500 genérico | Status `400` com a mensagem da regra (JSON na API, página nas telas) |
| Dashboard | Inexistente | Dashboard Administrativo com API REST própria |

---

## 🗂️ Estrutura do projeto

```
Sistema-de-Pizzaria-PediuPatrao-com-Dashboard/
├── src/
│   ├── main/
│   │   ├── java/com/umc/pediupatrao/
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java        ← cria usuários admin/gerente/atendente no startup
│   │   │   │   ├── MongoInitConfig.java
│   │   │   │   └── SecurityConfig.java         ← regras de acesso por URL e perfil
│   │   │   ├── controller/
│   │   │   │   ├── AuditoriaController.java    ← consulta de registros com filtros opcionais
│   │   │   │   ├── ClienteController.java      ← API REST de clientes
│   │   │   │   ├── DashboardApiController.java ← API REST do dashboard (/api/dashboard/**)
│   │   │   │   ├── DashboardController.java    ← abre a página /dashboard
│   │   │   │   ├── GlobalControllerAdvice.java ← respostas de erro 400 (JSON na API, página nas telas)
│   │   │   │   ├── HomeController.java         ← páginas Thymeleaf (usuários, clientes, produtos)
│   │   │   │   ├── PedidoController.java       ← fluxo de pedidos (MVC)
│   │   │   │   ├── ProdutoController.java      ← API REST de produtos
│   │   │   │   └── UsuarioController.java      ← API REST de usuários
│   │   │   ├── dto/
│   │   │   │   └── *DTO.java                   ← formatos de resposta da API do dashboard
│   │   │   ├── entity/
│   │   │   │   ├── Auditoria.java
│   │   │   │   ├── Cliente.java
│   │   │   │   ├── ItemPedido.java
│   │   │   │   ├── Pedido.java                 ← @Document MongoDB
│   │   │   │   ├── Produto.java
│   │   │   │   ├── StatusPedido.java           ← enum com os status válidos do fluxo
│   │   │   │   └── Usuario.java
│   │   │   ├── repository/
│   │   │   │   └── *.java                      ← repositórios Spring Data MongoDB
│   │   │   └── service/
│   │   │       ├── AuditoriaService.java       ← grava os registros de auditoria
│   │   │       ├── ClienteService.java         ← cadastro e auditoria campo a campo
│   │   │       ├── DashboardService.java       ← cálculos e filtros do dashboard
│   │   │       ├── PedidoService.java          ← regras de negócio e validação de transições
│   │   │       ├── UsuarioDetailsService.java  ← integração com o Spring Security
│   │   │       └── UsuarioService.java         ← gestão de usuários e perfis
│   │   └── resources/
│   │       ├── application.properties          ← configuração do MongoDB Atlas
│   │       └── templates/                      ← páginas HTML com Thymeleaf (dashboard/painel.html)
│   └── test/java/com/umc/pediupatrao/          ← testes automatizados
├── docs/
│   ├── DOCUMENTACAO-DASHBOARD.md               ← requisitos, endpoints, testes e decisões do dashboard
│   └── evidencias/                             ← prints dos testes
├── pom.xml
└── README.md
```

---

## 🌿 Gitflow

O projeto segue o **gitflow**: cada funcionalidade é desenvolvida em uma branch `feature/`, integrada à `develop` por Pull Request e, depois de testada, levada para a `main`.

| Branch | Conteúdo |
|---|---|
| `main` | Versão estável do sistema |
| `develop` | Integração das funcionalidades antes de irem para a `main` |
| `feature/correcoes-parte1` | Regras de pedidos, auditoria, gestão de usuários e segurança |
| `feature/dashboard-api` | API e tela do Dashboard Administrativo, testes e documentação |
