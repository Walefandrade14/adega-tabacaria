package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.CategoriaProduto;

public record CategoriaResponse(Long id, String nome) {

    public static CategoriaResponse de(CategoriaProduto c) {
        return new CategoriaResponse(c.getId(), c.getNome());
    }
}
