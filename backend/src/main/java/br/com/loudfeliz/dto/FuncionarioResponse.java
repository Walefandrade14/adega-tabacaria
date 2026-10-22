package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Cargo;
import br.com.loudfeliz.dominio.Funcionario;

public record FuncionarioResponse(Long id, String nome, Cargo cargo, String usuario, boolean ativo) {

    public static FuncionarioResponse de(Funcionario f) {
        return new FuncionarioResponse(f.getId(), f.getNome(), f.getCargo(), f.getUsuario(), f.isAtivo());
    }
}
