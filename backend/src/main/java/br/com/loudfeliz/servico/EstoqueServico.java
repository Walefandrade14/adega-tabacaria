package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.ItemPedido;
import br.com.loudfeliz.dominio.Pedido;
import br.com.loudfeliz.excecao.RegraNegocioException;
import br.com.loudfeliz.repositorio.ProdutoRepositorio;
import org.springframework.stereotype.Service;

@Service
public class EstoqueServico {

    private final ProdutoRepositorio produtos;

    public EstoqueServico(ProdutoRepositorio produtos) {
        this.produtos = produtos;
    }

    public void baixar(Pedido pedido) {
        for (ItemPedido item : pedido.getItens()) {
            if (produtos.baixarEstoque(item.getProduto().getId(), item.getQuantidade()) == 0) {
                throw new RegraNegocioException("RN02", "Estoque insuficiente para '" + item.getProduto().getNome()
                        + "' no momento da confirmação. O pedido continua em aberto.");
            }
        }
    }

    public void devolver(Pedido pedido) {
        for (ItemPedido item : pedido.getItens()) {
            produtos.devolverEstoque(item.getProduto().getId(), item.getQuantidade());
        }
    }
}
