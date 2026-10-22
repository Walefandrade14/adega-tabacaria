package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Produto;
import java.math.BigDecimal;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer estoqueQuantidade,
        CategoriaResponse categoria,
        Long fornecedorId,
        boolean restritoMaiorIdade,
        boolean ofertaEspecial,
        Integer limiteDiarioOferta,
        boolean ativo,
        String imagemUrl) {

    public static ProdutoResponse de(Produto p) {
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(), p.getEstoqueQuantidade(),
                CategoriaResponse.de(p.getCategoria()), p.getFornecedor() == null ? null : p.getFornecedor().getId(),
                p.isRestritoMaiorIdade(), p.isOfertaEspecial(), p.getLimiteDiarioOferta(), p.isAtivo(), p.getImagemUrl());
    }
}
