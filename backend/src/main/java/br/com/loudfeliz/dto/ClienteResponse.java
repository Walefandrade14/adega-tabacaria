package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.Fuso;
import java.time.LocalDate;

public record ClienteResponse(Long id, String nome, String email, String telefone, LocalDate dataNascimento, boolean maiorDeIdade) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelefone(), c.getDataNascimento(), c.idadeEm(Fuso.hoje()) >= 18);
    }
}
