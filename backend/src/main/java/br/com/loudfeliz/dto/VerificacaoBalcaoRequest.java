package br.com.loudfeliz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record VerificacaoBalcaoRequest(String clienteCpf, @NotEmpty List<@Valid ItemRequest> itens, Boolean documentoConferido) {
}
