package br.com.loudfeliz.web;

import br.com.loudfeliz.dominio.StatusPedido;
import br.com.loudfeliz.dto.CalculoResponse;
import br.com.loudfeliz.dto.CancelamentoRequest;
import br.com.loudfeliz.dto.PagamentoRequest;
import br.com.loudfeliz.dto.PagamentoResponse;
import br.com.loudfeliz.dto.PedidoResponse;
import br.com.loudfeliz.dto.StatusRequest;
import br.com.loudfeliz.dto.VendaBalcaoRequest;
import br.com.loudfeliz.dto.VerificacaoBalcaoRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6. Admin: pedidos e PDV")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/admin")
public class AdminPedidoController {

    private final PedidoServico pedidos;
    private final PagamentoServico pagamentos;

    public AdminPedidoController(PedidoServico pedidos, PagamentoServico pagamentos) {
        this.pedidos = pedidos;
        this.pagamentos = pagamentos;
    }

    @GetMapping("/pedidos")
    public List<PedidoResponse> listar(@RequestParam(required = false) StatusPedido status) {
        return pedidos.listar(status);
    }

    @GetMapping("/pedidos/{id}")
    public PedidoResponse buscar(@PathVariable Long id) {
        return pedidos.buscar(id);
    }

    @Operation(summary = "Avança o pedido: PAGO → EM_SEPARACAO → EM_ENTREGA → CONCLUIDO")
    @PatchMapping("/pedidos/{id}/status")
    public PedidoResponse alterarStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody StatusRequest requisicao) {
        return pedidos.alterarStatus(Usuario.id(jwt), id, requisicao.status());
    }

    @Operation(summary = "Cancela com estorno proporcional; após a separação exige Dono ou responsável (RN08)")
    @PostMapping("/pedidos/{id}/cancelar")
    public PedidoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody(required = false) CancelamentoRequest requisicao) {
        return pedidos.cancelarPeloFuncionario(Usuario.id(jwt), id, requisicao == null ? null : requisicao.percentualEstorno());
    }

    @PostMapping("/pedidos/{id}/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse pagar(@PathVariable Long id, @Valid @RequestBody PagamentoRequest requisicao) {
        return pagamentos.pagarComoFuncionario(id, requisicao);
    }

    @PostMapping("/pagamentos/{id}/confirmar-pix")
    public PagamentoResponse confirmarPix(@PathVariable Long id) {
        return pagamentos.confirmarPix(id, null);
    }

    @Operation(summary = "PDV: confere estoque, combos e se precisa checar o RG (alerta visual da RN01)")
    @PostMapping("/pdv/verificar")
    public CalculoResponse verificar(@Valid @RequestBody VerificacaoBalcaoRequest requisicao) {
        return pedidos.verificarBalcao(requisicao);
    }

    @Operation(summary = "PDV: registra venda de balcão e o pagamento")
    @PostMapping("/pdv/vendas")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse vender(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VendaBalcaoRequest requisicao) {
        return pedidos.venderNoBalcao(Usuario.id(jwt), requisicao);
    }
}
