<div align="center">
  <h1>EventFlow</h1>
  <p>Sistema de Gestão de Eventos, Minicursos e Oficinas</p>

  <p>
    <img alt="Java" src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
    <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white"/>
    <img alt="React" src="https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB"/>
    <img alt="MySQL" src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/>
  </p>

  <p>
    <img alt="Status" src="https://img.shields.io/badge/status-em%20desenvolvimento-yellow?style=flat-square"/>
    <img alt="Projeto" src="https://img.shields.io/badge/projeto-P09--B-blue?style=flat-square"/>
    <img alt="Licença" src="https://img.shields.io/badge/licença-MIT-green?style=flat-square"/>
  </p>

  <p>
    <a href="#sobre">Sobre</a> •
    <a href="#funcionalidades">Funcionalidades</a> •
    <a href="#tecnologias">Tecnologias</a> •
    <a href="#como-executar">Como Executar</a> •
    <a href="#estrutura">Estrutura</a> •
    <a href="docs/SECURITY.md">🔒 Segurança</a> •
    <a href="#equipe">Equipe</a>
  </p>
</div>

---

## Sobre

O **EventFlow** é uma plataforma web para gestão de eventos educacionais — minicursos, oficinas e workshops — no contexto acadêmico. O sistema centraliza o controle de turmas, vagas, inscrições e presença, com acesso separado por perfil de usuário.

> **Disciplina:** Engenharia de Software — Projeto P09-B

---

## Funcionalidades

<details>
<summary><strong>📅 Gestão de Eventos e Turmas</strong></summary>

- [ ] Cadastro de eventos com título, descrição, data, local e carga horária
- [ ] Criação de turmas vinculadas a um evento, com limite de vagas
- [ ] Controle de status: aberta, encerrada ou cancelada
- [ ] Visualização pública da grade de eventos disponíveis

</details>

<details>
<summary><strong>📝 Inscrições</strong></summary>

- [ ] Inscrição de alunos com validação de vagas disponíveis
- [ ] Cancelamento dentro do prazo configurado
- [ ] Prevenção de inscrições duplicadas em turmas conflitantes
- [ ] Lista de espera automática quando as vagas esgotam

</details>

<details>
<summary><strong>✅ Controle de Presença</strong></summary>

- [ ] Registro de presença por aula ou sessão
- [ ] Edição de registros pelo professor responsável
- [ ] Cálculo automático de frequência por turma
- [ ] Exportação de lista de presença

</details>

<details>
<summary><strong>📊 Relatórios e Dashboards</strong></summary>

- [ ] Dashboard com visão geral de eventos e inscrições (Admin)
- [ ] Relatório de presença por turma e por aluno
- [ ] Histórico de participação acessível ao próprio aluno

</details>

<details>
<summary><strong>🔒 Autenticação e Segurança</strong></summary>

- [ ] Cadastro e login com autenticação via JWT
- [ ] Controle de acesso por perfil (Admin, Professor, Aluno)
- [ ] Proteção de rotas no frontend e no backend

</details>

---

## Perfis de Usuário

| Perfil        | Descrição                                                       |
| ------------- | --------------------------------------------------------------- |
| **Admin**     | Gerencia eventos, turmas, vagas, usuários e permissões          |
| **Professor** | Gerencia suas turmas, registra e acompanha presença             |
| **Aluno**     | Realiza inscrições, consulta agenda e histórico de participação |

---

## Tecnologias

| Camada         | Tecnologia                             |
| -------------- | -------------------------------------- |
| Frontend       | React.js · React Router · Axios · Vite |
| Backend        | Java · Spring Boot · Spring Security   |
| Banco de Dados | PostgreSQL                             |
| Autenticação   | JWT                                    |
| Build          | Maven (backend) · npm (frontend)       |

---

## Como Executar

### Pré-requisitos

- [Java 17+](https://adoptium.net/)
- [Node.js 18+](https://nodejs.org/)
- [PostgreSQL 15+](https://www.postgresql.org/)
- [Maven](https://maven.apache.org/)

### Backend

```bash
cd backend

# Configure o banco de dados em src/main/resources/application.properties
# spring.datasource.url=jdbc:postgresql://localhost:5432/eventflow
# spring.datasource.username=seu_usuario
# spring.datasource.password=sua_senha

./mvnw spring-boot:run
```

> API disponível em `http://localhost:8080`

### Frontend

```bash
cd frontend

npm install
npm run dev
```

> Aplicação disponível em `http://localhost:5173`

---

## Estrutura

```
EventFlow/
├── backend/                  # API Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/         # Código-fonte Java
│   │   │   └── resources/    # application.properties
│   │   └── test/
│   └── pom.xml
├── frontend/                 # Aplicação React
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   └── services/
│   └── package.json
└── README.md
```

---

## Segurança

A documentação completa de segurança do sistema está em [`docs/SECURITY.md`](docs/SECURITY.md), cobrindo:

- Autenticação JWT e gestão de tokens
- Controle de acesso por perfil (RBAC)
- Proteção contra SQL Injection
- Armazenamento seguro de senhas (BCrypt)
- Gestão de segredos e variáveis de ambiente
- Validação no servidor
- Logs de auditoria
- Vulnerabilidades conhecidas e plano de resposta a incidente

---

## Equipe

| Nome              | GitHub                                     |
| ----------------- | ------------------------------------------ |
| João Francisco    | [@joaofsdev](https://github.com/joaofsdev) |
| Iago Koch         | —                                          |
| William Vodzinsky | —                                          |
| Caio Rosa         | —                                          |
| Pedro Israel      | —                                          |

---

<div align="center">
  <sub>EventFlow · Engenharia de Software</sub>
</div>
