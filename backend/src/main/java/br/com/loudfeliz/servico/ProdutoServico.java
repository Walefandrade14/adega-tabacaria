package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Produto;
import br.com.loudfeliz.dto.ProdutoRequest;
import br.com.loudfeliz.dto.ProdutoResponse;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RegraNegocioException;
import br.com.loudfeliz.repositorio.CategoriaRepositorio;
import br.com.loudfeliz.repositorio.FornecedorRepositorio;
import br.com.loudfeliz.repositorio.ProdutoRepositorio;
import br.com.loudfeliz.util.Texto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoServico {

    private final ProdutoRepositorio produtos;
    private final CategoriaRepositorio categorias;
    private final FornecedorRepositorio fornecedores;

    public ProdutoServico(ProdutoRepositorio produtos, CategoriaRepositorio categorias, FornecedorRepositorio fornecedores) {
        this.produtos = produtos;
        this.categorias = categorias;
        this.fornecedores = fornecedores;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarAtivos(Long categoriaId, String busca) {
        String termo = Texto.normalizar(busca);
        return produtos.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(p -> categoriaId == null || p.getCategoria().getId().equals(categoriaId))
                .filter(p -> termo.isEmpty() || Texto.normalizar(p.getNome()).contains(termo) || Texto.normalizar(p.getDescricao()).contains(termo))
                .map(ProdutoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarAtivo(Long id) {
        return produtos.findComCategoriaById(id).filter(Produto::isAtivo).map(ProdutoResponse::de)
                .orElseThrow(() -> new NaoEncontradoException("Produto não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarTodos() {
        return produtos.findAllByOrderByNomeAsc().stream().map(ProdutoResponse::de).toList();
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest requisicao) {
        Produto produto = new Produto();
        aplicar(produto, requisicao);
        return ProdutoResponse.de(produtos.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest requisicao) {
        Produto produto = obter(id);
        aplicar(produto, requisicao);
        return ProdutoResponse.de(produto);
    }

    @Transactional
    public ProdutoResponse ajustarEstoque(Long id, int ajuste) {
        Produto produto = obter(id);
        int novo = produto.getEstoqueQuantidade() + ajuste;
        if (novo < 0) {
            throw new RegraNegocioException("RN02", "O estoque de '" + produto.getNome() + "' não pode ficar negativo (atual: "
                    + produto.getEstoqueQuantidade() + ").");
        }
        produto.setEstoqueQuantidade(novo);
        return ProdutoResponse.de(produto);
    }

    @Transactional
    public void desativar(Long id) {
        obter(id).setAtivo(false);
    }

    private Produto obter(Long id) {
        return produtos.findComCategoriaById(id).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado."));
    }

    private void aplicar(Produto produto, ProdutoRequest requisicao) {
        produto.setNome(requisicao.nome().trim());
        produto.setDescricao(requisicao.descricao());
        produto.setPreco(requisicao.preco());
        produto.setEstoqueQuantidade(requisicao.estoqueQuantidade());
        produto.setCategoria(categorias.findById(requisicao.categoriaId())
                .orElseThrow(() -> new NaoEncontradoException("Categoria não encontrada.")));
        produto.setFornecedor(requisicao.fornecedorId() == null ? null : fornecedores.findById(requisicao.fornecedorId())
                .orElseThrow(() -> new NaoEncontradoException("Fornecedor não encontrado.")));
        produto.setRestritoMaiorIdade(Boolean.TRUE.equals(requisicao.restritoMaiorIdade()));
        produto.setOfertaEspecial(Boolean.TRUE.equals(requisicao.ofertaEspecial()));
        produto.setLimiteDiarioOferta(requisicao.limiteDiarioOferta());
        produto.setAtivo(requisicao.ativo() == null || requisicao.ativo());
        produto.setImagemUrl(requisicao.imagemUrl());
    }
}
