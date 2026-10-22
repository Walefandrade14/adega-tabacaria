package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Funcionario;
import br.com.loudfeliz.dto.FuncionarioRequest;
import br.com.loudfeliz.dto.FuncionarioResponse;
import br.com.loudfeliz.excecao.ConflitoException;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import br.com.loudfeliz.repositorio.FuncionarioRepositorio;
import br.com.loudfeliz.util.Cpf;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncionarioServico {

    private final FuncionarioRepositorio funcionarios;
    private final PasswordEncoder encoder;

    public FuncionarioServico(FuncionarioRepositorio funcionarios, PasswordEncoder encoder) {
        this.funcionarios = funcionarios;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listar() {
        return funcionarios.findAllByOrderByNomeAsc().stream().map(FuncionarioResponse::de).toList();
    }

    @Transactional
    public FuncionarioResponse criar(FuncionarioRequest requisicao) {
        String cpf = Cpf.somenteDigitos(requisicao.cpf());
        if (!Cpf.valido(cpf)) {
            throw new RequisicaoInvalidaException("CPF inválido.");
        }
        if (funcionarios.existsByUsuario(requisicao.usuario().trim())) {
            throw new ConflitoException("Esse usuário já existe.");
        }
        if (funcionarios.existsByCpf(cpf)) {
            throw new ConflitoException("Já existe um funcionário com esse CPF.");
        }
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(requisicao.nome().trim());
        funcionario.setCpf(cpf);
        funcionario.setCargo(requisicao.cargo());
        funcionario.setUsuario(requisicao.usuario().trim());
        funcionario.setSenhaHash(encoder.encode(requisicao.senha()));
        funcionario.setAtivo(true);
        return FuncionarioResponse.de(funcionarios.save(funcionario));
    }

    @Transactional
    public FuncionarioResponse alterarAtivo(Long id, boolean ativo) {
        Funcionario funcionario = funcionarios.findById(id).orElseThrow(() -> new NaoEncontradoException("Funcionário não encontrado."));
        funcionario.setAtivo(ativo);
        return FuncionarioResponse.de(funcionario);
    }
}
