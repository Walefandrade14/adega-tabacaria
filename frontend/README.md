# LouD Feliz no Simples — Front-end (Angular 19)

Site da Adega & Tabacaria, consumindo a API do projeto (`../backend`).

## Rodar

Requer Node.js 20.11+, 22 ou 24 (LTS recomendado).

```bash
npm install
npm start
```

Abre em `http://localhost:4200`.

## API

| Ambiente | Arquivo | URL base |
|---|---|---|
| Desenvolvimento (`npm start`) | `src/environments/environment.ts` | `http://localhost:8080/api` |
| Produção (`npm run build`) | `src/environments/environment.prod.ts` | `https://loud-feliz-api.onrender.com/api` |

No deploy, a variável `API_URL` sobrescreve a URL de produção (`node scripts/definir-api.mjs` roda antes do build).

Rotas da API usadas pelo site: catálogo (`/api/produtos`), autenticação (`/api/auth/*`), conta do cliente (`/api/clientes/me`, endereços), taxa de entrega (`/api/entrega/taxa`) e o cadastro do painel (`/api/admin/produtos`, `/api/admin/categorias`). O token de login fica no `localStorage` e vai no cabeçalho `Authorization` só para a API. O filtro por categoria usa o nome: `Bebidas`, `Tabacaria`, `Narguilé`, `Acessórios`.

Se a API estiver fora do ar, a página de produtos mostra dados de exemplo e um aviso. Em produção o tempo de espera é maior (até 75 s) porque o servidor gratuito dorme e leva cerca de 1 minuto para acordar; nesse meio-tempo aparece o aviso "Acordando o servidor". Para desligar os dados de exemplo, mude `usarDadosExemploSeApiOffline` para `false`.

O CORS já está liberado na API para `http://localhost:4200` e para o site no Render.

## Personalizar

- Endereço, horário, WhatsApp e Instagram: `src/app/core/loja.ts`
- Fotos: `public/img/`
- Cores e fontes: variáveis em `src/styles.css`

## Estrutura

```
src/app
├── core        auth (sessão, interceptor, guards), models, services, máscaras, dados de exemplo
├── shared      header, footer, logo, loja-info, produto-card, category-icon
└── pages       home, produtos, sobre, contato, cadastro, entrar, minha-conta, admin (lazy loading)
```

## Teste de ponta a ponta

`e2e/fluxo.py` (Playwright) cadastra um cliente, adiciona endereço, sai e entra de novo; depois entra como dono, cadastra e edita produto, mexe no estoque e cria categoria; por fim confere a página 404 e a sessão inválida (na área restrita volta para o login, na vitrine os produtos carregam sem o token). Roda no CI contra a API real com MySQL.

## Erros

- `core/erro-api.ts`: mensagem para o usuário a partir da resposta da API (campos, `detail`, sem conexão, tempo esgotado, 5xx).
- `core/auth/auth.interceptor.ts`: token só vai para a API (fora de `/auth`); 401 com token encerra a sessão, avisa e repete a chamada sem token.
- `core/erro-global.ts` + `shared/aviso-global`: `ErrorHandler` global com aviso e botão "Recarregar".
- `pages/nao-encontrada`: página 404.
