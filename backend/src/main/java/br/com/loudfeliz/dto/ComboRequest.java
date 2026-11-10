package br.com.loudfeliz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ComboRequest(
        @NotBlank @Size(max = 120) String nome,
        @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal descontoPercentual,
        @DecimalMin("0.01") BigDecimal descontoValorFixo,
        Boolean ativo,
        @NotEmpty List<@Valid ComboItemRequest> itens) {
}
