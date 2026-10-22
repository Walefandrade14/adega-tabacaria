package br.com.loudfeliz.web;

import br.com.loudfeliz.dto.CalculoResponse;
import br.com.loudfeliz.dto.CarrinhoRequest;
import br.com.loudfeliz.dto.ClienteResponse;
import br.com.loudfeliz.dto.EnderecoRequest;
import br.com.loudfeliz.dto.EnderecoResponse;
import br.com.loudfeliz.dto.PagamentoRequest;
import br.com.loudfeliz.dto.PagamentoResponse;
import br.com.loudfeliz.dto.PedidoResponse;
import br.com.loudfeliz.servico.ClienteServico;
import br.com.loudfeliz.servico.PagamentoServico;
import br.com.loudfeliz.servico.PedidoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3. Cliente: conta, carrinho e pedidos")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api")
public class ClienteController {

    private final ClienteServico clientes;
    private final PedidoServico pedidos;
    private final PagamentoServico pagamentos;

    public ClienteController(ClienteServico clientes, PedidoServico pedidos, PagamentoServico pagamentos) {
        this.clientes = clientes;
        this.pedidos = pedidos;
        this.pagamentos = pagamentos;
    }

    @GetMapping("/clientes/me")
    public ClienteResponse perfil(@AuthenticationPrincipal Jwt jwt) {
        return clientes.perfil(Usuario.id(jwt));
    }

    @GetMapping("/clientes/me/enderecos")
    public List<EnderecoResponse> enderecos(@AuthenticationPrincipal Jwt jwt) {
        return clientes.listarEnderecos(Usuario.id(jwt));
    }

    @PostMapping("/clientes/me/enderecos")
    @ResponseStatus(HttpStatus.CREATED)
    public EnderecoResponse adicionarEndereco(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EnderecoRequest requisicao) {
        return clientes.adicionarEndereco(Usuario.id(jwt), requisicao);
    }

    @DeleteMapping("/clientes/me/enderecos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerEndereco(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        clientes.removerEndereco(Usuario.id(jwt), id);
    }

    @Operation(summary = "Simula o carrinho: combos (RN03), taxa (RN09), mínimo (RN05) e problemas, sem gravar")
    @PostMapping("/carrinho/simular")
    public CalculoResponse simular(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CarrinhoRequest requisicao) {
        return pedidos.simular(Usuario.id(jwt), requisicao);
    }

    @Operation(summary = "Cria o pedido (fica ABERTO até o pagamento ser aprovado — RN07)")
    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criarPedido(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CarrinhoRequest requisicao) {
        return pedidos.criar(Usuario.id(jwt), requisicao);
    }

    @GetMapping("/pedidos")
    public List<PedidoResponse> meusPedidos(@AuthenticationPrincipal Jwt jwt) {
        return pedidos.listarDoCliente(Usuario.id(jwt));
    }

    @GetMapping("/pedidos/{id}")
    public PedidoResponse meuPedido(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidos.buscarDoCliente(Usuario.id(jwt), id);
    }

    @Operation(summary = "Paga o pedido. Cartão e dinheiro aprovam na hora; Pix fica pendente até a confirmação (simulada)")
    @PostMapping("/pedidos/{id}/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse pagar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody PagamentoRequest requisicao) {
        return pagamentos.pagarComoCliente(Usuario.id(jwt), id, requisicao);
    }

    @Operation(summary = "Simula a confirmação do Pix pelo gateway")
    @PostMapping("/pagamentos/{id}/confirmar-pix")
    public PagamentoResponse confirmarPix(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pagamentos.confirmarPix(id, Usuario.id(jwt));
    }

    @Operation(summary = "Cancela o pedido antes da separação, com estorno (RN08)")
    @PostMapping("/pedidos/{id}/cancelar")
    public PedidoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidos.cancelarPeloCliente(Usuario.id(jwt), id);
    }
}
