package br.com.loudfeliz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.loudfeliz.config.ModoPagamento;
import br.com.loudfeliz.dominio.Fuso;
import br.com.loudfeliz.repositorio.HorarioRepositorio;
import br.com.loudfeliz.repositorio.PedidoRepositorio;
import br.com.loudfeliz.servico.ExpiracaoPedidos;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class RegrasDeNegocioTest {

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private HorarioRepositorio horarios;

    @Autowired
    private PedidoRepositorio pedidos;

    @Autowired
    private ModoPagamento modoPagamento;

    @Autowired
    private ExpiracaoPedidos expiracao;

    private MockMvc mvc;
    private String dono;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        deliveryVinteEQuatroHoras(true);
        dono = JsonPath.read(chamar(post("/api/auth/funcionarios/login"), null, """
                {"usuario":"dono","senha":"admin123"}""", 200), "$.token");
    }

    @Test
    void catalogoPublicoEStatusDaLoja() throws Exception {
        String produtos = chamar(get("/api/produtos"), null, null, 200);
        List<Object> lista = JsonPath.read(produtos, "$");
        assertFalse(lista.isEmpty());
        assertTrue(produtos.contains("\"categoria\""));
        List<Object> categorias = JsonPath.read(chamar(get("/api/categorias"), null, null, 200), "$");
        assertTrue(categorias.size() >= 4);
        Boolean delivery = JsonPath.read(chamar(get("/api/loja/status"), null, null, 200), "$.deliveryDisponivel");
        assertTrue(delivery);
        chamar(get("/actuator/health"), null, null, 200);
    }

    @Test
    void errosDeSegurancaERotaSaemEmProblemDetail() throws Exception {
        String semToken = chamar(get("/api/admin/produtos"), null, null, 401);
        assertEquals("Não autorizado", JsonPath.read(semToken, "$.title"));
        assertEquals("Faça login para acessar este recurso.", JsonPath.read(semToken, "$.detail"));

        String tokenRuim = mvc.perform(get("/api/admin/produtos").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals("Sessão expirada ou inválida. Entre de novo.", JsonPath.read(tokenRuim, "$.detail"));

        String cliente = novoCliente(LocalDate.of(1995, 5, 10), "Centro").token();
        String negado = chamar(get("/api/admin/produtos"), cliente, null, 403);
        assertEquals("Acesso negado", JsonPath.read(negado, "$.title"));
        Integer statusNegado = JsonPath.read(negado, "$.status");
        assertEquals(403, statusNegado);

        String rotaInexistente = chamar(get("/api/nao-existe"), dono, null, 404);
        assertEquals("Não encontrado", JsonPath.read(rotaInexistente, "$.title"));

        String metodoErrado = chamar(delete("/api/produtos"), dono, null, 405);
        assertEquals("Método não permitido", JsonPath.read(metodoErrado, "$.title"));

        String jsonQuebrado = chamar(post("/api/auth/login"), null, "{", 400);
        assertEquals("O corpo da requisição não é um JSON válido para este endpoint.", JsonPath.read(jsonQuebrado, "$.detail"));

        String senhaErrada = chamar(post("/api/auth/funcionarios/login"), null, """
                {"usuario":"dono","senha":"errada"}""", 401);
        assertEquals("Não autorizado", JsonPath.read(senhaErrada, "$.title"));
    }

    @Test
    void rotasProtegidasExigemPerfilCorreto() throws Exception {
        chamar(get("/api/admin/produtos"), null, null, 401);
        chamar(get("/api/pedidos"), null, null, 401);
        String cliente = novoCliente(LocalDate.of(1995, 5, 10), "Centro").token();
        chamar(get("/api/admin/produtos"), cliente, null, 403);
        chamar(get("/api/pedidos"), dono, null, 403);
    }

    @Test
    void fluxoCompletoDeDeliveryComCartao() throws Exception {
        long produto = novoProduto("60.00", 10, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        String carrinho = carrinho("DELIVERY", cliente.enderecoId(), item(produto, 1));

        String simulacao = chamar(post("/api/carrinho/simular"), cliente.token(), carrinho, 200);
        assertTrue(JsonPath.<Boolean>read(simulacao, "$.podeFinalizar"));
        assertEquals(5.0, numero(simulacao, "$.taxaEntrega"), 0.001);
        assertEquals(65.0, numero(simulacao, "$.total"), 0.001);

        long pedido = id(chamar(post("/api/pedidos"), cliente.token(), carrinho, 201));
        String pagamento = chamar(post("/api/pedidos/" + pedido + "/pagamentos"), cliente.token(), """
                {"forma":"CARTAO_CREDITO"}""", 201);
        assertEquals("APROVADO", JsonPath.read(pagamento, "$.status"));
        assertEquals("PAGO", statusDoPedido(cliente.token(), pedido));
        assertEquals(9, estoque(produto));

        mudarStatus(pedido, "EM_SEPARACAO", 200);
        mudarStatus(pedido, "EM_ENTREGA", 200);
        mudarStatus(pedido, "CONCLUIDO", 200);
        assertEquals("CONCLUIDO", statusDoPedido(cliente.token(), pedido));
    }

    @Test
    void rn01MenorDeIdadeNaoCompraItemRestrito() throws Exception {
        long produto = novoProduto("40.00", 10, true, false, null);
        Cliente menor = novoCliente(Fuso.hoje().minusYears(16), "Centro");
        String erro = chamar(post("/api/pedidos"), menor.token(), carrinho("RETIRADA", null, item(produto, 1)), 422);
        assertEquals("RN01", JsonPath.read(erro, "$.regra"));
    }

    @Test
    void rn02NaoVendeAlemDoEstoque() throws Exception {
        long produto = novoProduto("15.00", 2, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        String erro = chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 3)), 422);
        assertEquals("RN02", JsonPath.read(erro, "$.regra"));
    }

    @Test
    void rn03ComboAplicaDescontoAutomaticamente() throws Exception {
        long destilado = novoProduto("100.00", 10, false, false, null);
        long energetico = novoProduto("10.00", 10, false, false, null);
        chamar(post("/api/admin/combos"), dono, """
                {"nome":"Combo %s","descontoPercentual":10,"itens":[{"produtoId":%d,"quantidade":1},{"produtoId":%d,"quantidade":2}]}"""
                .formatted(UUID.randomUUID(), destilado, energetico), 201);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        String simulacao = chamar(post("/api/carrinho/simular"), cliente.token(),
                carrinho("RETIRADA", null, item(destilado, 1), item(energetico, 2)), 200);
        assertEquals(120.0, numero(simulacao, "$.subtotal"), 0.001);
        assertEquals(12.0, numero(simulacao, "$.descontoTotal"), 0.001);
        assertEquals(108.0, numero(simulacao, "$.total"), 0.001);
    }

    @Test
    void rn04DeliveryBloqueadoForaDoHorario() throws Exception {
        long produto = novoProduto("50.00", 10, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        deliveryVinteEQuatroHoras(false);
        String erro = chamar(post("/api/pedidos"), cliente.token(), carrinho("DELIVERY", cliente.enderecoId(), item(produto, 1)), 422);
        assertEquals("RN04", JsonPath.read(erro, "$.regra"));
        long pedido = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 1)), 201));
        assertTrue(pedido > 0);
    }

    @Test
    void rn05TaxaDeEntregaNaoContaParaOMinimo() throws Exception {
        long produto = novoProduto("32.00", 10, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        String erro = chamar(post("/api/pedidos"), cliente.token(), carrinho("DELIVERY", cliente.enderecoId(), item(produto, 1)), 422);
        assertEquals("RN05", JsonPath.read(erro, "$.regra"));
    }

    @Test
    void rn06LimiteDiarioDeOfertaPorCpf() throws Exception {
        long oferta = novoProduto("10.00", 50, false, true, 3);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        String erro = chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(oferta, 4)), 422);
        assertEquals("RN06", JsonPath.read(erro, "$.regra"));

        long pedido = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(oferta, 2)), 201));
        chamar(post("/api/pedidos/" + pedido + "/pagamentos"), cliente.token(), """
                {"forma":"CARTAO_DEBITO"}""", 201);
        String segundo = chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(oferta, 2)), 422);
        assertEquals("RN06", JsonPath.read(segundo, "$.regra"));
    }

    @Test
    void rn07RecusaMantemPedidoAbertoEPixReservaOEstoque() throws Exception {
        long produto = novoProduto("25.00", 5, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        long pedido = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 1)), 201));

        String recusado = chamar(post("/api/pedidos/" + pedido + "/pagamentos"), cliente.token(), """
                {"forma":"CARTAO_CREDITO","simularRecusa":true}""", 201);
        assertEquals("RECUSADO", JsonPath.read(recusado, "$.status"));
        assertEquals("ABERTO", statusDoPedido(cliente.token(), pedido));
        assertEquals(5, estoque(produto));

        String pix = chamar(post("/api/pedidos/" + pedido + "/pagamentos"), cliente.token(), """
                {"forma":"PIX"}""", 201);
        assertEquals("PENDENTE", JsonPath.read(pix, "$.status"));
        assertEquals("AGUARDANDO_PAGAMENTO", statusDoPedido(cliente.token(), pedido));
        assertEquals(4, estoque(produto));
        mudarStatus(pedido, "EM_SEPARACAO", 422);

        String confirmado = chamar(post("/api/pagamentos/" + id(pix) + "/confirmar-pix"), cliente.token(), null, 200);
        assertEquals("APROVADO", JsonPath.read(confirmado, "$.status"));
        assertEquals("PAGO", statusDoPedido(cliente.token(), pedido));
        assertEquals(4, estoque(produto));
    }

    @Test
    void rn08CancelamentoComEstorno() throws Exception {
        long produto = novoProduto("40.00", 5, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");

        long primeiro = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 2)), 201));
        chamar(post("/api/pedidos/" + primeiro + "/pagamentos"), cliente.token(), """
                {"forma":"PIX"}""", 201);
        String pixPendente = chamar(get("/api/pedidos/" + primeiro), cliente.token(), null, 200);
        long pagamentoPix = ((Number) JsonPath.read(pixPendente, "$.pagamentos[0].id")).longValue();
        chamar(post("/api/pagamentos/" + pagamentoPix + "/confirmar-pix"), cliente.token(), null, 200);
        assertEquals(3, estoque(produto));
        String cancelado = chamar(post("/api/pedidos/" + primeiro + "/cancelar"), cliente.token(), null, 200);
        assertEquals("CANCELADO", JsonPath.read(cancelado, "$.status"));
        assertEquals("ESTORNADO", JsonPath.read(cancelado, "$.pagamentos[0].status"));
        assertEquals(5, estoque(produto));

        long segundo = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 1)), 201));
        chamar(post("/api/pedidos/" + segundo + "/pagamentos"), cliente.token(), """
                {"forma":"CARTAO_CREDITO"}""", 201);
        mudarStatus(segundo, "EM_SEPARACAO", 200);
        String negado = chamar(post("/api/pedidos/" + segundo + "/cancelar"), cliente.token(), null, 422);
        assertEquals("RN08", JsonPath.read(negado, "$.regra"));

        String peloDono = chamar(post("/api/admin/pedidos/" + segundo + "/cancelar"), dono, """
                {"percentualEstorno":50}""", 200);
        assertEquals("CANCELADO", JsonPath.read(peloDono, "$.status"));
        assertEquals(20.0, numero(peloDono, "$.pagamentos[1].valor"), 0.001);
        assertEquals("ESTORNADO", JsonPath.read(peloDono, "$.pagamentos[1].status"));
        assertEquals(5, estoque(produto));
    }

    @Test
    void rn09BairroSemEntrega() throws Exception {
        long produto = novoProduto("50.00", 10, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Bairro Sem Entrega " + UUID.randomUUID());
        String erro = chamar(post("/api/pedidos"), cliente.token(), carrinho("DELIVERY", cliente.enderecoId(), item(produto, 1)), 422);
        assertEquals("RN09", JsonPath.read(erro, "$.regra"));
        chamar(get("/api/entrega/taxa?bairro=centro"), null, null, 200);
    }

    @Test
    void pdvExigeConferenciaDoDocumentoParaItemRestrito() throws Exception {
        long produto = novoProduto("30.00", 10, true, false, null);
        String verificacao = chamar(post("/api/admin/pdv/verificar"), dono, """
                {"itens":[%s]}""".formatted(item(produto, 1)), 200);
        assertTrue(JsonPath.<Boolean>read(verificacao, "$.exigeConferenciaDocumento"));
        assertFalse(JsonPath.<Boolean>read(verificacao, "$.podeFinalizar"));

        String erro = chamar(post("/api/admin/pdv/vendas"), dono, """
                {"itens":[%s],"formaPagamento":"DINHEIRO","documentoConferido":false}""".formatted(item(produto, 1)), 422);
        assertEquals("RN01", JsonPath.read(erro, "$.regra"));

        String venda = chamar(post("/api/admin/pdv/vendas"), dono, """
                {"itens":[%s],"formaPagamento":"DINHEIRO","documentoConferido":true}""".formatted(item(produto, 1)), 201);
        assertEquals("CONCLUIDO", JsonPath.read(venda, "$.status"));
        assertEquals("BALCAO", JsonPath.read(venda, "$.tipoEntrega"));
        assertEquals(9, estoque(produto));
    }

    @Test
    void foraDoModoSimuladoSoALojaConfirmaOPix() throws Exception {
        long produto = novoProduto("25.00", 5, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        ReflectionTestUtils.setField(modoPagamento, "simulado", false);
        try {
            assertFalse(JsonPath.<Boolean>read(chamar(get("/api/loja/status"), null, null, 200), "$.pagamentoSimulado"));
            long pedido = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 1)), 201));
            long pix = id(chamar(post("/api/pedidos/" + pedido + "/pagamentos"), cliente.token(), """
                    {"forma":"PIX"}""", 201));

            String negado = chamar(post("/api/pagamentos/" + pix + "/confirmar-pix"), cliente.token(), null, 422);
            assertEquals("RN07", JsonPath.read(negado, "$.regra"));
            assertEquals("AGUARDANDO_PAGAMENTO", statusDoPedido(cliente.token(), pedido));
            assertEquals(4, estoque(produto));

            String confirmado = chamar(post("/api/admin/pagamentos/" + pix + "/confirmar-pix"), dono, null, 200);
            assertEquals("APROVADO", JsonPath.read(confirmado, "$.status"));
            assertEquals("PAGO", statusDoPedido(cliente.token(), pedido));
            assertEquals(4, estoque(produto));
        } finally {
            ReflectionTestUtils.setField(modoPagamento, "simulado", true);
        }
        assertTrue(JsonPath.<Boolean>read(chamar(get("/api/loja/status"), null, null, 200), "$.pagamentoSimulado"));
    }

    @Test
    void loginTravaDepoisDeCincoErros() throws Exception {
        String email = "trava-" + UUID.randomUUID() + "@teste.com";
        chamar(post("/api/auth/cadastro"), null, """
                {"nome":"Cliente Trava","cpf":"%s","dataNascimento":"1990-01-01","email":"%s","senha":"segredo123"}"""
                .formatted(cpfValido(), email), 201);
        String errada = """
                {"email":"%s","senha":"errada"}""".formatted(email);
        for (int i = 0; i < 5; i++) {
            chamar(post("/api/auth/login"), null, errada, 401);
        }
        String bloqueado = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"%s","senha":"segredo123"}""".formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals("Muitas tentativas", JsonPath.read(bloqueado, "$.title"));
        assertTrue(JsonPath.<String>read(bloqueado, "$.detail").contains("15 minuto"));
    }

    @Test
    void funcionarioDesativadoPerdeOAcessoNaHora() throws Exception {
        String usuario = "caixa" + ThreadLocalRandom.current().nextInt(1_000_000);
        long funcionario = id(chamar(post("/api/admin/funcionarios"), dono, """
                {"nome":"Caixa Teste","cpf":"%s","cargo":"CAIXA","usuario":"%s","senha":"caixa123"}"""
                .formatted(cpfValido(), usuario), 201));
        String token = JsonPath.read(chamar(post("/api/auth/funcionarios/login"), null, """
                {"usuario":"%s","senha":"caixa123"}""".formatted(usuario), 200), "$.token");
        chamar(get("/api/admin/pedidos"), token, null, 200);

        chamar(patch("/api/admin/funcionarios/" + funcionario + "/ativo?ativo=false"), dono, null, 200);
        String negado = chamar(get("/api/admin/pedidos"), token, null, 401);
        assertEquals("Sessão expirada ou inválida. Entre de novo.", JsonPath.read(negado, "$.detail"));
        chamar(post("/api/auth/funcionarios/login"), null, """
                {"usuario":"%s","senha":"caixa123"}""".formatted(usuario), 401);
    }

    @Test
    void clienteEditaDadosTrocaSenhaEEndereco() throws Exception {
        String email = "conta-" + UUID.randomUUID() + "@teste.com";
        String antigo = JsonPath.read(chamar(post("/api/auth/cadastro"), null, """
                {"nome":"Nome Antigo","cpf":"%s","dataNascimento":"1990-01-01","email":"%s","senha":"segredo123"}"""
                .formatted(cpfValido(), email), 201), "$.token");

        String perfil = chamar(put("/api/clientes/me"), antigo, """
                {"nome":"Nome Novo","telefone":"11988887777"}""", 200);
        assertEquals("Nome Novo", JsonPath.read(perfil, "$.nome"));
        assertEquals(email, JsonPath.read(perfil, "$.email"));

        chamar(put("/api/clientes/me/senha"), antigo, """
                {"senhaAtual":"errada","novaSenha":"outra123"}""", 400);
        String novo = JsonPath.read(chamar(put("/api/clientes/me/senha"), antigo, """
                {"senhaAtual":"segredo123","novaSenha":"outra123"}""", 200), "$.token");
        chamar(get("/api/clientes/me"), antigo, null, 401);
        chamar(get("/api/clientes/me"), novo, null, 200);
        chamar(post("/api/auth/login"), null, """
                {"email":"%s","senha":"segredo123"}""".formatted(email), 401);
        String login = JsonPath.read(chamar(post("/api/auth/login"), null, """
                {"email":"%s","senha":"outra123"}""".formatted(email), 200), "$.token");
        chamar(get("/api/clientes/me"), login, null, 200);

        long endereco = id(chamar(post("/api/clientes/me/enderecos"), novo, endereco("Centro", "10"), 201));
        String editado = chamar(put("/api/clientes/me/enderecos/" + endereco), novo, endereco("Vila Nova", "10"), 200);
        assertEquals(endereco, id(editado));
        assertEquals("Vila Nova", JsonPath.read(editado, "$.bairro"));

        long produto = novoProduto("50.00", 5, false, false, null);
        long pedido = id(chamar(post("/api/pedidos"), novo, carrinho("DELIVERY", endereco, item(produto, 1)), 201));
        String mudou = chamar(put("/api/clientes/me/enderecos/" + endereco), novo, endereco("Vila Nova", "99"), 200);
        long novoEndereco = id(mudou);
        assertTrue(novoEndereco != endereco);
        List<Object> lista = JsonPath.read(chamar(get("/api/clientes/me/enderecos"), novo, null, 200), "$[*].id");
        assertEquals(1, lista.size());
        assertEquals(novoEndereco, ((Number) lista.get(0)).longValue());
        assertEquals("10", JsonPath.read(chamar(get("/api/pedidos/" + pedido), novo, null, 200), "$.enderecoEntrega.numero"));
        chamar(post("/api/pedidos"), novo, carrinho("DELIVERY", endereco, item(produto, 1)), 404);

        chamar(delete("/api/clientes/me/enderecos/" + novoEndereco), novo, null, 204);
        List<Object> vazia = JsonPath.read(chamar(get("/api/clientes/me/enderecos"), novo, null, 200), "$");
        assertTrue(vazia.isEmpty());
    }

    @Test
    void pedidoSemPagamentoExpiraSozinho() throws Exception {
        long produto = novoProduto("30.00", 5, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        long antigo = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 1)), 201));
        chamar(post("/api/pedidos/" + antigo + "/pagamentos"), cliente.token(), """
                {"forma":"PIX"}""", 201);
        long recente = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 1)), 201));
        pedidos.findById(antigo).ifPresent(p -> {
            p.setCriadoEm(Fuso.agora().minusHours(2));
            pedidos.save(p);
        });

        assertTrue(expiracao.expirar() >= 1);
        String expirado = chamar(get("/api/pedidos/" + antigo), cliente.token(), null, 200);
        assertEquals("CANCELADO", JsonPath.read(expirado, "$.status"));
        assertEquals("RECUSADO", JsonPath.read(expirado, "$.pagamentos[0].status"));
        assertEquals("ABERTO", statusDoPedido(cliente.token(), recente));
        assertEquals(5, estoque(produto));
    }

    @Test
    void relatorioDoDonoEListaPaginada() throws Exception {
        long produto = novoProduto("20.00", 10, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        long pedido = id(chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(produto, 2)), 201));
        chamar(post("/api/pedidos/" + pedido + "/pagamentos"), cliente.token(), """
                {"forma":"CARTAO_DEBITO"}""", 201);

        String resumo = chamar(get("/api/admin/relatorios/resumo"), dono, null, 200);
        assertTrue(((Number) JsonPath.read(resumo, "$.pedidos")).longValue() >= 1);
        assertTrue(numero(resumo, "$.faturamento") >= 40.0);
        assertTrue(numero(resumo, "$.ticketMedio") > 0);
        List<Object> dias = JsonPath.read(resumo, "$.dias");
        assertEquals(7, dias.size());
        List<Object> canais = JsonPath.read(resumo, "$.canais[?(@.tipo == 'RETIRADA')].pedidos");
        assertTrue(((Number) canais.get(0)).longValue() >= 1);
        List<Object> vendidos = JsonPath.read(resumo, "$.maisVendidos");
        assertFalse(vendidos.isEmpty());
        int limite = JsonPath.read(resumo, "$.limiteEstoqueBaixo");
        List<Integer> estoques = JsonPath.read(resumo, "$.estoqueBaixo[*].estoque");
        assertTrue(estoques.stream().allMatch(e -> e <= limite));

        chamar(get("/api/admin/relatorios/resumo?de=2026-02-10&ate=2026-02-01"), dono, null, 400);
        chamar(get("/api/admin/relatorios/resumo"), cliente.token(), null, 403);

        String pagina = chamar(get("/api/admin/pedidos?tamanho=2"), dono, null, 200);
        List<Object> itens = JsonPath.read(pagina, "$.itens");
        assertTrue(itens.size() <= 2 && !itens.isEmpty());
        assertEquals(0, (int) JsonPath.read(pagina, "$.pagina"));
        assertTrue(((Number) JsonPath.read(pagina, "$.total")).longValue() >= itens.size());
        String filtrada = chamar(get("/api/admin/pedidos?status=PAGO&status=CONCLUIDO&tamanho=50"), dono, null, 200);
        List<String> status = JsonPath.read(filtrada, "$.itens[*].status");
        assertTrue(status.contains("PAGO"));
        assertTrue(status.stream().allMatch(s -> s.equals("PAGO") || s.equals("CONCLUIDO")));
    }

    @Test
    void cpfOpcionalNoCadastroMasExigidoNaOferta() throws Exception {
        String token = JsonPath.read(chamar(post("/api/auth/cadastro"), null, """
                {"nome":"Sem CPF","dataNascimento":"1990-01-01","email":"sem-cpf-%s@teste.com","senha":"segredo123"}"""
                .formatted(UUID.randomUUID()), 201), "$.token");
        String perfil = chamar(get("/api/clientes/me"), token, null, 200);
        assertNull(JsonPath.read(perfil, "$.cpfMascarado"));
        chamar(post("/api/auth/cadastro"), null, """
                {"nome":"CPF Ruim","cpf":"111.111.111-11","dataNascimento":"1990-01-01","email":"cpf-ruim-%s@teste.com","senha":"segredo123"}"""
                .formatted(UUID.randomUUID()), 400);

        long oferta = novoProduto("15.00", 10, false, true, 2);
        long comum = novoProduto("15.00", 10, false, false, null);
        chamar(post("/api/pedidos"), token, carrinho("RETIRADA", null, item(comum, 1)), 201);
        String bloqueado = chamar(post("/api/pedidos"), token, carrinho("RETIRADA", null, item(oferta, 1)), 422);
        assertEquals("RN06", JsonPath.read(bloqueado, "$.regra"));
        assertTrue(JsonPath.<String>read(bloqueado, "$.detail").contains("Cadastre seu CPF"));

        String cpf = cpfValido();
        String comCpf = chamar(put("/api/clientes/me"), token, """
                {"nome":"Sem CPF","cpf":"%s"}""".formatted(cpf), 200);
        assertEquals("***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**", JsonPath.read(comCpf, "$.cpfMascarado"));
        chamar(post("/api/pedidos"), token, carrinho("RETIRADA", null, item(oferta, 1)), 201);
        chamar(put("/api/clientes/me"), token, """
                {"nome":"Sem CPF","cpf":"%s"}""".formatted(cpfValido()), 400);

        String outro = JsonPath.read(chamar(post("/api/auth/cadastro"), null, """
                {"nome":"Outro","dataNascimento":"1990-01-01","email":"outro-%s@teste.com","senha":"segredo123"}"""
                .formatted(UUID.randomUUID()), 201), "$.token");
        chamar(put("/api/clientes/me"), outro, """
                {"nome":"Outro","cpf":"%s"}""".formatted(cpf), 409);
    }

    @Test
    void pixReservaOEstoqueEDevolveQuandoOPagamentoMudaOuOPedidoCancela() throws Exception {
        long produto = novoProduto("40.00", 1, false, false, null);
        Cliente ana = novoCliente(LocalDate.of(1990, 1, 1), "Centro");
        Cliente bia = novoCliente(LocalDate.of(1990, 1, 1), "Centro");

        long pedidoAna = id(chamar(post("/api/pedidos"), ana.token(), carrinho("RETIRADA", null, item(produto, 1)), 201));
        chamar(post("/api/pedidos/" + pedidoAna + "/pagamentos"), ana.token(), """
                {"forma":"PIX"}""", 201);
        assertEquals(0, estoque(produto));
        chamar(post("/api/pedidos/" + pedidoAna + "/pagamentos"), ana.token(), """
                {"forma":"PIX"}""", 201);
        assertEquals(0, estoque(produto));

        String semEstoque = chamar(post("/api/pedidos"), bia.token(), carrinho("RETIRADA", null, item(produto, 1)), 422);
        assertEquals("RN02", JsonPath.read(semEstoque, "$.regra"));

        String recusado = chamar(post("/api/pedidos/" + pedidoAna + "/pagamentos"), ana.token(), """
                {"forma":"CARTAO_CREDITO","simularRecusa":true}""", 201);
        assertEquals("RECUSADO", JsonPath.read(recusado, "$.status"));
        assertEquals("ABERTO", statusDoPedido(ana.token(), pedidoAna));
        assertEquals(1, estoque(produto));

        long pedidoBia = id(chamar(post("/api/pedidos"), bia.token(), carrinho("RETIRADA", null, item(produto, 1)), 201));
        chamar(post("/api/pedidos/" + pedidoBia + "/pagamentos"), bia.token(), """
                {"forma":"PIX"}""", 201);
        assertEquals(0, estoque(produto));
        String esgotado = chamar(post("/api/pedidos/" + pedidoAna + "/pagamentos"), ana.token(), """
                {"forma":"PIX"}""", 422);
        assertEquals("RN02", JsonPath.read(esgotado, "$.regra"));
        String pedidoSemPagamento = chamar(get("/api/pedidos/" + pedidoAna), ana.token(), null, 200);
        assertEquals("ABERTO", JsonPath.read(pedidoSemPagamento, "$.status"));
        List<String> statusPagamentos = JsonPath.read(pedidoSemPagamento, "$.pagamentos[*].status");
        assertFalse(statusPagamentos.contains("PENDENTE"));

        chamar(post("/api/pedidos/" + pedidoBia + "/cancelar"), bia.token(), null, 200);
        assertEquals(1, estoque(produto));

        String pix = chamar(post("/api/pedidos/" + pedidoAna + "/pagamentos"), ana.token(), """
                {"forma":"PIX"}""", 201);
        assertEquals(0, estoque(produto));
        chamar(post("/api/pagamentos/" + id(pix) + "/confirmar-pix"), ana.token(), null, 200);
        assertEquals("PAGO", statusDoPedido(ana.token(), pedidoAna));
        assertEquals(0, estoque(produto));
        chamar(post("/api/pedidos/" + pedidoAna + "/cancelar"), ana.token(), null, 200);
        assertEquals(1, estoque(produto));
    }

    @Test
    void ofertaNoBalcaoSoComClienteIdentificadoPorCpf() throws Exception {
        long oferta = novoProduto("12.00", 10, false, true, 2);
        String semCpf = chamar(post("/api/admin/pdv/vendas"), dono, """
                {"itens":[%s],"formaPagamento":"DINHEIRO"}""".formatted(item(oferta, 1)), 422);
        assertEquals("RN06", JsonPath.read(semCpf, "$.regra"));
        assertTrue(JsonPath.<String>read(semCpf, "$.detail").contains("CPF"));
        assertEquals(10, estoque(oferta));

        String cpf = cpfValido();
        chamar(post("/api/auth/cadastro"), null, """
                {"nome":"Cliente Balcão","cpf":"%s","dataNascimento":"1990-01-01","email":"balcao-%s@teste.com","senha":"segredo123"}"""
                .formatted(cpf, UUID.randomUUID()), 201);
        String venda = chamar(post("/api/admin/pdv/vendas"), dono, """
                {"clienteCpf":"%s","itens":[%s],"formaPagamento":"DINHEIRO"}""".formatted(cpf, item(oferta, 2)), 201);
        assertEquals("CONCLUIDO", JsonPath.read(venda, "$.status"));
        String acimaDoLimite = chamar(post("/api/admin/pdv/vendas"), dono, """
                {"clienteCpf":"%s","itens":[%s],"formaPagamento":"DINHEIRO"}""".formatted(cpf, item(oferta, 1)), 422);
        assertEquals("RN06", JsonPath.read(acimaDoLimite, "$.regra"));
        assertEquals(8, estoque(oferta));
    }

    @Test
    void pedidoComItemMaiorDeIdadeAvisaParaConferirDocumento() throws Exception {
        long restrito = novoProduto("45.00", 10, true, false, null);
        long comum = novoProduto("45.00", 10, false, false, null);
        Cliente cliente = novoCliente(LocalDate.of(1990, 1, 1), "Centro");

        String comRestrito = chamar(post("/api/pedidos"), cliente.token(),
                carrinho("DELIVERY", cliente.enderecoId(), item(restrito, 1), item(comum, 1)), 201);
        assertTrue(JsonPath.<Boolean>read(comRestrito, "$.conferirDocumento"));
        String semRestrito = chamar(post("/api/pedidos"), cliente.token(), carrinho("RETIRADA", null, item(comum, 1)), 201);
        assertFalse(JsonPath.<Boolean>read(semRestrito, "$.conferirDocumento"));
        String doPainel = chamar(get("/api/admin/pedidos/" + id(comRestrito)), dono, null, 200);
        assertTrue(JsonPath.<Boolean>read(doPainel, "$.conferirDocumento"));

        String balcao = chamar(post("/api/admin/pdv/vendas"), dono, """
                {"itens":[%s],"formaPagamento":"DINHEIRO","documentoConferido":true}""".formatted(item(restrito, 1)), 201);
        assertFalse(JsonPath.<Boolean>read(balcao, "$.conferirDocumento"));
    }

    private record Cliente(String token, Long enderecoId) {
    }

    private static String endereco(String bairro, String numero) {
        return """
                {"apelido":"Casa","logradouro":"Rua Teste","numero":"%s","bairro":"%s","cidade":"São Paulo","uf":"SP","cep":"01001000"}"""
                .formatted(numero, bairro);
    }

    private void deliveryVinteEQuatroHoras(boolean aceita) {
        horarios.findAll().forEach(h -> {
            h.setHoraAbertura(LocalTime.MIDNIGHT);
            h.setHoraFechamento(LocalTime.MIDNIGHT);
            h.setAceitaDelivery(aceita);
            horarios.save(h);
        });
    }

    private long novoProduto(String preco, int estoque, boolean restrito, boolean oferta, Integer limite) throws Exception {
        String corpo = """
                {"nome":"Produto %s","descricao":"teste","preco":%s,"estoqueQuantidade":%d,"categoriaId":1,
                 "restritoMaiorIdade":%b,"ofertaEspecial":%b,"limiteDiarioOferta":%s}"""
                .formatted(UUID.randomUUID(), preco, estoque, restrito, oferta, limite == null ? "null" : limite.toString());
        return id(chamar(post("/api/admin/produtos"), dono, corpo, 201));
    }

    private Cliente novoCliente(LocalDate nascimento, String bairro) throws Exception {
        String cadastro = """
                {"nome":"Cliente Teste","cpf":"%s","dataNascimento":"%s","email":"cliente-%s@teste.com","senha":"segredo123"}"""
                .formatted(cpfValido(), nascimento, UUID.randomUUID());
        String token = JsonPath.read(chamar(post("/api/auth/cadastro"), null, cadastro, 201), "$.token");
        String endereco = """
                {"apelido":"Casa","logradouro":"Rua Teste","numero":"10","bairro":"%s","cidade":"São Paulo","uf":"SP","cep":"01001000","principal":true}"""
                .formatted(bairro);
        long enderecoId = id(chamar(post("/api/clientes/me/enderecos"), token, endereco, 201));
        return new Cliente(token, enderecoId);
    }

    private void mudarStatus(long pedido, String novo, int esperado) throws Exception {
        chamar(patch("/api/admin/pedidos/" + pedido + "/status"), dono, """
                {"status":"%s"}""".formatted(novo), esperado);
    }

    private String statusDoPedido(String token, long pedido) throws Exception {
        return JsonPath.read(chamar(get("/api/pedidos/" + pedido), token, null, 200), "$.status");
    }

    private int estoque(long produto) throws Exception {
        return JsonPath.read(chamar(get("/api/produtos/" + produto), null, null, 200), "$.estoqueQuantidade");
    }

    private static String item(long produto, int quantidade) {
        return "{\"produtoId\":%d,\"quantidade\":%d}".formatted(produto, quantidade);
    }

    private static String carrinho(String tipo, Long enderecoId, String... itens) {
        return "{\"tipoEntrega\":\"%s\",\"enderecoId\":%s,\"itens\":[%s]}"
                .formatted(tipo, enderecoId == null ? "null" : enderecoId.toString(), String.join(",", itens));
    }

    private static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static double numero(String json, String caminho) {
        return ((Number) JsonPath.read(json, caminho)).doubleValue();
    }

    private String chamar(MockHttpServletRequestBuilder requisicao, String token, String corpo, int statusEsperado) throws Exception {
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        return mvc.perform(requisicao).andExpect(status().is(statusEsperado)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static String cpfValido() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = ThreadLocalRandom.current().nextInt(10);
            }
        } while (d[0] == d[1] && d[1] == d[2]);
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        StringBuilder cpf = new StringBuilder();
        for (int n : d) {
            cpf.append(n);
        }
        return cpf.toString();
    }

    private static int digito(int[] d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += d[i] * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
