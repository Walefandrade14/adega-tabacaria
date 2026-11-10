package br.com.loudfeliz.servico;

import br.com.loudfeliz.config.ModoPagamento;
import br.com.loudfeliz.dominio.FormaPagamento;
import br.com.loudfeliz.dominio.Pagamento;
import br.com.loudfeliz.dominio.Pedido;
import br.com.loudfeliz.dominio.StatusPagamento;
import br.com.loudfeliz.dominio.StatusPedido;
import br.com.loudfeliz.dominio.TipoEntrega;
import br.com.loudfeliz.dto.PagamentoRequest;
import br.com.loudfeliz.dto.PagamentoResponse;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RegraNegocioException;
import br.com.loudfeliz.repositorio.PagamentoRepositorio;
import br.com.loudfeliz.repositorio.PedidoRepositorio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoServico {

    private final PedidoRepositorio pedidos;
    private final PagamentoRepositorio pagamentos;
    private final EstoqueServico estoque;
    private final OfertaDiariaServico ofertas;
    private final ModoPagamento modo;

    public PagamentoServico(PedidoRepositorio pedidos, PagamentoRepositorio pagamentos, EstoqueServico estoque,
            OfertaDiariaServico ofertas, ModoPagamento modo) {
        this.pedidos = pedidos;
        this.pagamentos = pagamentos;
        this.estoque = estoque;
        this.ofertas = ofertas;
        this.modo = modo;
    }

    @Transactional
    public PagamentoResponse pagarComoCliente(Long clienteId, Long pedidoId, PagamentoRequest requisicao) {
        Pedido pedido = pedidos.travar(pedidoId)
                .filter(p -> p.getCliente() != null && p.getCliente().getId().equals(clienteId))
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
        return PagamentoResponse.de(pagar(pedido, requisicao));
    }

    @Transactional
    public PagamentoResponse pagarComoFuncionario(Long pedidoId, PagamentoRequest requisicao) {
        Pedido pedido = pedidos.travar(pedidoId).orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
        return PagamentoResponse.de(pagar(pedido, requisicao));
    }

    @Transactional
    public PagamentoResponse confirmarPix(Long pagamentoId, Long clienteId) {
        Pedido pedido = pedidos.idDoPedidoDoPagamento(pagamentoId)
                .flatMap(pedidos::travar)
                .filter(p -> clienteId == null || (p.getCliente() != null && p.getCliente().getId().equals(clienteId)))
                .orElseThrow(() -> new NaoEncontradoException("Pagamento não encontrado."));
        if (clienteId != null && !modo.simulado()) {
            throw new RegraNegocioException("RN07", "Quem confirma o Pix é a loja, quando o valor cai na conta. Assim que confirmar, o pedido muda para Pago.");
        }
        Pagamento pagamento = pedido.getPagamentos().stream()
                .filter(p -> p.getId().equals(pagamentoId))
                .findFirst()
                .orElseThrow(() -> new NaoEncontradoException("Pagamento não encontrado."));
        if (pagamento.getForma() != FormaPagamento.PIX || pagamento.getStatus() != StatusPagamento.PENDENTE
                || pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("RN07", "Este pagamento não está aguardando confirmação.");
        }
        aprovar(pedido, pagamento);
        return PagamentoResponse.de(pagamento);
    }

    public Pagamento pagar(Pedido pedido, PagamentoRequest requisicao) {
        if (pedido.getStatus() != StatusPedido.ABERTO && pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException("RN07", "Este pedido não aceita novo pagamento (status " + pedido.getStatus() + ").");
        }
        pedido.getPagamentos().stream()
                .filter(p -> p.getStatus() == StatusPagamento.PENDENTE)
                .forEach(p -> p.setStatus(StatusPagamento.RECUSADO));

        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setForma(requisicao.forma());
        pagamento.setValor(pedido.getTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setTransacaoExternaId(prefixo(requisicao.forma()) + UUID.randomUUID());
        pedido.getPagamentos().add(pagamento);
        pagamentos.save(pagamento);

        switch (requisicao.forma()) {
            case PIX -> {
                reservarEstoque(pedido);
                pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
            }
            case DINHEIRO -> aprovar(pedido, pagamento);
            case CARTAO_CREDITO, CARTAO_DEBITO -> {
                if (modo.simulado() && Boolean.TRUE.equals(requisicao.simularRecusa())) {
                    pagamento.setStatus(StatusPagamento.RECUSADO);
                    liberarReserva(pedido);
                    pedido.setStatus(StatusPedido.ABERTO);
                } else {
                    aprovar(pedido, pagamento);
                }
            }
        }
        return pagamento;
    }

    private void reservarEstoque(Pedido pedido) {
        if (!pedido.isEstoqueReservado()) {
            estoque.baixar(pedido);
            pedido.setEstoqueReservado(true);
        }
    }

    private void liberarReserva(Pedido pedido) {
        if (pedido.isEstoqueReservado()) {
            estoque.devolver(pedido);
            pedido.setEstoqueReservado(false);
        }
    }

    private void aprovar(Pedido pedido, Pagamento pagamento) {
        reservarEstoque(pedido);
        ofertas.registrar(pedido);
        pagamento.setStatus(StatusPagamento.APROVADO);
        pedido.setStatus(pedido.getTipoEntrega() == TipoEntrega.BALCAO ? StatusPedido.CONCLUIDO : StatusPedido.PAGO);
    }

    private String prefixo(FormaPagamento forma) {
        return switch (forma) {
            case PIX -> "PIX-";
            case DINHEIRO -> "DIN-";
            case CARTAO_CREDITO, CARTAO_DEBITO -> "CARD-";
        };
    }
}
