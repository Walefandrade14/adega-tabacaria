# Adega & Tabacaria LouD Feliz no Simples

Sistema web de comércio para autônomo: vitrine/catálogo, pedidos online com delivery ou retirada, PDV de balcão e as regras de negócio RN01–RN09.

| Pasta | O que é |
|---|---|
| `backend/` | API REST em Spring Boot 4.1 (Java 17+), MySQL 8, Flyway, Spring Security com JWT, Swagger |
| `frontend/` | Site em Angular 19 (tema escuro vermelho/dourado) |
| `adega_tabacaria_schema.sql` | Modelo do banco (vira a migration `V1` da API) |
| `render.yaml` | Blueprint de deploy no Render (API + site) |
| `.github/workflows/ci.yml` | CI: testes da API contra MySQL, imagem Docker com smoke test, build do Angular e teste de ponta a ponta (Playwright) das telas de cadastro e do painel |

## Rodar local

Pré-requisitos: Java 17+, Node 20.11+/22/24 (LTS), MySQL 8 rodando em `localhost:3306`.

O padrão é usuário `root` e senha `root`. Se o seu MySQL usa outra senha, crie `backend/local.properties` (ele fica fora do Git) com:

```properties
spring.datasource.password=SUA_SENHA
```

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080` e cria o banco `adega_tabacaria` sozinha (Flyway cria as tabelas e os dados iniciais). Documentação interativa: `http://localhost:8080/swagger-ui.html`.

Login inicial do dono: usuário `dono`, senha `admin123` (mude com `ADMIN_USER` / `ADMIN_PASSWORD`).

Telas do site:

| Rota | Quem usa | O que faz |
|---|---|---|
| `/cadastro` | Cliente | Cria a conta (nome, CPF, nascimento, e-mail, telefone, senha) |
| `/entrar` | Cliente | Login |
| `/minha-conta` | Cliente | Dados e endereços de entrega (CEP preenche sozinho e mostra se a loja entrega no bairro) |
| `/admin/entrar` | Funcionário | Login da equipe (link "Área do funcionário" no rodapé) |
| `/admin` | Dono / estoque | Cadastro de produtos, ajuste rápido de estoque, ativar/desativar e categorias |

```bash
cd frontend
npm install
npm start                       # http://localhost:4200
```

Testes da API (usam o banco `adega_tabacaria_test`):

```bash
cd backend
./mvnw test
```

## Endpoints principais

Público: `GET /api/produtos`, `GET /api/produtos/{id}`, `GET /api/categorias`, `GET /api/combos`, `GET /api/loja/status`, `GET /api/entrega/regioes`, `GET /api/entrega/taxa?bairro=`.

Autenticação: `POST /api/auth/cadastro`, `POST /api/auth/login`, `POST /api/auth/funcionarios/login` — devolvem um token JWT para usar em `Authorization: Bearer <token>`.

Cliente: endereços em `/api/clientes/me/enderecos`, `POST /api/carrinho/simular`, `POST /api/pedidos`, `POST /api/pedidos/{id}/pagamentos`, `POST /api/pagamentos/{id}/confirmar-pix`, `POST /api/pedidos/{id}/cancelar`.

Funcionários (`/api/admin/**`): produtos, estoque, categorias, fornecedores, combos, horários, taxas de entrega, configurações, equipe, pedidos (status e cancelamento) e PDV (`/api/admin/pdv/verificar`, `/api/admin/pdv/vendas`).

## Regras de negócio na API

| Regra | Onde é aplicada |
|---|---|
| RN01 Maioridade | Pedido online bloqueia item restrito para menor de 18 (data de nascimento). No PDV, `verificar` devolve `exigeConferenciaDocumento` e a venda só fecha com `documentoConferido: true` |
| RN02 Estoque | Bloqueia na criação do pedido e baixa o estoque de forma atômica só na aprovação do pagamento — duas pessoas não levam a mesma última unidade |
| RN03 Combos | Desconto percentual ou fixo aplicado sozinho quando o carrinho fecha o combo, rateado entre os itens |
| RN04 Horário | Intervalo que cruza a meia-noite (ex. 18:00–02:00); o pedido da 01:00 pertence ao dia anterior. Fuso America/Sao_Paulo |
| RN05 Mínimo delivery | Conta só o valor dos produtos (já com desconto); a taxa de entrega não entra. Valor configurável |
| RN06 Oferta especial | Limite por CPF por dia (padrão 3, ou o limite do produto). Vale para pedido online e para venda de balcão com CPF cadastrado. Prevalece sobre o combo |
| RN07 Pagamento | Pedido nasce ABERTO; cartão/dinheiro aprovam na hora, Pix fica pendente até a confirmação. Recusa mantém o pedido aberto sem mexer no estoque |
| RN08 Cancelamento | Cliente cancela até o pagamento confirmado (com estorno total). Depois da separação só o Dono ou o funcionário responsável, com estorno proporcional |
| RN09 Taxa por região | Taxa pelo bairro do endereço (sem acento/maiúscula) ou faixa de CEP; bairro fora da lista não recebe delivery |

O gateway de pagamento é simulado (`simularRecusa: true` força recusa no cartão).

## Deploy (grátis)

Banco MySQL no [Aiven](https://aiven.io/free-mysql-database) e API + site no [Render](https://render.com), pelo `render.yaml`.

1. **Aiven:** crie a conta, *Create service* → MySQL → plano **Free** → crie. Na página do serviço, copie a **Service URI** (`mysql://avnadmin:...@...:porta/defaultdb?ssl-mode=REQUIRED`).
2. **Render:** entre com o GitHub, *New* → *Blueprint* → escolha este repositório. Ele lê o `render.yaml` e pede:
   - `DATABASE_URL`: cole a Service URI do Aiven
   - `ADMIN_PASSWORD`: senha do usuário `dono` em produção
3. *Apply*. Em ~10 min ficam no ar:
   - API: `https://loud-feliz-api.onrender.com` (Swagger na raiz)
   - Site: `https://loud-feliz-web.onrender.com`

Se o Render der outro endereço para a API (nome já usado), troque a variável `API_URL` do serviço `loud-feliz-web` e faça *Manual Deploy*.

No plano grátis a API dorme após 15 min sem acesso e leva ~1 min para acordar; o site mostra um aviso enquanto isso. Cada push na `main` faz novo deploy.
