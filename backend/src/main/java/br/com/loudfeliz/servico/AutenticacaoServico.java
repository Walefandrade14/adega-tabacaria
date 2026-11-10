package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.Funcionario;
import br.com.loudfeliz.dto.CadastroRequest;
import br.com.loudfeliz.dto.LoginFuncionarioRequest;
import br.com.loudfeliz.dto.LoginRequest;
import br.com.loudfeliz.dto.TokenResponse;
import br.com.loudfeliz.excecao.ConflitoException;
import br.com.loudfeliz.excecao.CredenciaisInvalidasException;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import br.com.loudfeliz.repositorio.ClienteRepositorio;
import br.com.loudfeliz.repositorio.FuncionarioRepositorio;
import br.com.loudfeliz.util.Cpf;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoServico {

    private final ClienteRepositorio clientes;
    private final FuncionarioRepositorio funcionarios;
    private final PasswordEncoder encoder;
    private final TokenServico tokens;
    private final TentativasLogin tentativas;

    public AutenticacaoServico(ClienteRepositorio clientes, FuncionarioRepositorio funcionarios, PasswordEncoder encoder,
            TokenServico tokens, TentativasLogin tentativas) {
        this.clientes = clientes;
        this.funcionarios = funcionarios;
        this.encoder = encoder;
        this.tokens = tokens;
        this.tentativas = tentativas;
    }

    @Transactional
    public TokenResponse cadastrar(CadastroRequest requisicao) {
        String cpf = Cpf.somenteDigitos(requisicao.cpf());
        if (cpf.isEmpty()) {
            cpf = null;
        } else if (!Cpf.valido(cpf)) {
            throw new RequisicaoInvalidaException("CPF inválido.");
        }
        String email = requisicao.email().trim().toLowerCase(Locale.ROOT);
        if (clientes.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Este e-mail já está cadastrado.");
        }
        if (cpf != null && clientes.existsByCpf(cpf)) {
            throw new ConflitoException("Este CPF já está cadastrado.");
        }
        Cliente cliente = new Cliente();
        cliente.setNome(requisicao.nome().trim());
        cliente.setCpf(cpf);
        cliente.setDataNascimento(requisicao.dataNascimento());
        cliente.setEmail(email);
        cliente.setSenhaHash(encoder.encode(requisicao.senha()));
        cliente.setTelefone(requisicao.telefone());
        clientes.save(cliente);
        return tokens.gerar(cliente.getId(), cliente.getNome(), "cliente", "CLIENTE");
    }

    @Transactional(readOnly = true)
    public TokenResponse entrarCliente(LoginRequest requisicao) {
        String conta = "cliente:" + requisicao.email();
        tentativas.verificar(conta);
        Cliente cliente = clientes.findByEmailIgnoreCase(requisicao.email().trim())
                .filter(c -> encoder.matches(requisicao.senha(), c.getSenhaHash()))
                .orElse(null);
        if (cliente == null) {
            tentativas.falhou(conta);
            throw new CredenciaisInvalidasException("E-mail ou senha incorretos.");
        }
        tentativas.sucesso(conta);
        return tokens.gerar(cliente.getId(), cliente.getNome(), "cliente", "CLIENTE", cliente.getSenhaAlteradaEm());
    }

    @Transactional(readOnly = true)
    public TokenResponse entrarFuncionario(LoginFuncionarioRequest requisicao) {
        String conta = "funcionario:" + requisicao.usuario();
        tentativas.verificar(conta);
        Funcionario funcionario = funcionarios.findByUsuario(requisicao.usuario().trim())
                .filter(Funcionario::isAtivo)
                .filter(f -> encoder.matches(requisicao.senha(), f.getSenhaHash()))
                .orElse(null);
        if (funcionario == null) {
            tentativas.falhou(conta);
            throw new CredenciaisInvalidasException("Usuário ou senha incorretos.");
        }
        tentativas.sucesso(conta);
        return tokens.gerar(funcionario.getId(), funcionario.getNome(), "funcionario", funcionario.getCargo().name());
    }
}
