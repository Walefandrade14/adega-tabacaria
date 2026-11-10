import json
import os
import random
import re
import sys
import uuid

from playwright.sync_api import expect, sync_playwright

BASE = os.environ.get("E2E_BASE", "http://127.0.0.1:4200")
MOCK = os.environ.get("E2E_MOCK")
MINIMO_PRODUTOS = int(os.environ.get("E2E_MINIMO_PRODUTOS", "10"))
SENHA_DONO = os.environ.get("E2E_SENHA_DONO", "admin123")
COMPRA = {}


def cpf_valido():
    d = [random.randint(0, 9) for _ in range(9)]
    for tamanho in (9, 10):
        soma = sum(d[i] * (tamanho + 1 - i) for i in range(tamanho))
        resto = (soma * 10) % 11
        d.append(0 if resto == 10 else resto)
    return "".join(map(str, d))


def contexto(navegador, **kwargs):
    ctx = navegador.new_context(viewport={"width": 1366, "height": 900}, **kwargs)
    if MOCK:
        sys.path.insert(0, MOCK)
        from mock import Mock, viacep

        global _mock
        _mock = globals().get("_mock") or Mock()
        _mock.simulado = not os.environ.get("E2E_PIX_LOJA")
        ctx.route("http://localhost:8080/api/**", _mock.handle)
        ctx.route("https://viacep.com.br/**", viacep)
    return ctx


def cliente(navegador, erros):
    pg = contexto(navegador).new_page()
    pg.on("pageerror", lambda e: erros.append(str(e)))
    sufixo = uuid.uuid4().hex[:8]
    email = f"e2e-{sufixo}@teste.com"

    pg.goto(BASE + "/minha-conta")
    pg.wait_for_url("**/entrar?voltar=**")

    pg.goto(BASE + "/cadastro")
    pg.fill("input[formcontrolname=nome]", f"Cliente E2E {sufixo}")
    pg.locator("input[formcontrolname=cpf]").press_sequentially(cpf_valido())
    pg.fill("input[formcontrolname=dataNascimento]", "1994-06-15")
    pg.fill("input[formcontrolname=email]", email)
    pg.locator("input[formcontrolname=telefone]").press_sequentially("11987654321")
    pg.fill("input[formcontrolname=senha]", "segredo123")
    pg.fill("input[formcontrolname=confirmacao]", "segredo123")
    pg.click("button[type=submit]")
    pg.wait_for_url("**/minha-conta**")
    expect(pg.get_by_text("Conta criada!")).to_be_visible()
    expect(pg.locator("a.conta")).to_contain_text("Cliente")

    pg.locator("input[formcontrolname=cep]").press_sequentially("01001000")
    pg.wait_for_timeout(1500)
    pg.fill("input[formcontrolname=logradouro]", "Praça da Sé")
    pg.fill("input[formcontrolname=numero]", "100")
    pg.fill("input[formcontrolname=bairro]", "Centro")
    pg.fill("input[formcontrolname=cidade]", "São Paulo")
    pg.fill("input[formcontrolname=uf]", "SP")
    pg.fill("input[formcontrolname=apelido]", "Casa")
    pg.click("form.novo button[type=submit]")
    expect(pg.get_by_text("Entregamos aqui")).to_be_visible()

    pg.get_by_role("button", name="Sair").click()
    pg.wait_for_url(BASE + "/")
    expect(pg.locator("a.conta")).to_contain_text("Entrar")

    pg.goto(BASE + "/entrar")
    pg.fill("input[formcontrolname=email]", email)
    pg.fill("input[formcontrolname=senha]", "errada")
    pg.click("button[type=submit]")
    expect(pg.locator(".alerta.erro")).to_be_visible()
    pg.fill("input[formcontrolname=senha]", "segredo123")
    pg.click("button[type=submit]")
    pg.wait_for_url("**/minha-conta")
    expect(pg.get_by_text(email)).to_be_visible()
    expect(pg.get_by_text("Praça da Sé, 100")).to_be_visible()
    print("cliente: cadastro, endereço, logout e login ok")

    pg.goto(BASE + "/produtos")
    card = pg.locator("app-produto-card").filter(has=pg.get_by_role("button", name="Adicionar")).first
    produto = card.locator("h3").inner_text().strip()
    card.get_by_role("button", name="Adicionar").click()
    expect(pg.locator(".sacola .contador")).to_have_text("1")
    pg.reload()
    expect(pg.locator(".sacola .contador")).to_have_text("1")
    pg.locator("a.sacola").click()
    pg.wait_for_url("**/carrinho")
    expect(pg.locator(".item", has_text=produto)).to_be_visible()
    pg.locator(".opcao", has_text="Retirar na loja").click()
    finalizar = pg.get_by_role("button", name="Ir para o pagamento")
    expect(finalizar).to_be_enabled(timeout=15000)
    finalizar.click()
    pg.wait_for_url(re.compile(r".*/pedidos/\d+$"))
    expect(pg.locator("[data-status]")).to_have_text("Aguardando pagamento")
    expect(pg.get_by_text(re.compile(r"cancelado sozinho"))).to_be_visible()
    pg.locator(".formas .opcao", has_text="Pix").click()
    pg.get_by_role("button", name=re.compile(r"^Pagar")).click()
    expect(pg.locator("code.pix")).to_be_visible()
    ja_paguei = pg.get_by_role("button", name="Já paguei")
    aguardando = pg.locator("[data-aguardando-pix]")
    expect(ja_paguei.or_(aguardando)).to_be_visible()
    if ja_paguei.is_visible():
        ja_paguei.click()
        status_esperado = "Pago"
    else:
        expect(pg.get_by_role("button", name="Simular cartão recusado (teste)")).to_have_count(0)
        COMPRA["pix_pela_loja"] = True
        status_esperado = "Aguardando Pix"
    expect(pg.locator("[data-status]")).to_have_text(status_esperado)
    expect(pg.locator(".sacola .contador")).to_have_count(0)
    COMPRA["cliente"] = f"Cliente E2E {sufixo}"
    pg.goto(BASE + "/pedidos")
    expect(pg.locator(".linha-pedido").first).to_contain_text(status_esperado)
    print(f"cliente: carrinho, retirada, pedido, Pix ({status_esperado}) e Meus pedidos ok")

    pg.goto(BASE + "/minha-conta")
    pg.get_by_role("button", name="Editar dados").click()
    pg.fill("input[formcontrolname=nome]", f"Cliente E2E {sufixo} Silva")
    pg.get_by_role("button", name="Salvar", exact=True).click()
    expect(pg.get_by_text("Dados atualizados.")).to_be_visible()
    expect(pg.locator(".dados h2")).to_have_text(f"Cliente E2E {sufixo} Silva")
    pg.get_by_role("button", name="Editar Casa").click()
    pg.fill("form.novo input[formcontrolname=numero]", "101")
    pg.get_by_role("button", name="Salvar alterações").click()
    expect(pg.get_by_text("Praça da Sé, 101")).to_be_visible()
    pg.get_by_role("button", name="Trocar senha").click()
    pg.fill("input[formcontrolname=senhaAtual]", "errada1")
    pg.fill("input[formcontrolname=novaSenha]", "novasenha1")
    pg.fill("input[formcontrolname=confirmacao]", "novasenha1")
    pg.locator("form.mini-form button[type=submit]").click()
    expect(pg.locator(".aviso-dados")).to_contain_text("senha atual")
    pg.fill("input[formcontrolname=senhaAtual]", "segredo123")
    pg.locator("form.mini-form button[type=submit]").click()
    expect(pg.locator(".aviso-dados")).to_contain_text("Senha trocada")
    pg.reload()
    expect(pg.locator(".dados h2")).to_have_text(f"Cliente E2E {sufixo} Silva")
    print("cliente: editar dados, editar endereço e trocar senha ok")


def dono(navegador, erros):
    pg = contexto(navegador).new_page()
    pg.on("pageerror", lambda e: erros.append(str(e)))
    nome = f"Gin E2E {uuid.uuid4().hex[:6]}"
    categoria = f"Categoria E2E {uuid.uuid4().hex[:6]}"

    pg.goto(BASE + "/admin")
    pg.wait_for_url("**/admin/entrar**")
    pg.fill("input[formcontrolname=usuario]", "dono")
    pg.fill("input[formcontrolname=senha]", SENHA_DONO)
    pg.click("button[type=submit]")
    pg.wait_for_url(BASE + "/admin")
    expect(pg.locator(".tabela tbody tr").nth(MINIMO_PRODUTOS - 1)).to_be_visible()

    pg.get_by_role("button", name="Novo produto").click()
    modal = pg.locator("app-produto-formulario")
    modal.locator("input[formcontrolname=nome]").fill(nome)
    modal.locator("select").select_option(label="Bebidas")
    modal.locator("input[formcontrolname=preco]").fill("129.90")
    modal.locator("input[formcontrolname=estoqueQuantidade]").fill("6")
    modal.locator("input[formcontrolname=restritoMaiorIdade]").check()
    modal.get_by_role("button", name="Cadastrar produto").click()
    expect(modal).to_have_count(0)
    linha = pg.locator(".tabela tbody tr", has_text=nome)
    expect(linha).to_be_visible()
    linha.get_by_role("button", name=f"Somar 1 a {nome}").click()
    expect(linha.locator(".estoque span")).to_have_text("7")

    linha.get_by_role("button", name="Editar").click()
    modal.locator("input[formcontrolname=preco]").fill("119.90")
    modal.get_by_role("button", name="Salvar alterações").click()
    expect(linha).to_contain_text("119,90")

    pg.get_by_role("tab", name="Categorias").click()
    pg.fill("form.nova input", categoria)
    pg.get_by_role("button", name="Adicionar").click()
    expect(pg.locator(".lista-categorias li", has_text=categoria)).to_be_visible()
    bebidas = pg.locator(".lista-categorias li", has_text="Bebidas")
    bebidas.get_by_role("button", name="Excluir").click()
    bebidas.get_by_role("button", name="Confirmar exclusão").click()
    expect(pg.locator(".alerta.erro")).to_contain_text("tem produtos cadastrados")

    pg.get_by_role("tab", name="Pedidos").click()
    pedido = pg.locator(".tabela tbody tr", has_text=COMPRA["cliente"])
    expect(pedido).to_be_visible()
    if COMPRA.get("pix_pela_loja"):
        expect(pedido.locator("[data-status]")).to_have_text("Aguardando Pix")
        pedido.get_by_role("button", name="Confirmar Pix").click()
    expect(pedido.locator("[data-status]")).to_have_text("Pago")
    pedido.get_by_role("button", name="Em separação").click()
    expect(pedido.locator("[data-status]")).to_have_text("Em separação")
    pedido.get_by_role("button", name="Concluído").click()
    expect(pg.locator(".toast")).to_contain_text("Concluído")
    pg.locator("select.filtro").select_option("todos")
    expect(pedido.locator("[data-status]")).to_have_text("Concluído")

    pg.get_by_role("tab", name="Resumo").click()
    expect(pg.locator(".kpis .numero")).to_have_count(4)
    expect(pg.locator(".kpis")).to_contain_text("Faturamento")
    expect(pg.get_by_role("heading", name="Mais vendidos")).to_be_visible()
    pg.get_by_role("button", name="30 dias").click()
    expect(pg.locator(".colunas .coluna")).to_have_count(30)

    vitrine = contexto(navegador).new_page()
    vitrine.goto(BASE + "/produtos")
    expect(vitrine.get_by_text(nome)).to_be_visible()
    expect(vitrine.get_by_text("exibindo dados de exemplo")).to_have_count(0)
    print("dono: login, produto, estoque, edição, categoria, pedidos, resumo e vitrine ok")


def cadastro_sem_cpf(navegador, erros):
    pg = contexto(navegador).new_page()
    pg.on("pageerror", lambda e: erros.append(str(e)))
    sufixo = uuid.uuid4().hex[:8]
    pg.goto(BASE + "/cadastro")
    pg.fill("input[formcontrolname=nome]", f"Sem CPF {sufixo}")
    pg.fill("input[formcontrolname=dataNascimento]", "1991-03-20")
    pg.fill("input[formcontrolname=email]", f"semcpf-{sufixo}@teste.com")
    pg.fill("input[formcontrolname=senha]", "segredo123")
    pg.fill("input[formcontrolname=confirmacao]", "segredo123")
    pg.click("button[type=submit]")
    pg.wait_for_url("**/minha-conta**")
    expect(pg.locator(".sem-cpf")).to_contain_text("Não informado")
    pg.locator(".sem-cpf").get_by_role("button", name="cadastrar").click()
    cpf = cpf_valido()
    pg.locator("form.mini-form input[formcontrolname=cpf]").press_sequentially(cpf)
    pg.get_by_role("button", name="Salvar", exact=True).click()
    expect(pg.get_by_text("Dados atualizados.")).to_be_visible()
    expect(pg.locator("dl")).to_contain_text(f"***.{cpf[3:6]}.{cpf[6:9]}-**")
    print("cadastro sem CPF e CPF informado depois ok")


def tratamento_de_erros(navegador, erros):
    pg = contexto(navegador).new_page()
    pg.on("pageerror", lambda e: erros.append(str(e)))

    pg.goto(BASE + "/rota-que-nao-existe")
    expect(pg.get_by_role("heading", name="Página não encontrada")).to_be_visible()
    expect(pg).to_have_title(re.compile("Página não encontrada"))
    expect(pg.get_by_text("/rota-que-nao-existe")).to_be_visible()

    invalida = json.dumps({"token": "token-invalido", "tipo": "Bearer", "expiraEm": "2099-01-01T00:00:00Z",
                           "nome": "Sessão Antiga", "perfil": "CLIENTE"})
    pg.evaluate("s => localStorage.setItem('loudfeliz.sessao', s)", invalida)
    pg.goto(BASE + "/minha-conta")
    pg.wait_for_url("**/entrar?voltar=**")
    assert "minha-conta" in pg.url, pg.url
    expect(pg.locator(".aviso-global")).to_contain_text("Sua sessão expirou")
    assert pg.evaluate("localStorage.getItem('loudfeliz.sessao')") is None

    pg.evaluate("s => localStorage.setItem('loudfeliz.sessao', s)", invalida)
    pg.goto(BASE + "/produtos")
    expect(pg.locator(".aviso-global")).to_contain_text("Sua sessão expirou")
    expect(pg.locator("app-produto-card").first).to_be_visible()
    expect(pg.get_by_text("exibindo dados de exemplo")).to_have_count(0)
    pg.get_by_role("button", name="Fechar aviso").click()
    expect(pg.locator(".aviso-global")).to_have_count(0)
    print("erros: 404, sessão inválida na área restrita e na vitrine ok")


def main():
    erros = []
    with sync_playwright() as pw:
        navegador = pw.chromium.launch()
        try:
            cliente(navegador, erros)
            dono(navegador, erros)
            cadastro_sem_cpf(navegador, erros)
            tratamento_de_erros(navegador, erros)
        except Exception as e:
            mensagem = str(e).replace("%", "%25").replace("\n", "%0A")
            print(f"::error title=E2E::{mensagem[:3500]}")
            raise
        finally:
            navegador.close()
    if erros:
        print("::error title=Erros de JavaScript::" + " | ".join(erros)[:3000])
        sys.exit(1)
    print("E2E OK")


if __name__ == "__main__":
    main()
