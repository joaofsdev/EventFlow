<div align="center">

# EventFlow — Segurança da Informação

**Sistema de Gestão de Eventos Acadêmicos**

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4479A1?style=for-the-badge&logo=postgresql&logoColor=white)

*Disciplina: Segurança da Informação · Projeto P09-B*
*Centro Universitário Católica de Santa Catarina — Joinville, SC*

---

[Sobre o Sistema](#1-sobre-o-sistema) •
[Autenticação](#2-autenticação--jwt) •
[Controle de Acesso](#3-controle-de-acesso--rbac) •
[SQL Injection](#4-proteção-contra-sql-injection) •
[Proteção de Dados](#5-proteção-de-dados-dos-usuários) •
[Segredos](#6-gestão-de-segredos) •
[Validação](#7-validação-no-servidor) •
[Logs de Auditoria](#8-logs-de-auditoria) •
[Riscos](#9-vulnerabilidades-e-riscos-conhecidos) •
[Resposta a Incidente](#10-plano-de-resposta-a-incidente) •
[Demo](#11-dados-fictícios-para-demonstração) •
[Resumo](#12-resumo--mapa-de-segurança)

</div>

---

## 1. Sobre o Sistema

O **EventFlow** é uma plataforma web para gestão de eventos acadêmicos — minicursos, oficinas e workshops. O sistema centraliza o controle de turmas, vagas, inscrições e presença, com acesso segmentado por perfil de usuário.

### 1.1 Problema que o Sistema Resolve

A gestão de eventos acadêmicos é frequentemente realizada de forma manual, com planilhas avulsas, Google Forms e WhatsApp. Esse modelo apresenta problemas críticos:

- Ausência de controle centralizado de vagas e inscrições
- Risco de inscrições duplicadas ou conflitantes
- Dificuldade no registro e auditoria de presença
- Inexistência de histórico estruturado de participação por aluno
- Nenhuma garantia de segurança ou privacidade sobre os dados dos participantes

O EventFlow resolve esses problemas automatizando o fluxo completo: inscrição, controle de vagas, lista de espera, registro de presença e geração de histórico.

### 1.2 Público-Alvo

| Perfil | Responsabilidades |
|---|---|
| **Admin** | Gerencia eventos, turmas, vagas, usuários e permissões do sistema |
| **Professor** | Gerencia suas turmas, registra e acompanha presença dos alunos |
| **Aluno** | Realiza inscrições, consulta agenda e acessa histórico de participação |

### 1.3 Relevância da Segurança

Por tratar dados pessoais de alunos (nome, matrícula, histórico de participação), o EventFlow está sujeito às obrigações da **Lei Geral de Proteção de Dados (LGPD — Lei 13.709/2018)**. Isso torna a segurança da informação um requisito legal, não apenas técnico.

---

## 2. Autenticação — JWT

A autenticação no EventFlow é baseada em tokens JWT. Após o login, o usuário recebe um **Access Token** de curta duração e um **Refresh Token** de longa duração, eliminando a necessidade de trafegar credenciais a cada requisição.

### 2.1 Fluxo de Autenticação

```
1. Usuário envia email + senha via POST /auth/login
2. Backend valida as credenciais contra o banco de dados
3. Backend gera:
     - Access Token  (JWT, expiração: 15 minutos)
     - Refresh Token (UUID opaco, expiração: 7 dias, salvo no banco)
4. Frontend armazena os tokens e envia o Access Token em cada requisição:
     Authorization: Bearer <access_token>
5. No logout, o Refresh Token é invalidado no banco (revogação)
```

### 2.2 Boas Práticas

- **Access Token com expiração curta (15 min):** limita a janela de exposição em caso de vazamento
- **Payload do JWT contém apenas ID do usuário e papel (ROLE)** — nenhum dado sensível
- **Refresh Token invalidado no logout**, impedindo reuso após encerramento de sessão
- **Senha nunca trafega** após o login inicial

### 2.3 Exemplo de Payload JWT

```json
{
  "sub": "42",
  "role": "ROLE_ALUNO",
  "iat": 1718000000,
  "exp": 1718000900
}
```

---

## 3. Controle de Acesso — RBAC

O sistema adota o modelo de **Role-Based Access Control**. A autorização é aplicada no backend via anotações do Spring Security, garantindo que nenhuma operação sensível seja executada sem a devida permissão.

### 3.1 Aplicação no Backend

```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> criarEvento(...) { ... }

@PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
public ResponseEntity<?> registrarPresenca(...) { ... }

@PreAuthorize("hasRole('ALUNO')")
public ResponseEntity<?> realizarInscricao(...) { ... }
```

### 3.2 Frontend vs. Backend

> ⚠️ **A proteção de rotas no frontend é apenas UX** — esconder botões e redirecionar páginas não impede acesso indevido. Qualquer usuário com conhecimento técnico pode chamar endpoints diretamente via Postman ou cURL.
>
> A **autorização real é garantida exclusivamente pelo backend**, que valida o token JWT e o papel do usuário a cada requisição.

### 3.3 Matriz de Permissões

| Operação | Admin | Professor | Aluno |
|---|:---:|:---:|:---:|
| Criar / editar eventos | ✅ | ❌ | ❌ |
| Registrar presença | ✅ | ✅ | ❌ |
| Realizar inscrição | ✅ | ❌ | ✅ |
| Ver dashboard geral | ✅ | ❌ | ❌ |
| Consultar histórico próprio | ✅ | ✅ | ✅ |

---

## 4. Proteção contra SQL Injection

SQL Injection ocorre quando um atacante insere comandos SQL em campos de entrada para manipular consultas ao banco de dados. O EventFlow mitiga essa vulnerabilidade **estruturalmente** por meio do uso de JPA/Hibernate com Prepared Statements.

### 4.1 Como o JPA Protege

Ao usar Spring Data JPA, as queries são compiladas separadamente dos parâmetros. O banco recebe a estrutura da query e os dados como entidades distintas, tornando impossível que um input malicioso seja interpretado como código SQL.

### 4.2 Código Seguro vs. Vulnerável

```java
// ✅ SEGURO — JPA gera Prepared Statement automaticamente
repository.findByEmailAndStatus(email, status);

// ✅ SEGURO — JPQL com bind parameter
@Query("SELECT u FROM Usuario u WHERE u.email = :email")
Optional<Usuario> findByEmail(@Param("email") String email);

// ❌ VULNERÁVEL — concatenação direta de String
@Query("SELECT u FROM Usuario u WHERE u.email = '" + email + "'")
```

### 4.3 Exemplo de Ataque Bloqueado

Um atacante que insira o valor abaixo em um campo de login:

```
' OR '1'='1
```

Com Prepared Statement, esse valor é tratado como **dado literal**, e não como código SQL. A query não retorna nenhum resultado, pois nenhum usuário possui exatamente esse email.

---

## 5. Proteção de Dados dos Usuários

### 5.1 Armazenamento Seguro de Senhas — BCrypt

Senhas **nunca são armazenadas em texto puro**. O EventFlow utiliza o algoritmo BCrypt com fator de custo 12, que aplica múltiplas rodadas de hashing com salt automático.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // Strength 12 = 2^12 iterações de hash
    return new BCryptPasswordEncoder(12);
}
```

**Por que BCrypt e não MD5/SHA?**

- MD5 e SHA são algoritmos rápidos — ideais para checksums, inadequados para senhas
- BCrypt é **lento por design**, tornando ataques de força bruta computacionalmente inviáveis
- O **salt automático** garante que dois usuários com a mesma senha tenham hashes distintos, eliminando ataques por rainbow tables

### 5.2 Tabela de Ameaças e Mitigações

| Ameaça | Mitigação Implementada |
|---|---|
| Senha em texto puro no banco | BCrypt com salt automático (fator 12) |
| Dados em trânsito interceptados | HTTPS / TLS obrigatório em produção |
| Acesso a dados de outro usuário | Validação de ownership: `userId` do recurso `==` `sub` do token |
| Exposição de stack trace na API | `GlobalExceptionHandler` — nunca retorna erros internos ao cliente |
| Dados sensíveis na resposta da API | DTOs — nunca retornar a entidade JPA diretamente |
| Token vazado com longa validade | Access Token expira em 15 minutos |

### 5.3 Boas Práticas com DTOs

Retornar diretamente a entidade JPA em uma resposta da API é um erro grave de segurança, pois pode expor campos internos como o hash da senha.

```java
// ❌ ERRADO — pode expor hash da senha e campos internos
return ResponseEntity.ok(usuarioRepository.findById(id));

// ✅ CORRETO — retorna apenas o necessário
UsuarioDTO dto = new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getEmail());
return ResponseEntity.ok(dto);
```

---

## 6. Gestão de Segredos

Credenciais, chaves e configurações sensíveis **nunca são commitadas no repositório**. O projeto utiliza variáveis de ambiente carregadas via arquivo `.env`, que está listado no `.gitignore`.

### 6.1 Variáveis de Ambiente

O repositório contém um arquivo `.env.example` com todas as chaves necessárias, sem valores reais:

```env
# Banco de Dados
DB_URL=jdbc:postgresql://localhost:5432/eventflow
DB_USERNAME=
DB_PASSWORD=

# JWT
JWT_SECRET=
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000

# Servidor
SERVER_PORT=8080
```

### 6.2 Configuração no Spring Boot

```properties
# application.properties — lê do ambiente, nunca hardcoded
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```

> ⚠️ O arquivo `.env` real está no `.gitignore`. Qualquer segredo encontrado no histórico do Git deve ser considerado comprometido e rotacionado imediatamente.

---

## 7. Validação no Servidor

A validação de dados é realizada **obrigatoriamente no backend**, independentemente de qualquer validação existente no frontend. O frontend pode ser contornado por qualquer cliente HTTP.

### 7.1 Bean Validation com Spring

```java
public class CriarEventoRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 100, message = "Título deve ter no máximo 100 caracteres")
    private String titulo;

    @NotNull(message = "Data é obrigatória")
    @Future(message = "A data do evento deve ser futura")
    private LocalDate data;

    @Min(value = 1, message = "Número mínimo de vagas é 1")
    @Max(value = 500, message = "Número máximo de vagas é 500")
    private int vagas;
}

// Controller — @Valid dispara a validação automaticamente
@PostMapping("/eventos")
public ResponseEntity<?> criar(@Valid @RequestBody CriarEventoRequest request) { ... }
```

### 7.2 Tratamento Global de Erros

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        // Retorna erros de validação sem expor stack trace interno
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
          .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
```

---

## 8. Logs de Auditoria

Ações relevantes são registradas para garantir rastreabilidade. Os logs incluem: quem executou a ação, qual recurso foi afetado, quando ocorreu e o resultado (sucesso ou falha).

### 8.1 Eventos Auditados

| Evento | Nível | Informações Registradas |
|---|---|---|
| Login bem-sucedido | `INFO` | userId, IP, timestamp |
| Falha de login | `WARN` | email tentado, IP, timestamp |
| Acesso negado (403) | `WARN` | userId, endpoint tentado, timestamp |
| Criação de evento | `INFO` | userId, eventId, timestamp |
| Cancelamento de inscrição | `INFO` | userId, inscricaoId, timestamp |
| Alteração de perfil de usuário | `INFO` | adminId, userId afetado, papel anterior, papel novo |

### 8.2 Implementação

```java
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    public void logAcao(String acao, Long userId, String detalhe) {
        log.info("[AUDITORIA] acao={} userId={} detalhe={} timestamp={}",
            acao, userId, detalhe, Instant.now());
    }

    public void logAcessoNegado(Long userId, String endpoint) {
        log.warn("[AUDITORIA] ACESSO_NEGADO userId={} endpoint={} timestamp={}",
            userId, endpoint, Instant.now());
    }
}
```

### 8.3 Regra de Acesso Negado Demonstrável

Um aluno autenticado que tente acessar `POST /eventos` (exclusivo de Admin) recebe:

```json
HTTP 403 Forbidden
{
  "erro": "Acesso negado",
  "mensagem": "Você não tem permissão para executar esta ação."
}
```

E o seguinte registro é gerado no log:

```
[AUDITORIA] ACESSO_NEGADO userId=42 endpoint=POST /eventos timestamp=2025-06-09T14:30:00Z
```

---

## 9. Vulnerabilidades e Riscos Conhecidos

Todo sistema possui limitações. A identificação honesta de riscos é parte essencial de uma postura de segurança madura.

| Vulnerabilidade / Risco | Severidade | Situação Atual | Mitigação Recomendada |
|---|:---:|---|---|
| Sem rate limiting nos endpoints de login | Alta | Não implementado | Implementar bloqueio após N tentativas (Spring Security ou filtro customizado) |
| Tokens JWT não são revogáveis individualmente | Média | Por design do JWT | Manter expiração curta (15 min); Refresh Token é revogável |
| Logs armazenados apenas localmente | Média | Sem agregação externa | Integrar com serviço centralizado (ex: Loki, CloudWatch) em produção |
| Sem CAPTCHA no formulário de login | Baixa | Não implementado | Adicionar para dificultar ataques automatizados |
| Ausência de testes de segurança automatizados | Média | Não implementado | Adicionar testes com Spring Security Test e análise estática (OWASP Dependency-Check) |
| HTTPS dependente de configuração de infraestrutura | Alta | Responsabilidade do deploy | Garantir TLS no servidor de produção ou usar proxy reverso (Nginx/Caddy) |

---

## 10. Plano de Resposta a Incidente

Em caso de incidente de segurança, o time deve seguir as etapas abaixo em ordem.

### Fase 1 — Contenção (0 a 2 horas)

```
1. Identificar o vetor do ataque (logs de auditoria, registros de acesso)
2. Revogar todos os Refresh Tokens ativos no banco de dados:
     UPDATE refresh_tokens SET revogado = true;
3. Alterar imediatamente as variáveis de ambiente comprometidas (JWT_SECRET, DB_PASSWORD)
4. Restartar a aplicação para invalidar todos os Access Tokens em circulação
5. Isolar o ambiente afetado se necessário (bloquear IP, desativar conta comprometida)
```

### Fase 2 — Análise (2 a 24 horas)

- Revisar os logs de auditoria para identificar o escopo do acesso indevido
- Identificar quais registros foram lidos, criados ou alterados
- Verificar se houve exfiltração de dados de usuários

### Fase 3 — Correção

- Corrigir a vulnerabilidade explorada e fazer novo deploy
- Alterar todas as credenciais e segredos mesmo que não confirmadamente comprometidos
- Documentar o incidente: vetor, impacto, ações tomadas e lições aprendidas

### Fase 4 — Comunicação

| Situação | Ação |
|---|---|
| Dados de alunos expostos | Notificar os usuários afetados e a instituição |
| Incidente com dados pessoais | Avaliar obrigação de notificação à ANPD (LGPD, Art. 48) |
| Brecha por dependência de terceiros | Abrir CVE ou notificar o mantenedor da biblioteca |

---

## 11. Dados Fictícios para Demonstração

O projeto inclui um script de seed (`DataSeeder.java`) que popula o banco com dados fictícios para facilitar testes e demonstrações.

### 11.1 Usuários de Demonstração

| Perfil | Email | Senha |
|---|---|---|
| Admin | `admin@eventflow.dev` | `Admin@1234` |
| Professor | `professor@eventflow.dev` | `Prof@1234` |
| Aluno | `aluno@eventflow.dev` | `Aluno@1234` |

> ⚠️ Esses dados existem **apenas no ambiente de desenvolvimento/demo**. O seed não é executado em produção.

### 11.2 Dados Gerados pelo Seed

- 3 eventos com turmas abertas (vagas disponíveis e esgotadas)
- 10 alunos fictícios com inscrições em diferentes turmas
- Registros de presença para demonstração do dashboard
- 1 turma com lista de espera ativa

---

## 12. Resumo — Mapa de Segurança

| Requisito de Segurança | Tecnologia / Abordagem | Seção |
|---|---|:---:|
| Autenticação stateless | JWT (Access Token 15 min + Refresh Token 7 dias) | §2 |
| Autorização por papel (RBAC) | Spring Security (`@PreAuthorize`) | §3 |
| Regra de dono do recurso | Validação de `userId` vs `sub` do token | §5.2 |
| Armazenamento de senhas | BCrypt com salt automático (fator 12) | §5.1 |
| Gestão de segredos | `.env` + `.gitignore` + `.env.example` | §6 |
| Validação no servidor | Bean Validation + `GlobalExceptionHandler` | §7 |
| Logs de auditoria | `AuditLogger` com rastreio de ações e acessos negados | §8 |
| Acesso negado demonstrável | HTTP 403 + log de auditoria | §8.3 |
| Proteção SQL Injection | JPA / Hibernate com Prepared Statements | §4 |
| Dados em trânsito | HTTPS / TLS (obrigatório em produção) | §5.2 |
| Exposição mínima de dados | DTOs em todas as respostas da API | §5.3 |
| Vulnerabilidades conhecidas | Tabela de riscos e mitigações recomendadas | §9 |
| Plano de resposta a incidente | 4 fases: Contenção → Análise → Correção → Comunicação | §10 |
| Dados fictícios para demo | Seed com usuários, eventos e inscrições | §11 |
| Conformidade LGPD | Autenticação + RBAC + notificação ANPD (Art. 48) | §3, §10 |

---

<div align="center">
  <sub>EventFlow · Segurança da Informação · P09-B</sub>
  <br/>
  <sub>João Francisco · Iago Koch · William Vodzinsky · Caio Rosa · Pedro Israel</sub>
</div>
