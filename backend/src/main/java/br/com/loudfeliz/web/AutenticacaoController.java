package br.com.loudfeliz.web;

import br.com.loudfeliz.config.CookieDeSessao;
import br.com.loudfeliz.dto.CadastroRequest;
import br.com.loudfeliz.dto.LoginFuncionarioRequest;
import br.com.loudfeliz.dto.LoginRequest;
import br.com.loudfeliz.dto.TokenResponse;
import br.com.loudfeliz.servico.AutenticacaoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. Autenticação")
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final AutenticacaoServico autenticacao;
    private final CookieDeSessao cookies;

    public AutenticacaoController(AutenticacaoServico autenticacao, CookieDeSessao cookies) {
        this.autenticacao = autenticacao;
        this.cookies = cookies;
    }

    @Operation(summary = "Cadastro de cliente (CPF opcional; data de nascimento obrigatória — RN01/RN06). Grava a sessão em cookie HttpOnly")
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse cadastrar(@Valid @RequestBody CadastroRequest requisicao, HttpServletRequest http, HttpServletResponse resposta) {
        return comCookie(autenticacao.cadastrar(requisicao), http, resposta);
    }

    @Operation(summary = "Login de cliente. O site usa o cookie HttpOnly; o token no corpo serve para Postman e Swagger")
    @PostMapping("/login")
    public TokenResponse entrar(@Valid @RequestBody LoginRequest requisicao, HttpServletRequest http, HttpServletResponse resposta) {
        return comCookie(autenticacao.entrarCliente(requisicao), http, resposta);
    }

    @Operation(summary = "Login de funcionário (dono, caixa, entregador, estoque)")
    @PostMapping("/funcionarios/login")
    public TokenResponse entrarFuncionario(@Valid @RequestBody LoginFuncionarioRequest requisicao, HttpServletRequest http, HttpServletResponse resposta) {
        return comCookie(autenticacao.entrarFuncionario(requisicao), http, resposta);
    }

    @Operation(summary = "Encerra a sessão do navegador apagando o cookie")
    @PostMapping("/sair")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(HttpServletRequest http, HttpServletResponse resposta) {
        cookies.apagar(http, resposta);
    }

    private TokenResponse comCookie(TokenResponse token, HttpServletRequest http, HttpServletResponse resposta) {
        cookies.gravar(http, resposta, token);
        return token;
    }
}
