package br.com.loudfeliz.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TaxaEntregaRequest(
        @NotBlank @Size(max = 80) String bairro,
        @Pattern(regexp = "\\d{5}", message = "deve conter 5 dígitos") String cepPrefixo,
        @NotNull @DecimalMin("0.00") BigDecimal valor) {
}
