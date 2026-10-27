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


def dono(navegador, erros):
    pg = contexto(navegador).new_page()
    pg.on("pageerror", lambda e: erros.append(str(e)))
    nome = f"Gin E2E {uuid.uuid4().hex[:6]}"
    categoria = f"Categoria E2E {uuid.uuid4().hex[:6]}"

    pg.goto(BASE + "/admin")
    pg.wait_for_url("**/admin/entrar**")
    pg.fill("input[formcontrolname=usuario]", "dono")
    pg.fill("input[formcontrolname=senha]", "admin123")
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

    vitrine = contexto(navegador).new_page()
    vitrine.goto(BASE + "/produtos")
    expect(vitrine.get_by_text(nome)).to_be_visible()
    expect(vitrine.get_by_text("exibindo dados de exemplo")).to_have_count(0)
    print("dono: login, produto, estoque, edição, categoria e vitrine ok")


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
