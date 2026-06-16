Plano de Arquitetura do Backend — EventFlow

Problem Statement

O EventFlow precisa de um backend completo com Spring Boot 4 / Java 21 para gestão de eventos acadêmicos, incluindo autenticação JWT, RBAC com 3 perfis, CRUD de eventos/turmas, inscrições com lista de espera, e controle de
presença — seguindo rigorosamente as práticas de segurança documentadas no SECURITY.md.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Requisitos Confirmados

- Evento → Turma: 1:N (um evento tem várias turmas)
- Professor → Turma: 1:N (cada turma tem um professor)
- Presença: registro único por inscrição (presente/ausente)
- Conflito de horário: validação por sobreposição de data+hora
- Lista de espera: promoção automática quando vaga abre
- Banco: PostgreSQL
- Stack: Spring Boot 4.0.6, Java 21, Spring Security, JWT (jjwt 0.13.0), Bean Validation, Lombok, SpringDoc OpenAPI

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Estrutura de Pacotes

br.com.eventflow/
├── config/              # Configurações (Security, CORS, OpenAPI)
├── security/            # JWT filter, provider, entrypoint
├── controller/          # REST controllers
├── service/             # Lógica de negócio
├── repository/          # Spring Data JPA repositories
├── entity/              # Entidades JPA
├── dto/
│   ├── request/         # DTOs de entrada (com validações)
│   └── response/        # DTOs de saída
├── exception/           # Exceções customizadas + GlobalExceptionHandler
├── audit/               # AuditLogger
└── seed/                # DataSeeder (dev only)

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Modelagem de Entidades

erDiagram
USUARIO {
Long id PK
String nome
String email UK
String senha
Role papel
LocalDateTime criadoEm
}

      EVENTO {
          Long id PK
          String titulo
          String descricao
          LocalDate dataInicio
          LocalDate dataFim
          String local
          Integer cargaHoraria
          StatusEvento status
          LocalDateTime criadoEm
      }
      
      TURMA {
          Long id PK
          Long eventoId FK
          Long professorId FK
          String nome
          LocalDateTime dataHoraInicio
          LocalDateTime dataHoraFim
          Integer vagas
          StatusTurma status
      }
      
      INSCRICAO {
          Long id PK
          Long alunoId FK
          Long turmaId FK
          StatusInscricao status
          Boolean presente
          LocalDateTime criadaEm
          Integer posicaoEspera
      }
      
      REFRESH_TOKEN {
          Long id PK
          Long usuarioId FK
          String token UK
          LocalDateTime expiraEm
          Boolean revogado
      }
  
      USUARIO ||--o{ TURMA : "leciona"
      EVENTO ||--o{ TURMA : "contém"
      USUARIO ||--o{ INSCRICAO : "se inscreve"
      TURMA ||--o{ INSCRICAO : "possui"
      USUARIO ||--o| REFRESH_TOKEN : "possui"

Enums:

- Role: ADMIN, PROFESSOR, ALUNO
- StatusEvento: ABERTO, ENCERRADO, CANCELADO
- StatusTurma: ABERTA, ENCERRADA, CANCELADA
- StatusInscricao: CONFIRMADA, LISTA_ESPERA, CANCELADA

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Endpoints da API REST

Autenticação (/auth)

┌────────┬────────────────┬─────────┬────────────────────────────────┐
│ Método │ Rota           │ Acesso      │ Descrição                      │
├────────┼────────────────┼─────────────┼────────────────────────────────┤
│ POST   │ /auth/register │ Público     │ Cadastro de aluno              │
├────────┼────────────────┼─────────────┼────────────────────────────────┤
│ POST   │ /auth/login    │ Público     │ Login → Access + Refresh Token │
├────────┼────────────────┼─────────────┼────────────────────────────────┤
│ POST   │ /auth/refresh  │ Público     │ Renovar Access Token           │
├────────┼────────────────┼─────────────┼────────────────────────────────┤
│ POST   │ /auth/logout   │ Autenticado │ Revogar Refresh Token          │
└────────┴────────────────┴─────────────┴────────────────────────────────┘

Usuários (/usuarios)

┌────────┬──────────────────────┬──────────────────┬─────────────────┐
│ Método │ Rota                 │ Acesso           │ Descrição       │
├────────┼──────────────────────┼──────────────────┼─────────────────┤
│ GET    │ /usuarios            │ ADMIN            │ Listar usuários │
├────────┼──────────────────────┼──────────────────┼─────────────────┤
│ GET    │ /usuarios/{id}       │ ADMIN ou próprio │ Buscar por ID   │
├────────┼──────────────────────┼──────────────────┼─────────────────┤
│ PUT    │ /usuarios/{id}/papel │ ADMIN            │ Alterar papel   │
└────────┴──────────────────────┴──────────────────┴─────────────────┘

Eventos (/eventos)

┌────────┬───────────────┬─────────┬──────────────────────────────┐
│ Método │ Rota          │ Acesso  │ Descrição                    │
├────────┼───────────────┼─────────┼──────────────────────────────┤
│ POST   │ /eventos      │ ADMIN   │ Criar evento                 │
├────────┼───────────────┼─────────┼──────────────────────────────┤
│ GET    │ /eventos      │ Público │ Listar eventos (com filtros) │
├────────┼───────────────┼─────────┼──────────────────────────────┤
│ GET    │ /eventos/{id} │ Público │ Detalhar evento + turmas     │
├────────┼───────────────┼─────────┼──────────────────────────────┤
│ PUT    │ /eventos/{id} │ ADMIN   │ Editar evento                │
├────────┼───────────────┼─────────┼──────────────────────────────┤
│ DELETE │ /eventos/{id} │ ADMIN   │ Cancelar evento              │
└────────┴───────────────┴─────────┴──────────────────────────────┘

Turmas (/turmas)

┌────────┬────────────────────────────┬─────────┬─────────────────────────┐
│ Método │ Rota                       │ Acesso  │ Descrição               │
├────────┼────────────────────────────┼─────────┼─────────────────────────┤
│ POST   │ /eventos/{eventoId}/turmas │ ADMIN                       │ Criar turma             │
├────────┼────────────────────────────┼─────────────────────────────┼─────────────────────────┤
│ GET    │ /eventos/{eventoId}/turmas │ Público                     │ Listar turmas do evento │
├────────┼────────────────────────────┼─────────────────────────────┼─────────────────────────┤
│ PUT    │ /turmas/{id}               │ ADMIN                       │ Editar turma            │
├────────┼────────────────────────────┼─────────────────────────────┼─────────────────────────┤
│ DELETE │ /turmas/{id}               │ ADMIN                       │ Cancelar turma          │
├────────┼────────────────────────────┼─────────────────────────────┼─────────────────────────┤
│ GET    │ /turmas/{id}/inscritos     │ ADMIN, PROFESSOR (da turma) │ Lista de inscritos      │
└────────┴────────────────────────────┴─────────────────────────────┴─────────────────────────┘

Inscrições (/inscricoes)

┌────────┬──────────────────────────────┬─────────────────┬────────────────────┐
│ Método │ Rota                         │ Acesso          │ Descrição          │
├────────┼──────────────────────────────┼─────────────────┼────────────────────┤
│ POST   │ /turmas/{turmaId}/inscricoes │ ALUNO           │ Realizar inscrição │
├────────┼──────────────────────────────┼─────────────────┼────────────────────┤
│ DELETE │ /inscricoes/{id}             │ ALUNO (própria) │ Cancelar inscrição │
├────────┼──────────────────────────────┼─────────────────┼────────────────────┤
│ GET    │ /inscricoes/minhas           │ ALUNO           │ Minhas inscrições  │
└────────┴──────────────────────────────┴─────────────────┴────────────────────┘

Presença (/presenca)

┌────────┬────────────────────────────┬─────────────────────────────┬────────────────────────────┐
│ Método │ Rota                       │ Acesso                      │ Descrição                  │
├────────┼────────────────────────────┼─────────────────────────────┼────────────────────────────┤
│ PUT    │ /turmas/{turmaId}/presenca │ PROFESSOR (da turma), ADMIN │ Registrar presença (batch) │
├────────┼────────────────────────────┼─────────────────────────────┼────────────────────────────┤
│ GET    │ /turmas/{turmaId}/presenca │ PROFESSOR (da turma), ADMIN │ Listar presenças da turma  │
└────────┴────────────────────────────┴─────────────────────────────┴────────────────────────────┘

Dashboard (/dashboard)

┌────────┬──────────────────┬────────┬───────────────────────────┐
│ Método │ Rota             │ Acesso │ Descrição                 │
├────────┼──────────────────┼────────┼───────────────────────────┤
│ GET    │ /dashboard/admin │ ADMIN  │ Visão geral (totais)      │
├────────┼──────────────────┼────────┼───────────────────────────┤
│ GET    │ /dashboard/aluno │ ALUNO  │ Histórico de participação │
└────────┴──────────────────┴────────┴───────────────────────────┘

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Fluxo de Autenticação e Autorização

sequenceDiagram
participant C as Client
participant F as JwtAuthFilter
participant SC as SecurityContext
participant Ctrl as Controller

      C->>F: Request + Authorization: Bearer <token>
      F->>F: Extrair e validar JWT (assinatura + expiração)
      alt Token válido
          F->>SC: Setar Authentication (userId, role)
          SC->>Ctrl: Requisição prossegue
          Ctrl->>Ctrl: @PreAuthorize verifica papel
      else Token inválido/ausente
          F->>C: 401 Unauthorized
      end

Detalhes:

- Access Token: JWT assinado com HMAC-SHA256, expira em 15 min, payload: {sub: userId, role: ROLE_X}
- Refresh Token: UUID opaco salvo no banco, expira em 7 dias, revogável
- JwtAuthFilter (OncePerRequestFilter): intercepta todas as requisições exceto /auth/**
- SecurityFilterChain: configuração via HttpSecurity (sem WebSecurityConfigurerAdapter)
- Senha: BCrypt fator 12
- JWT Secret: via variável de ambiente JWT_SECRET

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Tratamento Global de Exceções

GlobalExceptionHandler (@RestControllerAdvice)
├── MethodArgumentNotValidException → 400 + campo:mensagem
├── ResourceNotFoundException → 404
├── BusinessException → 409 (conflito de horário, turma cheia, etc.)
├── AccessDeniedException → 403
├── AuthenticationException → 401
└── Exception (genérica) → 500 (sem stack trace, mensagem genérica)

Response padrão de erro:

{
"status": 400,
"erro": "Validação falhou",
"detalhes": { "titulo": "Título é obrigatório" },
"timestamp": "2026-06-16T10:00:00"
}

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Validações de Entrada (Bean Validation)

┌────────────────────┬─────────────────────────────────────────────────────────────────────────────────┐
│ DTO                │ Validações                                                                                         │
├──────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ RegisterRequest          │ nome @NotBlank @Size(max=100), email @Email @NotBlank, senha @Size(min=8)                          │
├──────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ LoginRequest             │ email @NotBlank @Email, senha @NotBlank                                                            │
├──────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ CriarEventoRequest       │ titulo @NotBlank @Size(max=100), data @NotNull @Future, vagas @Min(1) @Max(500)                    │
├──────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ CriarTurmaRequest        │ nome @NotBlank, dataHoraInicio/Fim @NotNull @Future, vagas @Min(1) @Max(500), professorId @NotNull │
├──────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ RegistrarPresencaRequest │ lista de {inscricaoId, presente}                                                                   │
└──────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────────────┘

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task Breakdown

Task 1: Configuração do projeto e infraestrutura base

Objetivo: Configurar o application.yaml com PostgreSQL, adicionar dependência jjwt 0.13.0 ao pom.xml, trocar MySQL connector por PostgreSQL, criar arquivo .env.example e configurar as propriedades via variáveis de ambiente.

Orientação:

- Trocar mysql-connector-j por org.postgresql:postgresql no pom.xml
- Adicionar jjwt-api, jjwt-impl, jjwt-jackson versão 0.13.0
- Configurar application.yaml com placeholders ${DB_URL}, ${JWT_SECRET}, etc.
- Criar .env.example na raiz do backend

Testes: Aplicação sobe sem erros com H2 (perfil test) ou com PostgreSQL local.

Demo: Projeto compila e inicia com ./mvnw spring-boot:run sem erros.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 2: Entidades JPA e Enums

Objetivo: Criar todas as entidades (Usuario, Evento, Turma, Inscricao, RefreshToken) com seus relacionamentos JPA e os enums (Role, StatusEvento, StatusTurma, StatusInscricao).

Orientação:

- Usar @Entity, @ManyToOne, @OneToMany conforme diagrama ER
- Usar Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
- Campos de auditoria: criadoEm com @CreationTimestamp
- Unique constraints: email em Usuario, (alunoId + turmaId) em Inscricao

Testes: Teste de unidade validando que o schema é gerado corretamente (JPA metamodel).

Demo: Schema gerado automaticamente no banco (ddl-auto=update) com todas as tabelas e relações.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 3: Repositories

Objetivo: Criar interfaces Spring Data JPA para cada entidade com queries customizadas necessárias.

Orientação:

- UsuarioRepository: findByEmail(String)
- EventoRepository: busca com filtros (status, data)
- TurmaRepository: findByEventoId(Long), findByProfessorId(Long)
- InscricaoRepository: findByAlunoId(Long), findByTurmaIdAndStatus(...), countByTurmaIdAndStatus(...), validação de conflito de horário via @Query JPQL
- RefreshTokenRepository: findByToken(String), deleteByUsuarioId(Long)

Testes: Testes de integração com banco H2 para as queries customizadas.

Demo: Queries executam corretamente contra H2 em teste.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 4: Security — JWT e autenticação

Objetivo: Implementar toda a camada de segurança: JwtService (gerar/validar tokens), JwtAuthFilter, SecurityConfig, UserDetailsService customizado, AuthenticationEntryPoint.

Orientação:

- JwtService: gerar access token (15min) com claims {sub, role}, validar assinatura e expiração
- JwtAuthFilter extends OncePerRequestFilter: extrair token do header, validar, setar SecurityContext
- SecurityConfig: SecurityFilterChain com endpoints públicos (/auth/**, GET /eventos/**), CSRF desabilitado, stateless session
- CustomUserDetailsService carrega por email
- AuthEntryPoint retorna 401 JSON padronizado

Testes: Testes unitários do JwtService (geração, validação, expiração). Teste de integração com Spring Security Test para verificar filtro.

Demo: Endpoints protegidos retornam 401 sem token; com token válido, requisição passa.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 5: Auth Controller (register, login, refresh, logout)

Objetivo: Implementar o AuthController e AuthService com os 4 endpoints de autenticação.

Orientação:

- POST /auth/register: cria usuário com papel ALUNO, hash BCrypt(12), retorna 201
- POST /auth/login: valida credenciais, gera access + refresh token, retorna ambos
- POST /auth/refresh: valida refresh token (não revogado, não expirado), gera novo access token
- POST /auth/logout: revoga refresh token no banco
- DTOs de request com Bean Validation; DTOs de response sem dados sensíveis

Testes: Testes de integração cobrindo: registro com sucesso, registro com email duplicado (409), login com credenciais corretas, login com senha errada (401), refresh com token válido, refresh com token revogado (401), logout.

Demo: Fluxo completo funcional via Swagger/Postman: registrar → login → usar access token → refresh → logout.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 6: GlobalExceptionHandler e exceções customizadas

Objetivo: Criar o tratamento centralizado de erros com exceções de domínio.

Orientação:

- ResourceNotFoundException, BusinessException, ConflictException
- GlobalExceptionHandler com handlers para: validação, recurso não encontrado, conflito, acesso negado, auth, exceção genérica
- Resposta padronizada: {status, erro, detalhes, timestamp}
- Nunca expor stack trace ou mensagens internas

Testes: Testes unitários do handler. Teste de integração verificando que erros retornam o formato padronizado.

Demo: Requisição com body inválido retorna 400 com mapa campo→mensagem; recurso inexistente retorna 404 limpo.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 7: CRUD de Eventos

Objetivo: Implementar EventoController e EventoService com CRUD completo protegido por roles.

Orientação:

- POST /eventos — ADMIN: cria evento
- GET /eventos — Público: lista com paginação e filtros (status, data)
- GET /eventos/{id} — Público: detalha evento com turmas
- PUT /eventos/{id} — ADMIN: edita
- DELETE /eventos/{id} — ADMIN: muda status para CANCELADO (soft delete)
- DTOs separados para request e response
- @PreAuthorize nos endpoints protegidos

Testes: Testes de integração com autenticação mockada (ADMIN cria, anônimo lista, ALUNO recebe 403 ao criar).

Demo: CRUD completo de eventos funcional via Swagger com controle de acesso.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 8: CRUD de Turmas

Objetivo: Implementar TurmaController e TurmaService vinculados a eventos.

Orientação:

- POST /eventos/{eventoId}/turmas — ADMIN: cria turma vinculada, valida que professor existe e evento existe
- GET /eventos/{eventoId}/turmas — Público
- PUT /turmas/{id} — ADMIN
- DELETE /turmas/{id} — ADMIN (soft delete)
- GET /turmas/{id}/inscritos — ADMIN ou PROFESSOR da turma

Testes: Testes cobrindo criação com evento inexistente (404), professor inexistente (404), listagem por evento.

Demo: Criar turmas em um evento, listar, editar, cancelar.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 9: Inscrições com validação de conflito e lista de espera

Objetivo: Implementar InscricaoController e InscricaoService com lógica de negócio completa.

Orientação:

- POST /turmas/{turmaId}/inscricoes — ALUNO: valida (turma aberta, não duplicada, sem conflito de horário, vagas); se sem vaga → LISTA_ESPERA com posição
- DELETE /inscricoes/{id} — ALUNO (própria): cancela e promove automaticamente o primeiro da fila
- GET /inscricoes/minhas — ALUNO: lista próprias inscrições
- Validação de conflito: JPQL que verifica sobreposição de horário com turmas já inscritas
- Promoção automática: ao cancelar, buscar primeira inscrição com status LISTA_ESPERA da turma, mudar para CONFIRMADA

Testes: Testes cobrindo: inscrição normal, turma cheia (vai para espera), conflito de horário (409), cancelamento + promoção automática, inscrição duplicada (409).

Demo: Fluxo completo: inscrever, tentar conflito (erro), lotar turma, próximo vai para espera, cancelar → promovido.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 10: Controle de Presença

Objetivo: Implementar endpoints de registro de presença em batch.

Orientação:

- PUT /turmas/{turmaId}/presenca — PROFESSOR (da turma) ou ADMIN: recebe lista [{inscricaoId, presente}], atualiza campo presente nas inscrições
- GET /turmas/{turmaId}/presenca — PROFESSOR (da turma) ou ADMIN: retorna lista de inscritos com status de presença
- Validar que o professor autenticado é o responsável pela turma (ownership check)

Testes: Teste com professor da turma (sucesso), professor de outra turma (403), aluno tentando (403).

Demo: Professor registra presença em batch; lista atualizada reflete corretamente.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 11: Dashboard e Histórico

Objetivo: Implementar endpoints de dashboard para Admin e histórico para Aluno.

Orientação:

- GET /dashboard/admin — ADMIN: total de eventos, turmas, inscrições, taxa de ocupação
- GET /dashboard/aluno — ALUNO: turmas inscritas, presença, histórico de participação
- Queries agregadas via JPQL ou @Query nativa

Testes: Teste verificando que dados retornados correspondem ao estado do banco.

Demo: Admin vê números consolidados; aluno vê seu histórico.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 12: Audit Logger e refinamentos de segurança

Objetivo: Implementar o AuditLogger e integrar aos pontos críticos do sistema.

Orientação:

- Componente que loga ações conforme tabela do SECURITY.md (login ok, login falho, acesso negado, criação de evento, cancelamento de inscrição, alteração de papel)
- Integrar no AuthService (login), no AccessDeniedHandler, nos services de evento e inscrição
- Logger dedicado "AUDIT" com formato estruturado

Testes: Verificar que logs são gerados nos cenários esperados (capturar output do logger).

Demo: Ao fazer login ou ter acesso negado, log de auditoria aparece no console.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 13: DataSeeder para desenvolvimento

Objetivo: Criar classe DataSeeder que popula o banco com dados fictícios conforme SECURITY.md §11.

Orientação:

- Ativado apenas com perfil dev (@Profile("dev"))
- Cria os 3 usuários demo (admin, professor, aluno) com senhas hasheadas
- Cria 3 eventos com turmas, 10 alunos, inscrições, 1 turma com lista de espera ativa
- Implementa CommandLineRunner

Testes: Teste de integração verificando que o seed executa sem erros.

Demo: Ao iniciar com --spring.profiles.active=dev, banco está populado e pronto para uso.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

Task 14: Documentação OpenAPI (Swagger)

Objetivo: Configurar SpringDoc OpenAPI com autenticação JWT e descrições dos endpoints.

Orientação:

- OpenApiConfig: título, descrição, versão, security scheme Bearer JWT
- @Operation e @ApiResponse nos controllers para documentação rica
- Swagger UI acessível em /swagger-ui.html

Testes: Verificar que /v3/api-docs retorna JSON válido e /swagger-ui.html carrega.

Demo: Swagger UI funcional com botão "Authorize" para JWT, todos os endpoints listados e testáveis.

──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────