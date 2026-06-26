# Deploy — EventFlow Frontend (Vercel)

## 1. Pré-requisitos

- Conta na [Vercel](https://vercel.com)
- Repositório no GitHub com o projeto EventFlow
- URL pública do backend no Render já disponível (ex: `https://eventflow-21fb.onrender.com`)

---

## 2. Passo a passo na Vercel

1. Acesse [vercel.com](https://vercel.com) e faça login
2. Clique em **Add New → Project**
3. Clique em **Import Git Repository** e selecione o repositório do EventFlow
4. Configure o projeto:
   - **Root Directory:** `frontend/`
   - **Framework Preset:** Vite (detectado automaticamente)
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`
5. Em **Environment Variables**, adicione:
   - **Name:** `VITE_API_URL`
   - **Value:** URL pública do backend (ex: `https://eventflow-21fb.onrender.com`)
6. Clique em **Deploy**

---

## 3. Após o deploy

### URL pública
Após o deploy, a Vercel gera uma URL pública (ex: `https://eventflow-frontend.vercel.app`).
Ela aparece no dashboard do projeto na Vercel.

### Configurar CORS no backend
Para que o frontend consiga se comunicar com o backend, a URL da Vercel precisa ser adicionada nas origens permitidas do CORS no Spring Boot.

Procure o arquivo de configuração de CORS no backend (geralmente `CorsConfig.java` ou `WebConfig.java` em `src/main/java/.../config/`) e adicione a URL da Vercel na lista de origens permitidas:

```java
.allowedOrigins("https://SEU-PROJETO.vercel.app")
```

### Redeploy
Qualquer push na branch `main` dispara um novo deploy automaticamente na Vercel.

---

## 4. Variáveis de ambiente

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `VITE_API_URL` | URL base do backend (Render) | `https://eventflow-21fb.onrender.com` |

> **Importante:** Variáveis de ambiente no Vite precisam do prefixo `VITE_` para serem acessíveis no código do frontend.

---

## 5. Solução de problemas comuns

### Página em branco ao acessar rota diretamente
**Causa:** Falta do arquivo `vercel.json` com o rewrite para SPA.
**Solução:** Verificar que `frontend/vercel.json` existe com o conteúdo:
```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

### Erro de CORS no console do navegador
**Causa:** A URL da Vercel não está cadastrada como origem permitida no backend.
**Solução:** Adicionar a URL do frontend na configuração de CORS do Spring Boot (ver seção 3).

### Variável VITE_API_URL undefined em produção
**Causa:** A variável não foi configurada no painel da Vercel, ou foi criada sem o prefixo `VITE_`.
**Solução:** Verificar no dashboard da Vercel em **Settings → Environment Variables** se `VITE_API_URL` está definida corretamente. Após adicionar/alterar, é necessário fazer um novo deploy.

### Build falhando na Vercel
**Causa:** Root Directory não está configurado como `frontend/`.
**Solução:** No dashboard da Vercel, vá em **Settings → General → Root Directory** e defina como `frontend/`.
