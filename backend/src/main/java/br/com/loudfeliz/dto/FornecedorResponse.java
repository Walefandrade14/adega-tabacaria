package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Fornecedor;

public record FornecedorResponse(Long id, String razaoSocial, String cnpj, String telefone, String email) {

    public static FornecedorResponse de(Fornecedor f) {
        return new FornecedorResponse(f.getId(), f.getRazaoSocial(), f.getCnpj(), f.getTelefone(), f.getEmail());
    }
}
