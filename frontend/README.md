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

Rotas usadas: `GET /api/produtos` e `GET /api/produtos/{id}`. O filtro por categoria usa o nome: `Bebidas`, `Tabacaria`, `Narguilé`, `Acessórios`.

Se a API estiver fora do ar, a página de produtos mostra dados de exemplo e um aviso. Em produção o tempo de espera é maior (até 75 s) porque o servidor gratuito dorme e leva cerca de 1 minuto para acordar; nesse meio-tempo aparece o aviso "Acordando o servidor". Para desligar os dados de exemplo, mude `usarDadosExemploSeApiOffline` para `false`.

O CORS já está liberado na API para `http://localhost:4200` e para o site no Render.

## Personalizar

- Endereço, horário, WhatsApp e Instagram: `src/app/core/loja.ts`
- Fotos: `public/img/`
- Cores e fontes: variáveis em `src/styles.css`

## Estrutura

```
src/app
├── core        models, services, dados de exemplo, info da loja
├── shared      header, footer, logo, loja-info, produto-card, category-icon
└── pages       home, produtos, sobre, contato (lazy loading)
```
