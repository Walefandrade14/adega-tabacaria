package br.com.loudfeliz.config;

import br.com.loudfeliz.dominio.Cargo;
import br.com.loudfeliz.dominio.Funcionario;
import br.com.loudfeliz.repositorio.FuncionarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DadosIniciais implements ApplicationRunner {

    private final FuncionarioRepositorio funcionarios;
    private final PasswordEncoder encoder;
    private final String usuario;
    private final String senha;

    public DadosIniciais(FuncionarioRepositorio funcionarios, PasswordEncoder encoder,
                         @Value("${app.admin.usuario}") String usuario, @Value("${app.admin.senha}") String senha) {
        this.funcionarios = funcionarios;
        this.encoder = encoder;
        this.usuario = usuario;
        this.senha = senha;
    }

    private static final String CPF_DONO = "00000000000";

    @Override
    public void run(ApplicationArguments args) {
        Funcionario dono = funcionarios.findByCpf(CPF_DONO).orElseGet(Funcionario::new);
        boolean novo = dono.getId() == null;
        if (!novo && usuario.equals(dono.getUsuario()) && encoder.matches(senha, dono.getSenhaHash())) {
            return;
        }
        if (novo) {
            dono.setNome("Dono da loja");
            dono.setCpf(CPF_DONO);
            dono.setCargo(Cargo.DONO);
            dono.setAtivo(true);
        }
        dono.setUsuario(usuario);
        dono.setSenhaHash(encoder.encode(senha));
        funcionarios.save(dono);
    }
}
