package br.com.loudfeliz.web;

import br.com.loudfeliz.dto.CategoriaRequest;
import br.com.loudfeliz.dto.CategoriaResponse;
import br.com.loudfeliz.dto.EstoqueRequest;
import br.com.loudfeliz.dto.FornecedorRequest;
import br.com.loudfeliz.dto.FornecedorResponse;
import br.com.loudfeliz.dto.ProdutoRequest;
import br.com.loudfeliz.dto.ProdutoResponse;
import br.com.loudfeliz.servico.CadastroServico;
import br.com.loudfeliz.servico.ProdutoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4. Admin: produtos, categorias e fornecedores")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogoController {

    private final ProdutoServico produtos;
    private final CadastroServico cadastros;

    public AdminCatalogoController(ProdutoServico produtos, CadastroServico cadastros) {
        this.produtos = produtos;
        this.cadastros = cadastros;
    }

    @GetMapping("/produtos")
    public List<ProdutoResponse> listarProdutos() {
        return produtos.listarTodos();
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse criarProduto(@Valid @RequestBody ProdutoRequest requisicao) {
        return produtos.criar(requisicao);
    }

    @PutMapping("/produtos/{id}")
    public ProdutoResponse atualizarProduto(@PathVariable Long id, @Valid @RequestBody ProdutoRequest requisicao) {
        return produtos.atualizar(id, requisicao);
    }

    @Operation(summary = "Entrada (+) ou saída (−) manual de estoque; nunca fica negativo (RN02)")
    @PatchMapping("/produtos/{id}/estoque")
    public ProdutoResponse ajustarEstoque(@PathVariable Long id, @Valid @RequestBody EstoqueRequest requisicao) {
        return produtos.ajustarEstoque(id, requisicao.ajuste());
    }

    @Operation(summary = "Desativa o produto (some do catálogo, mas mantém o histórico)")
    @DeleteMapping("/produtos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativarProduto(@PathVariable Long id) {
        produtos.desativar(id);
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return cadastros.listarCategorias();
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse criarCategoria(@Valid @RequestBody CategoriaRequest requisicao) {
        return cadastros.criarCategoria(requisicao);
    }

    @PutMapping("/categorias/{id}")
    public CategoriaResponse atualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaRequest requisicao) {
        return cadastros.atualizarCategoria(id, requisicao);
    }

    @DeleteMapping("/categorias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCategoria(@PathVariable Long id) {
        cadastros.removerCategoria(id);
    }

    @GetMapping("/fornecedores")
    public List<FornecedorResponse> listarFornecedores() {
        return cadastros.listarFornecedores();
    }

    @PostMapping("/fornecedores")
    @ResponseStatus(HttpStatus.CREATED)
    public FornecedorResponse criarFornecedor(@Valid @RequestBody FornecedorRequest requisicao) {
        return cadastros.criarFornecedor(requisicao);
    }

    @PutMapping("/fornecedores/{id}")
    public FornecedorResponse atualizarFornecedor(@PathVariable Long id, @Valid @RequestBody FornecedorRequest requisicao) {
        return cadastros.atualizarFornecedor(id, requisicao);
    }

    @DeleteMapping("/fornecedores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerFornecedor(@PathVariable Long id) {
        cadastros.removerFornecedor(id);
    }
}
