package br.com.loudfeliz.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank @Size(max = 160) String nome,
        String descricao,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal preco,
        @NotNull @Min(0) Integer estoqueQuantidade,
        @NotNull Long categoriaId,
        Long fornecedorId,
        Boolean restritoMaiorIdade,
        Boolean ofertaEspecial,
        @Min(1) Integer limiteDiarioOferta,
        Boolean ativo,
        @Size(max = 255) String imagemUrl) {
}
