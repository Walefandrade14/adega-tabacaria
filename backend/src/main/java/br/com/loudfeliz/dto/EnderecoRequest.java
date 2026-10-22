package br.com.loudfeliz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @Size(max = 40) String apelido,
        @NotBlank @Size(max = 160) String logradouro,
        @NotBlank @Size(max = 10) String numero,
        @Size(max = 80) String complemento,
        @NotBlank @Size(max = 80) String bairro,
        @NotBlank @Size(max = 80) String cidade,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "deve ser a sigla do estado") String uf,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "deve conter 8 dígitos") String cep,
        boolean principal) {
}
