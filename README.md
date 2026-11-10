# Adega & Tabacaria LouD Feliz no Simples

Sistema web de comércio para autônomo: vitrine/catálogo, pedidos online com delivery ou retirada, PDV de balcão e as regras de negócio RN01–RN09.

| Pasta | O que é |
|---|---|
| `backend/` | API REST em Spring Boot 4.1 (Java 17+), MySQL 8, Flyway, Spring Security com JWT, Swagger |
| `frontend/` | Site em Angular 19 (tema escuro vermelho/dourado) |
| `adega_tabacaria_schema.sql` | Modelo do banco (vira a migration `V1` da API) |
| `render.yaml` | Blueprint de deploy no Render (API + site) |
| `deploy/vm/` | Deploy numa VM própria (Azure etc.): Docker Compose com MySQL, API e Caddy com HTTPS |
| `.github/workflows/ci.yml` | CI: testes da API contra MySQL, imagem Docker com smoke test, build do Angular e teste de ponta a ponta (Playwright) do cadastro, da conta, da compra (carrinho → pedido → Pix) e do painel, no modo simulado e na stack da VM |

**No ar:** [loudfeliz.chilecentral.cloudapp.azure.com](https://loudfeliz.chilecentral.cloudapp.azure.com)

## Protótipo (Figma)

[Arquivo de design no Figma](https://www.figma.com/design/G2MQcwhF17Zy43fYk8hvSK/Adega-Tabacaria) com 25 telas (desktop e mobile) e os componentes. O protótipo é navegável, com estados de hover, sessão de cliente e de funcionário, modais e menu mobile:

| Fluxo | Começa em | Abrir |
|---|---|---|
| Site desktop | Home | [protótipo](https://www.figma.com/proto/G2MQcwhF17Zy43fYk8hvSK/Adega-Tabacaria?node-id=26-2&starting-point-node-id=26%3A2) |
| Conta do cliente | Entrar | [protótipo](https://www.figma.com/proto/G2MQcwhF17Zy43fYk8hvSK/Adega-Tabacaria?node-id=44-1496&starting-point-node-id=44%3A1496) |
| Painel do dono | Área do funcionário | [protótipo](https://www.figma.com/proto/G2MQcwhF17Zy43fYk8hvSK/Adega-Tabacaria?node-id=44-1697&starting-point-node-id=44%3A1697) |
| Mobile | Home | [protótipo](https://www.figma.com/proto/G2MQcwhF17Zy43fYk8hvSK/Adega-Tabacaria?node-id=29-1189&starting-point-node-id=29%3A1189) |

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
| `/cadastro` | Cliente | Cria a conta (nome, nascimento, e-mail, senha; CPF e telefone opcionais) |
| `/entrar` | Cliente | Login |
| `/minha-conta` | Cliente | Dados, troca de senha e endereços de entrega: criar, editar e remover (CEP preenche sozinho e mostra se a loja entrega no bairro) |
| `/carrinho` | Visitante / cliente | Itens com quantidade, entrega ou retirada, endereço, descontos, taxa e total calculados pela API; mostra a regra que bloqueia (RN01–RN09) |
| `/pedidos` | Cliente | Meus pedidos com o status de cada um |
| `/pedidos/:id` | Cliente | Andamento, pagamento (cartão, Pix com código copia e cola, dinheiro) e cancelamento |
| `/admin/entrar` | Funcionário | Login da equipe (link "Área do funcionário" no rodapé) |
| `/admin` | Dono / estoque | Cadastro de produtos, ajuste rápido de estoque, ativar/desativar, categorias, pedidos paginados (confirmar Pix, avançar status, cancelar com estorno) e, para o Dono, o Resumo de vendas |

Fluxo de compra: o botão **Adicionar** do card põe o produto no carrinho (guardado no navegador). No carrinho o cliente escolhe entrega ou retirada, a API simula o pedido a cada mudança e o botão só libera quando nenhuma regra bloqueia. Depois de criar o pedido ele paga na tela do pedido; no Pix, "Já paguei" simula a confirmação do banco. No painel, a aba **Pedidos** leva o pedido de Pago → Em separação → Saiu para entrega (ou direto para Concluído na retirada) → Concluído.

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

Cliente: `PUT /api/clientes/me` (nome e telefone), `PUT /api/clientes/me/senha`, endereços em `/api/clientes/me/enderecos` (GET, POST, PUT `/{id}`, DELETE `/{id}`), `POST /api/carrinho/simular`, `POST /api/pedidos`, `POST /api/pedidos/{id}/pagamentos`, `POST /api/pagamentos/{id}/confirmar-pix`, `POST /api/pedidos/{id}/cancelar`.

Funcionários (`/api/admin/**`): produtos, estoque, categorias, fornecedores, combos, horários, taxas de entrega, configurações, equipe, pedidos (`GET /api/admin/pedidos?status=PAGO&status=EM_SEPARACAO&pagina=0&tamanho=20`, status, cancelamento e `POST /api/admin/pagamentos/{id}/confirmar-pix`) e PDV (`/api/admin/pdv/verificar`, `/api/admin/pdv/vendas`). Só o Dono: `GET /api/admin/relatorios/resumo?de=2026-11-01&ate=2026-11-07` (faturamento, ticket médio, canais, faturamento por dia, mais vendidos e estoque baixo).

## Regras de negócio na API

| Regra | Onde é aplicada |
|---|---|
| RN01 Maioridade | Pedido online bloqueia item restrito para menor de 18 (data de nascimento). Pedido de entrega ou retirada com item +18 sai com `conferirDocumento: true`: o painel mostra "+18 · conferir documento" e o cliente é avisado para levar documento com foto. No PDV, `verificar` devolve `exigeConferenciaDocumento` e a venda só fecha com `documentoConferido: true` |
| RN02 Estoque | Bloqueia na criação do pedido e baixa o estoque de forma atômica na aprovação do pagamento. No Pix o estoque é reservado quando o código é gerado (se acabou, o Pix nem é gerado) e volta se o pedido for cancelado, expirar ou trocar para um cartão recusado — ninguém paga por item que acabou |
| RN03 Combos | Desconto percentual ou fixo aplicado sozinho quando o carrinho fecha o combo, rateado entre os itens |
| RN04 Horário | Intervalo que cruza a meia-noite (ex. 18:00–02:00); o pedido da 01:00 pertence ao dia anterior. Fuso America/Sao_Paulo |
| RN05 Mínimo delivery | Conta só o valor dos produtos (já com desconto); a taxa de entrega não entra. Valor configurável |
| RN06 Oferta especial | Limite por CPF por dia (padrão 3, ou o limite do produto). O CPF é opcional no cadastro, mas sem ele o cliente não compra oferta especial: o carrinho pede para cadastrar em Minha conta (informado uma vez, não muda). Vale para pedido online e para venda de balcão com CPF cadastrado. Prevalece sobre o combo. No balcão, item em oferta só sai com o CPF de um cliente cadastrado, para o limite valer também lá. |
| RN07 Pagamento | Pedido nasce ABERTO; cartão/dinheiro aprovam na hora, Pix fica pendente até a confirmação. Recusa mantém o pedido aberto sem mexer no estoque |
| RN08 Cancelamento | Cliente cancela até o pagamento confirmado (com estorno total). Depois da separação só o Dono ou o funcionário responsável, com estorno proporcional |
| RN09 Taxa por região | Taxa pelo bairro do endereço (sem acento/maiúscula) ou faixa de CEP; bairro fora da lista não recebe delivery |

O gateway de pagamento é simulado (`simularRecusa: true` força recusa no cartão).

## Segurança e rotinas

| O quê | Como funciona |
|---|---|
| Confirmação do Pix | Com `PAGAMENTO_SIMULADO=true` (padrão local e no CI) o cliente pode simular o "Já paguei". Com `false` (padrão na VM) só a loja confirma, pelo painel ou `POST /api/admin/pagamentos/{id}/confirmar-pix`, e o `simularRecusa` é ignorado. Num gateway real quem confirma é o webhook do banco |
| Tentativas de login | 5 senhas erradas seguidas na mesma conta travam o login por 15 min (resposta 429 com `Retry-After`). Acertar zera a contagem. Ajuste com `LOGIN_MAX_TENTATIVAS` e `LOGIN_BLOQUEIO_MINUTOS` |
| Sessão | Todo request confere o token no banco: funcionário desativado ou com cargo trocado perde o acesso na hora, e trocar a senha derruba os tokens antigos do cliente |
| Pedido sem pagamento | A cada minuto uma rotina cancela pedidos ABERTO ou aguardando Pix há mais de `minutos_expiracao_pedido` (configuração, padrão 30; 0 desliga). O estoque reservado pelo Pix volta para a prateleira |
| Endereço já usado | Editar ou remover um endereço que está num pedido guarda o antigo no histórico (o pedido continua mostrando o endereço da época) |
| Segredo do JWT | O repositório não tem mais um segredo padrão. Sem `JWT_SECRET` (ou `app.jwt.segredo` no `local.properties`), a API gera uma chave aleatória a cada início e avisa no log: ninguém consegue forjar token, mas as sessões caem quando ela reinicia |
| Concorrência | Pagar, confirmar Pix, mudar status, cancelar e expirar travam a linha do pedido (`SELECT … FOR UPDATE`), então duas ações ao mesmo tempo não se atropelam |

## Tratamento de erros

Toda resposta de erro da API sai em ProblemDetail (RFC 7807): `title`, `status`, `detail` e, quando cabe, `regra` (RN violada), `campos` (validação) ou `codigo` (rastreio no log).

| Status | Quando |
|---|---|
| 400 | Validação dos campos, JSON quebrado, parâmetro com tipo errado ou ausente, CPF inválido |
| 401 | Sem token, token inválido/expirado ou login errado |
| 403 | Perfil sem acesso à rota |
| 404 | Registro ou rota inexistente (inclusive recurso de outro cliente) |
| 405 / 415 | Método ou tipo de conteúdo não aceito |
| 409 | Duplicidade (e-mail, CPF, usuário) ou registro em uso |
| 422 | Regra de negócio RN01–RN09 |
| 500 | Erro inesperado: a resposta traz só um `codigo`, o stack trace fica no log com o mesmo código |

No site, `core/erro-api.ts` transforma essas respostas em mensagem para o usuário; o interceptor encerra a sessão inválida (e manda para o login se a tela for restrita); o `ErroGlobal` mostra um aviso com "Recarregar" para qualquer erro não tratado, inclusive site desatualizado depois de um deploy; e rotas inexistentes caem na página 404.

## Deploy no Render (grátis)

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

## Deploy na sua VM (Azure ou qualquer Ubuntu)

`deploy/vm` sobe tudo com Docker Compose numa máquina só: MySQL, API e o site no [Caddy](https://caddyserver.com), que também faz o proxy de `/api` (mesmo endereço, sem CORS) e tira o certificado HTTPS sozinho quando a VM tem um nome DNS.

1. **VM:** Ubuntu Server 24.04, tamanho B1s (grátis no Azure for Students) ou maior. Em *Rede*, libere as portas de entrada **80** e **443** além da 22 (no Cloud Shell: `az vm open-port -g <grupo> -n <vm> --port 80,443`).
2. **Nome DNS (para ter HTTPS):** na *Visão geral* da VM, clique em *Nome DNS → Configurar*, coloque um rótulo (ex.: `loudfeliz`) e salve. Fica `loudfeliz.<região>.cloudapp.azure.com`.
3. **Na VM** (`ssh azureuser@<IP>`):

   ```bash
   git clone https://github.com/Walefandrade14/adega-tabacaria.git
   cd adega-tabacaria
   sudo bash deploy/vm/instalar.sh loudfeliz.brazilsouth.cloudapp.azure.com
   ```

   Sem nome DNS, passe o IP público: o site fica em `http://<IP>`.

O script instala o Docker, cria swap se a VM tiver pouca memória, gera as senhas em `deploy/vm/.env` (banco, JWT e a do usuário `dono`), constrói as imagens e só termina quando a API responde; no fim mostra o endereço e a senha do painel. A primeira vez leva de 5 a 15 min, conforme o tamanho da VM.

- Atualizar: `git pull && sudo bash deploy/vm/instalar.sh`
- Logs: `sudo docker compose -f deploy/vm/docker-compose.yml logs -f api`
- Backup do banco: `sudo docker compose -f deploy/vm/docker-compose.yml exec db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" adega_tabacaria' > backup.sql`

Na VM o Pix só é confirmado pela loja (`PAGAMENTO_SIMULADO=false` no `deploy/vm/.env`). Para a demo com o botão "Já paguei", troque para `true` e rode o `instalar.sh` de novo.

A senha do `dono` vale na primeira subida (quando ele é criado); trocar `ADMIN_PASSWORD` depois não muda o usuário que já existe. O CI sobe essa mesma stack com o `instalar.sh` e roda o teste de ponta a ponta contra ela.
