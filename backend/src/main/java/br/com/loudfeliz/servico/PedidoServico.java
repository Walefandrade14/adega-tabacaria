package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Cargo;
import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.Endereco;
import br.com.loudfeliz.dominio.FormaPagamento;
import br.com.loudfeliz.dominio.Funcionario;
import br.com.loudfeliz.dominio.ItemPedido;
import br.com.loudfeliz.dominio.Pagamento;
import br.com.loudfeliz.dominio.Pedido;
import br.com.loudfeliz.dominio.StatusPagamento;
import br.com.loudfeliz.dominio.StatusPedido;
import br.com.loudfeliz.dominio.TipoEntrega;
import br.com.loudfeliz.dto.CalculoResponse;
import br.com.loudfeliz.dto.CarrinhoRequest;
import br.com.loudfeliz.dto.PagamentoRequest;
import br.com.loudfeliz.dto.PedidoResponse;
import br.com.loudfeliz.dto.VendaBalcaoRequest;
import br.com.loudfeliz.dto.VerificacaoBalcaoRequest;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RegraNegocioException;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import br.com.loudfeliz.repositorio.ClienteRepositorio;
import br.com.loudfeliz.repositorio.EnderecoRepositorio;
import br.com.loudfeliz.repositorio.FuncionarioRepositorio;
import br.com.loudfeliz.repositorio.PagamentoRepositorio;
import br.com.loudfeliz.repositorio.PedidoRepositorio;
import br.com.loudfeliz.util.Cpf;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoServico {

    private static final Set<StatusPedido> ESTOQUE_JA_BAIXADO = EnumSet.of(StatusPedido.PAGO, StatusPedido.EM_SEPARACAO, StatusPedido.EM_ENTREGA);

    private final PedidoRepositorio pedidos;
    private final ClienteRepositorio clientes;
    private final EnderecoRepositorio enderecos;
    private final FuncionarioRepositorio funcionarios;
    private final PagamentoRepositorio pagamentos;
    private final CalculadoraCarrinho calculadora;
    private final PagamentoServico pagamentoServico;
    private final EstoqueServico estoque;
    private final OfertaDiariaServico ofertas;

    public PedidoServico(PedidoRepositorio pedidos, ClienteRepositorio clientes, EnderecoRepositorio enderecos,
                         FuncionarioRepositorio funcionarios, PagamentoRepositorio pagamentos, CalculadoraCarrinho calculadora,
                         PagamentoServico pagamentoServico, EstoqueServico estoque, OfertaDiariaServico ofertas) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.enderecos = enderecos;
        this.funcionarios = funcionarios;
        this.pagamentos = pagamentos;
        this.calculadora = calculadora;
        this.pagamentoServico = pagamentoServico;
        this.estoque = estoque;
        this.ofertas = ofertas;
    }

    @Transactional(readOnly = true)
    public CalculoResponse simular(Long clienteId, CarrinhoRequest requisicao) {
        Cliente cliente = cliente(clienteId);
        validarTipoOnline(requisicao.tipoEntrega());
        Endereco endereco = enderecoDoPedido(clienteId, requisicao);
        return calculadora.calcular(cliente, CalculadoraCarrinho.agrupar(requisicao.itens()), requisicao.tipoEntrega(), endereco, false)
                .paraResposta();
    }

    @Transactional
    public PedidoResponse criar(Long clienteId, CarrinhoRequest requisicao) {
        Cliente cliente = cliente(clienteId);
        validarTipoOnline(requisicao.tipoEntrega());
        Endereco endereco = enderecoDoPedido(clienteId, requisicao);
        Calculo calculo = calculadora.calcular(cliente, CalculadoraCarrinho.agrupar(requisicao.itens()), requisicao.tipoEntrega(), endereco, false);
        calculo.exigirSemProblemas();
        Pedido pedido = montar(calculo, cliente, null, requisicao.tipoEntrega(), endereco);
        return PedidoResponse.de(pedidos.save(pedido));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarDoCliente(Long clienteId) {
        return pedidos.findByClienteIdOrderByIdDesc(clienteId).stream().map(PedidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarDoCliente(Long clienteId, Long pedidoId) {
        return PedidoResponse.de(pedidoDoCliente(clienteId, pedidoId));
    }

    @Transactional
    public PedidoResponse cancelarPeloCliente(Long clienteId, Long pedidoId) {
        Pedido pedido = pedidoDoCliente(clienteId, pedidoId);
        switch (pedido.getStatus()) {
            case ABERTO, AGUARDANDO_PAGAMENTO, PAGO -> cancelar(pedido, 100);
            case EM_SEPARACAO, EM_ENTREGA -> throw new RegraNegocioException("RN08",
                    "O pedido já está em separação ou em entrega. O cancelamento agora precisa ser aprovado pelo Dono ou pelo funcionário responsável.");
            default -> throw new RegraNegocioException("RN08", "Este pedido já foi finalizado.");
        }
        return PedidoResponse.de(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(StatusPedido status) {
        List<Pedido> lista = status == null ? pedidos.findAllByOrderByIdDesc() : pedidos.findByStatusOrderByIdDesc(status);
        return lista.stream().map(PedidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long pedidoId) {
        return PedidoResponse.de(pedido(pedidoId));
    }

    @Transactional
    public PedidoResponse alterarStatus(Long funcionarioId, Long pedidoId, StatusPedido novo) {
        Funcionario funcionario = funcionario(funcionarioId);
        Pedido pedido = pedido(pedidoId);
        StatusPedido atual = pedido.getStatus();
        boolean valido = switch (novo) {
            case EM_SEPARACAO -> atual == StatusPedido.PAGO;
            case EM_ENTREGA -> atual == StatusPedido.EM_SEPARACAO && pedido.getTipoEntrega() == TipoEntrega.DELIVERY;
            case CONCLUIDO -> atual == StatusPedido.EM_ENTREGA
                    || (atual == StatusPedido.EM_SEPARACAO && pedido.getTipoEntrega() != TipoEntrega.DELIVERY);
            default -> false;
        };
        if (!valido) {
            if (atual == StatusPedido.ABERTO || atual == StatusPedido.AGUARDANDO_PAGAMENTO) {
                throw new RegraNegocioException("RN07", "O pedido só pode seguir depois que o pagamento for aprovado.");
            }
            if (novo == StatusPedido.CANCELADO) {
                throw new RequisicaoInvalidaException("Para cancelar, use o endpoint de cancelamento.");
            }
            throw new RequisicaoInvalidaException("Não é possível mudar o pedido de " + atual + " para " + novo + ".");
        }
        if (novo == StatusPedido.EM_SEPARACAO && pedido.getFuncionario() == null) {
            pedido.setFuncionario(funcionario);
        }
        pedido.setStatus(novo);
        return PedidoResponse.de(pedido);
    }

    @Transactional
    public PedidoResponse cancelarPeloFuncionario(Long funcionarioId, Long pedidoId, Integer percentualEstorno) {
        Funcionario funcionario = funcionario(funcionarioId);
        Pedido pedido = pedido(pedidoId);
        if (pedido.getStatus() == StatusPedido.CONCLUIDO || pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("RN08", "Este pedido já foi finalizado.");
        }
        boolean depoisDaSeparacao = pedido.getStatus() == StatusPedido.EM_SEPARACAO || pedido.getStatus() == StatusPedido.EM_ENTREGA;
        boolean responsavel = pedido.getFuncionario() != null && pedido.getFuncionario().getId().equals(funcionario.getId());
        if (depoisDaSeparacao && funcionario.getCargo() != Cargo.DONO && !responsavel) {
            throw new RegraNegocioException("RN08", "Depois da separação, só o Dono ou o funcionário responsável pode aprovar o cancelamento.");
        }
        cancelar(pedido, percentualEstorno == null ? 100 : percentualEstorno);
        return PedidoResponse.de(pedido);
    }

    @Transactional(readOnly = true)
    public CalculoResponse verificarBalcao(VerificacaoBalcaoRequest requisicao) {
        return calculadora.calcular(clientePorCpf(requisicao.clienteCpf()), CalculadoraCarrinho.agrupar(requisicao.itens()),
                TipoEntrega.BALCAO, null, Boolean.TRUE.equals(requisicao.documentoConferido())).paraResposta();
    }

    @Transactional
    public PedidoResponse venderNoBalcao(Long funcionarioId, VendaBalcaoRequest requisicao) {
        Funcionario funcionario = funcionario(funcionarioId);
        Cliente cliente = clientePorCpf(requisicao.clienteCpf());
        Calculo calculo = calculadora.calcular(cliente, CalculadoraCarrinho.agrupar(requisicao.itens()), TipoEntrega.BALCAO, null,
                Boolean.TRUE.equals(requisicao.documentoConferido()));
        calculo.exigirSemProblemas();
        Pedido pedido = pedidos.save(montar(calculo, cliente, funcionario, TipoEntrega.BALCAO, null));
        pagamentoServico.pagar(pedido, new PagamentoRequest(requisicao.formaPagamento(), requisicao.simularRecusa()));
        return PedidoResponse.de(pedido);
    }

    private void cancelar(Pedido pedido, int percentualEstorno) {
        boolean estoqueBaixado = ESTOQUE_JA_BAIXADO.contains(pedido.getStatus());
        List<Pagamento> estornosParciais = new ArrayList<>();
        for (Pagamento pagamento : pedido.getPagamentos()) {
            if (pagamento.getStatus() == StatusPagamento.PENDENTE) {
                pagamento.setStatus(StatusPagamento.RECUSADO);
            } else if (pagamento.getStatus() == StatusPagamento.APROVADO) {
                if (percentualEstorno >= 100 || pagamento.getForma() == FormaPagamento.DINHEIRO) {
                    pagamento.setStatus(StatusPagamento.ESTORNADO);
                } else if (percentualEstorno > 0) {
                    Pagamento estorno = new Pagamento();
                    estorno.setPedido(pedido);
                    estorno.setForma(pagamento.getForma());
                    estorno.setStatus(StatusPagamento.ESTORNADO);
                    estorno.setValor(pagamento.getValor().multiply(BigDecimal.valueOf(percentualEstorno)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                    estorno.setTransacaoExternaId("ESTORNO-" + pagamento.getId());
                    estornosParciais.add(estorno);
                }
            }
        }
        for (Pagamento estorno : estornosParciais) {
            pedido.getPagamentos().add(pagamentos.save(estorno));
        }
        if (estoqueBaixado) {
            estoque.devolver(pedido);
            ofertas.estornar(pedido);
        }
        pedido.setStatus(StatusPedido.CANCELADO);
    }

    private Pedido montar(Calculo calculo, Cliente cliente, Funcionario funcionario, TipoEntrega tipo, Endereco endereco) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFuncionario(funcionario);
        pedido.setTipoEntrega(tipo);
        pedido.setEnderecoEntrega(endereco);
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setSubtotal(calculo.getSubtotal());
        pedido.setDescontoTotal(calculo.getDescontoTotal());
        pedido.setTaxaEntrega(calculo.getTaxaEntrega());
        pedido.setTotal(calculo.getTotal());
        for (Calculo.Linha linha : calculo.getLinhas()) {
            ItemPedido item = new ItemPedido();
            item.setPedido(pedido);
            item.setProduto(linha.produto());
            item.setQuantidade(linha.quantidade());
            item.setPrecoUnitario(linha.precoUnitario());
            item.setDescontoAplicado(linha.desconto().setScale(2, RoundingMode.HALF_UP));
            item.setCombo(linha.combo());
            pedido.getItens().add(item);
        }
        return pedido;
    }

    private void validarTipoOnline(TipoEntrega tipo) {
        if (tipo == TipoEntrega.BALCAO) {
            throw new RequisicaoInvalidaException("Venda de balcão é registrada pelo PDV.");
        }
    }

    private Endereco enderecoDoPedido(Long clienteId, CarrinhoRequest requisicao) {
        if (requisicao.tipoEntrega() != TipoEntrega.DELIVERY) {
            return null;
        }
        if (requisicao.enderecoId() != null) {
            return enderecos.findByIdAndClienteId(requisicao.enderecoId(), clienteId)
                    .orElseThrow(() -> new NaoEncontradoException("Endereço não encontrado."));
        }
        return enderecos.findByClienteIdOrderByPrincipalDescIdAsc(clienteId).stream().findFirst().orElse(null);
    }

    private Cliente clientePorCpf(String cpf) {
        String digitos = Cpf.somenteDigitos(cpf);
        return digitos.isEmpty() ? null : clientes.findByCpf(digitos).orElse(null);
    }

    private Cliente cliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado."));
    }

    private Funcionario funcionario(Long id) {
        return funcionarios.findById(id).orElseThrow(() -> new NaoEncontradoException("Funcionário não encontrado."));
    }

    private Pedido pedido(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
    }

    private Pedido pedidoDoCliente(Long clienteId, Long pedidoId) {
        return pedidos.findByIdAndClienteId(pedidoId, clienteId).orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado."));
    }
}
