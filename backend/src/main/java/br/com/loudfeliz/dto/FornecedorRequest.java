package br.com.loudfeliz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
        @NotBlank @Size(max = 160) String razaoSocial,
        @NotBlank @Pattern(regexp = "\\d{14}", message = "deve conter 14 dígitos") String cnpj,
        @Size(max = 20) String telefone,
        @Email @Size(max = 160) String email) {
}
