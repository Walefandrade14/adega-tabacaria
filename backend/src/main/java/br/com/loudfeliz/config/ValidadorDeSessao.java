package br.com.loudfeliz.config;

import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.Funcionario;
import br.com.loudfeliz.repositorio.ClienteRepositorio;
import br.com.loudfeliz.repositorio.FuncionarioRepositorio;
import br.com.loudfeliz.servico.TokenServico;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ValidadorDeSessao {

    private final ClienteRepositorio clientes;
    private final FuncionarioRepositorio funcionarios;

    public ValidadorDeSessao(ClienteRepositorio clientes, FuncionarioRepositorio funcionarios) {
        this.clientes = clientes;
        this.funcionarios = funcionarios;
    }

    @Transactional(readOnly = true)
    public void validar(Jwt jwt) {
        Long id = id(jwt);
        String tipo = jwt.getClaimAsString("tipo");
        if ("funcionario".equals(tipo)) {
            Funcionario funcionario = funcionarios.findById(id).filter(Funcionario::isAtivo).orElseThrow(ValidadorDeSessao::encerrada);
            List<String> perfis = jwt.getClaimAsStringList("roles");
            if (perfis == null || !perfis.contains(funcionario.getCargo().name())) {
                throw encerrada();
            }
        } else if ("cliente".equals(tipo)) {
            Cliente cliente = clientes.findById(id).orElseThrow(ValidadorDeSessao::encerrada);
            String versao = TokenServico.versaoDaSenha(cliente.getSenhaAlteradaEm());
            if (versao != null && !versao.equals(jwt.getClaimAsString(TokenServico.CLAIM_VERSAO_SENHA))) {
                throw encerrada();
            }
        } else {
            throw encerrada();
        }
    }

    private static Long id(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw encerrada();
        }
    }

    private static InvalidBearerTokenException encerrada() {
        return new InvalidBearerTokenException("Sessão encerrada");
    }
}
