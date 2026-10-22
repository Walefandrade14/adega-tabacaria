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

    @Override
    public void run(ApplicationArguments args) {
        if (funcionarios.existsByUsuario(usuario)) {
            return;
        }
        Funcionario dono = new Funcionario();
        dono.setNome("Dono da loja");
        dono.setCpf("00000000000");
        dono.setCargo(Cargo.DONO);
        dono.setUsuario(usuario);
        dono.setSenhaHash(encoder.encode(senha));
        dono.setAtivo(true);
        funcionarios.save(dono);
    }
}
