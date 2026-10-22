package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Endereco;

public record EnderecoResponse(
        Long id,
        String apelido,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String cep,
        boolean principal) {

    public static EnderecoResponse de(Endereco e) {
        if (e == null) {
            return null;
        }
        return new EnderecoResponse(e.getId(), e.getApelido(), e.getLogradouro(), e.getNumero(), e.getComplemento(),
                e.getBairro(), e.getCidade(), e.getUf(), e.getCep(), e.isPrincipal());
    }
}
