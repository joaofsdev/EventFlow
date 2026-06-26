# Deploy — EventFlow Backend

## 1. Rodar localmente com Docker

### Subir os serviços

```bash
docker compose up --build
```

A API estará disponível em `http://localhost:8080`.

### Parar os serviços

```bash
docker compose down
```

### Resetar o banco de dados

```bash
docker compose down -v
```

---

## 2. Deploy no Render (produção)

### Passo 1 — Criar banco PostgreSQL

1. No dashboard do Render, clique em **New → PostgreSQL**
2. Escolha o plano Free (90 dias)
3. Anote as credenciais geradas: host, porta, usuário, senha e nome do banco

### Passo 2 — Criar Web Service

1. Clique em **New → Web Service**
2. Conecte o repositório Git do projeto
3. Configure:
   - **Root Directory:** `backend/`
   - **Runtime:** Docker
   - **Branch:** `main`

### Passo 3 — Configurar variáveis de ambiente

No painel do Web Service, em **Environment → Environment Variables**, adicione:

| Variável | Valor |
|----------|-------|
| `DB_URL` | `jdbc:postgresql://HOST:PORT/DBNAME` (dados do passo 1) |
| `DB_USERNAME` | Usuário gerado pelo Render |
| `DB_PASSWORD` | Senha gerada pelo Render |
| `JWT_SECRET` | String aleatória com no mínimo 32 caracteres |
| `JWT_EXPIRATION_MS` | `900000` (15 min) ou `86400000` (24h) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 dias) |
| `SPRING_PROFILES_ACTIVE` | `prod` |

> O Render injeta a variável `PORT` automaticamente — não é necessário configurá-la.

### Passo 4 — Deploy

O deploy acontece automaticamente a cada push na branch `main`.

---

## 3. Variáveis de ambiente

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `DB_URL` | URL JDBC de conexão com o PostgreSQL | `jdbc:postgresql://localhost:5432/eventflow` |
| `DB_USERNAME` | Usuário do banco de dados | `eventflow_user` |
| `DB_PASSWORD` | Senha do banco de dados | `eventflow_pass` |
| `JWT_SECRET` | Chave secreta para assinatura JWT (mín. 32 chars) | `minha-chave-secreta-muito-longa-123` |
| `JWT_EXPIRATION_MS` | Expiração do access token (ms) | `900000` |
| `JWT_REFRESH_EXPIRATION_MS` | Expiração do refresh token (ms) | `604800000` |
| `SPRING_PROFILES_ACTIVE` | Profile do Spring Boot | `prod` |
| `PORT` | Porta do servidor (injetada pelo Render) | `8080` |
