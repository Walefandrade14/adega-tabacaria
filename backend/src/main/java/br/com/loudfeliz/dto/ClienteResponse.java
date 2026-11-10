package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.Fuso;
import java.time.LocalDate;

public record ClienteResponse(Long id, String nome, String email, String telefone, LocalDate dataNascimento, boolean maiorDeIdade,
        String cpfMascarado) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelefone(), c.getDataNascimento(),
                c.idadeEm(Fuso.hoje()) >= 18, mascarar(c.getCpf()));
    }

    private static String mascarar(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return null;
        }
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
