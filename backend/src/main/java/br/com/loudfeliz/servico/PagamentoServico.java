package br.com.loudfeliz.servico;

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

    public PagamentoServico(PedidoRepositorio pedidos, PagamentoRepositorio pagamentos, EstoqueServico estoque, OfertaDiariaServico ofertas) {
        this.pedidos = pedidos;
        this.pagamentos = pagamentos;
        this.estoque = estoque;
        this.ofertas = ofertas;
    }

    @Transactional
    public PagamentoResponse pagarComoCliente(Long clienteId, Long pedidoId, PagamentoRequest requisicao) {
        Pedido pedido = pedidos.findByIdAndClienteId(pedidoId, clienteId)
                .orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
        return PagamentoResponse.de(pagar(pedido, requisicao));
    }

    @Transactional
    public PagamentoResponse pagarComoFuncionario(Long pedidoId, PagamentoRequest requisicao) {
        Pedido pedido = pedidos.findById(pedidoId).orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
        return PagamentoResponse.de(pagar(pedido, requisicao));
    }

    @Transactional
    public PagamentoResponse confirmarPix(Long pagamentoId, Long clienteId) {
        Pagamento pagamento = pagamentos.findById(pagamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Pagamento não encontrado."));
        Pedido pedido = pagamento.getPedido();
        if (clienteId != null && (pedido.getCliente() == null || !pedido.getCliente().getId().equals(clienteId))) {
            throw new NaoEncontradoException("Pagamento não encontrado.");
        }
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
            case PIX -> pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
            case DINHEIRO -> aprovar(pedido, pagamento);
            case CARTAO_CREDITO, CARTAO_DEBITO -> {
                if (Boolean.TRUE.equals(requisicao.simularRecusa())) {
                    pagamento.setStatus(StatusPagamento.RECUSADO);
                    pedido.setStatus(StatusPedido.ABERTO);
                } else {
                    aprovar(pedido, pagamento);
                }
            }
        }
        return pagamento;
    }

    private void aprovar(Pedido pedido, Pagamento pagamento) {
        estoque.baixar(pedido);
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
