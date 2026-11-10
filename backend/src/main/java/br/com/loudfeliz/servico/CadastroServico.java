package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.CategoriaProduto;
import br.com.loudfeliz.dominio.Combo;
import br.com.loudfeliz.dominio.ComboItem;
import br.com.loudfeliz.dominio.Fornecedor;
import br.com.loudfeliz.dominio.Produto;
import br.com.loudfeliz.dto.CategoriaRequest;
import br.com.loudfeliz.dto.CategoriaResponse;
import br.com.loudfeliz.dto.ComboItemRequest;
import br.com.loudfeliz.dto.ComboRequest;
import br.com.loudfeliz.dto.ComboResponse;
import br.com.loudfeliz.dto.FornecedorRequest;
import br.com.loudfeliz.dto.FornecedorResponse;
import br.com.loudfeliz.excecao.ConflitoException;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import br.com.loudfeliz.repositorio.CategoriaRepositorio;
import br.com.loudfeliz.repositorio.ComboRepositorio;
import br.com.loudfeliz.repositorio.FornecedorRepositorio;
import br.com.loudfeliz.repositorio.ProdutoRepositorio;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroServico {

    private final CategoriaRepositorio categorias;
    private final FornecedorRepositorio fornecedores;
    private final ComboRepositorio combos;
    private final ProdutoRepositorio produtos;

    public CadastroServico(CategoriaRepositorio categorias, FornecedorRepositorio fornecedores, ComboRepositorio combos,
                           ProdutoRepositorio produtos) {
        this.categorias = categorias;
        this.fornecedores = fornecedores;
        this.combos = combos;
        this.produtos = produtos;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categorias.findAllByOrderByNomeAsc().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional
    public CategoriaResponse criarCategoria(CategoriaRequest requisicao) {
        if (categorias.existsByNomeIgnoreCase(requisicao.nome().trim())) {
            throw new ConflitoException("Já existe uma categoria com esse nome.");
        }
        CategoriaProduto categoria = new CategoriaProduto();
        categoria.setNome(requisicao.nome().trim());
        return CategoriaResponse.de(categorias.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizarCategoria(Long id, CategoriaRequest requisicao) {
        CategoriaProduto categoria = categorias.findById(id).orElseThrow(() -> new NaoEncontradoException("Categoria não encontrada."));
        categoria.setNome(requisicao.nome().trim());
        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public void removerCategoria(Long id) {
        categorias.delete(categorias.findById(id).orElseThrow(() -> new NaoEncontradoException("Categoria não encontrada.")));
        categorias.flush();
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponse> listarFornecedores() {
        return fornecedores.findAllByOrderByRazaoSocialAsc().stream().map(FornecedorResponse::de).toList();
    }

    @Transactional
    public FornecedorResponse criarFornecedor(FornecedorRequest requisicao) {
        Fornecedor fornecedor = new Fornecedor();
        aplicar(fornecedor, requisicao);
        return FornecedorResponse.de(fornecedores.save(fornecedor));
    }

    @Transactional
    public FornecedorResponse atualizarFornecedor(Long id, FornecedorRequest requisicao) {
        Fornecedor fornecedor = fornecedores.findById(id).orElseThrow(() -> new NaoEncontradoException("Fornecedor não encontrado."));
        aplicar(fornecedor, requisicao);
        return FornecedorResponse.de(fornecedor);
    }

    @Transactional
    public void removerFornecedor(Long id) {
        fornecedores.delete(fornecedores.findById(id).orElseThrow(() -> new NaoEncontradoException("Fornecedor não encontrado.")));
        fornecedores.flush();
    }

    @Transactional(readOnly = true)
    public List<ComboResponse> listarCombos(boolean somenteAtivos) {
        List<Combo> lista = somenteAtivos ? combos.findByAtivoTrueOrderByIdAsc() : combos.findAllByOrderByIdAsc();
        return lista.stream().map(ComboResponse::de).toList();
    }

    @Transactional
    public ComboResponse criarCombo(ComboRequest requisicao) {
        Combo combo = new Combo();
        aplicar(combo, requisicao);
        return ComboResponse.de(combos.save(combo));
    }

    @Transactional
    public ComboResponse atualizarCombo(Long id, ComboRequest requisicao) {
        Combo combo = combos.findComItensById(id).orElseThrow(() -> new NaoEncontradoException("Combo não encontrado."));
        combo.getItens().clear();
        combos.flush();
        aplicar(combo, requisicao);
        return ComboResponse.de(combos.save(combo));
    }

    @Transactional
    public void desativarCombo(Long id) {
        combos.findById(id).orElseThrow(() -> new NaoEncontradoException("Combo não encontrado.")).setAtivo(false);
    }

    private void aplicar(Fornecedor fornecedor, FornecedorRequest requisicao) {
        fornecedor.setRazaoSocial(requisicao.razaoSocial().trim());
        fornecedor.setCnpj(requisicao.cnpj());
        fornecedor.setTelefone(requisicao.telefone());
        fornecedor.setEmail(requisicao.email());
    }

    private void aplicar(Combo combo, ComboRequest requisicao) {
        if (requisicao.descontoPercentual() == null && requisicao.descontoValorFixo() == null) {
            throw new RequisicaoInvalidaException("Informe o desconto percentual ou o valor fixo do combo.");
        }
        combo.setNome(requisicao.nome().trim());
        combo.setDescontoPercentual(requisicao.descontoPercentual());
        combo.setDescontoValorFixo(requisicao.descontoPercentual() == null ? requisicao.descontoValorFixo() : null);
        combo.setAtivo(requisicao.ativo() == null || requisicao.ativo());
        Set<Long> vistos = new HashSet<>();
        for (ComboItemRequest itemRequisicao : requisicao.itens()) {
            if (!vistos.add(itemRequisicao.produtoId())) {
                throw new RequisicaoInvalidaException("O mesmo produto aparece duas vezes no combo.");
            }
            Produto produto = produtos.findById(itemRequisicao.produtoId())
                    .orElseThrow(() -> new NaoEncontradoException("Produto " + itemRequisicao.produtoId() + " não encontrado."));
            ComboItem item = new ComboItem();
            item.setCombo(combo);
            item.setProduto(produto);
            item.setQuantidade(itemRequisicao.quantidade());
            combo.getItens().add(item);
        }
    }
}
