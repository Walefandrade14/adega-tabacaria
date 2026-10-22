package br.com.loudfeliz.web;

import br.com.loudfeliz.dto.CadastroRequest;
import br.com.loudfeliz.dto.LoginFuncionarioRequest;
import br.com.loudfeliz.dto.LoginRequest;
import br.com.loudfeliz.dto.TokenResponse;
import br.com.loudfeliz.servico.AutenticacaoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    public AutenticacaoController(AutenticacaoServico autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Operation(summary = "Cadastro de cliente (CPF e data de nascimento obrigatórios — RN01/RN06)")
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        return autenticacao.cadastrar(requisicao);
    }

    @Operation(summary = "Login de cliente")
    @PostMapping("/login")
    public TokenResponse entrar(@Valid @RequestBody LoginRequest requisicao) {
        return autenticacao.entrarCliente(requisicao);
    }

    @Operation(summary = "Login de funcionário (dono, caixa, entregador, estoque)")
    @PostMapping("/funcionarios/login")
    public TokenResponse entrarFuncionario(@Valid @RequestBody LoginFuncionarioRequest requisicao) {
        return autenticacao.entrarFuncionario(requisicao);
    }
}
