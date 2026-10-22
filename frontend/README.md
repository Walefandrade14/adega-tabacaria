# LouD Feliz no Simples — Front-end (Angular 19)

Front-end da Adega & Tabacaria, consumindo a API REST do projeto.

## Rodar

Requer Node.js 18.19+, 20.11+ ou 22+.

```bash
npm install
npm start
```

Abre em `http://localhost:4200`.

## API

URL base em `src/environments/environment.ts` (padrão `http://localhost:8080`).

| Método | Rota             | Uso                     |
|--------|------------------|-------------------------|
| GET    | `/produtos`      | Lista do catálogo       |
| GET    | `/produtos/{id}` | Detalhe de um produto   |

Formato esperado de cada produto (JSON padrão do Spring/JPA, camelCase):

```json
{
  "id": 1,
  "nome": "Whisky 12 anos 1L",
  "descricao": "Blended escocês",
  "preco": 189.9,
  "estoqueQuantidade": 12,
  "categoria": { "id": 1, "nome": "Bebidas" },
  "restritoMaiorIdade": true,
  "ofertaEspecial": false,
  "limiteDiarioOferta": null,
  "ativo": true,
  "imagemUrl": null
}
```

O filtro por categoria usa o nome: `Bebidas`, `Tabacaria`, `Narguilé`, `Acessórios`.

Se a API estiver fora do ar, a página de produtos mostra dados de exemplo e um aviso.
Para desligar isso, mude `usarDadosExemploSeApiOffline` para `false`.

No Spring Boot, libere o CORS para o Angular:

```java
@CrossOrigin(origins = "http://localhost:4200")
```

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
