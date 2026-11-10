package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.TipoEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CarrinhoRequest(@NotNull TipoEntrega tipoEntrega, Long enderecoId, @NotEmpty List<@Valid ItemRequest> itens) {
}
