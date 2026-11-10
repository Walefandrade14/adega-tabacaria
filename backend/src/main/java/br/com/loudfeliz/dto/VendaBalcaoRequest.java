package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record VendaBalcaoRequest(
        String clienteCpf,
        @NotEmpty List<@Valid ItemRequest> itens,
        @NotNull FormaPagamento formaPagamento,
        Boolean documentoConferido,
        Boolean simularRecusa) {
}
