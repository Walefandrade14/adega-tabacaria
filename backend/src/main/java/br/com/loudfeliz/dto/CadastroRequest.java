package br.com.loudfeliz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CadastroRequest(
        @NotBlank @Size(min = 2, max = 120) String nome,
        @NotBlank String cpf,
        @NotNull @Past LocalDate dataNascimento,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 6, max = 72) String senha,
        @Size(max = 20) String telefone) {
}
