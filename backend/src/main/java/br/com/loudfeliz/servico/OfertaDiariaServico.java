package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.CompraOfertaDiaria;
import br.com.loudfeliz.dominio.ItemPedido;
import br.com.loudfeliz.dominio.Pedido;
import br.com.loudfeliz.dominio.Produto;
import br.com.loudfeliz.excecao.RegraNegocioException;
import br.com.loudfeliz.repositorio.CompraOfertaDiariaRepositorio;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class OfertaDiariaServico {

    private final CompraOfertaDiariaRepositorio compras;
    private final ConfiguracaoServico configuracoes;

    public OfertaDiariaServico(CompraOfertaDiariaRepositorio compras, ConfiguracaoServico configuracoes) {
        this.compras = compras;
        this.configuracoes = configuracoes;
    }

    public int compradoNoDia(Long clienteId, Long produtoId, LocalDate dia) {
        return compras.findByClienteIdAndProdutoIdAndDataReferencia(clienteId, produtoId, dia)
                .map(CompraOfertaDiaria::getQuantidade)
                .orElse(0);
    }

    public void registrar(Pedido pedido) {
        ajustar(pedido, 1);
    }

    public void estornar(Pedido pedido) {
        ajustar(pedido, -1);
    }

    private void ajustar(Pedido pedido, int sinal) {
        Cliente cliente = pedido.getCliente();
        if (cliente == null) {
            return;
        }
        LocalDate dia = pedido.getCriadoEm().toLocalDate();
        for (ItemPedido item : pedido.getItens()) {
            Produto produto = item.getProduto();
            if (!produto.isOfertaEspecial()) {
                continue;
            }
            CompraOfertaDiaria compra = compras.findByClienteIdAndProdutoIdAndDataReferencia(cliente.getId(), produto.getId(), dia)
                    .orElseGet(() -> {
                        CompraOfertaDiaria nova = new CompraOfertaDiaria();
                        nova.setCliente(cliente);
                        nova.setProduto(produto);
                        nova.setDataReferencia(dia);
                        return nova;
                    });
            int novaQuantidade = compra.getQuantidade() + sinal * item.getQuantidade();
            if (sinal > 0) {
                int limite = configuracoes.limiteOferta(produto);
                if (novaQuantidade > limite) {
                    throw new RegraNegocioException("RN06", "'" + produto.getNome() + "' é oferta especial: limite de "
                            + limite + " unidade(s) por CPF por dia.");
                }
            }
            compra.setQuantidade(Math.max(0, novaQuantidade));
            compras.save(compra);
        }
    }
}
