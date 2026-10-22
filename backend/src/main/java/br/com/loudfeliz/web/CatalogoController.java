package br.com.loudfeliz.web;

import br.com.loudfeliz.dto.CategoriaResponse;
import br.com.loudfeliz.dto.ComboResponse;
import br.com.loudfeliz.dto.LojaStatusResponse;
import br.com.loudfeliz.dto.ProdutoResponse;
import br.com.loudfeliz.dto.TaxaEntregaResponse;
import br.com.loudfeliz.servico.CadastroServico;
import br.com.loudfeliz.servico.EntregaServico;
import br.com.loudfeliz.servico.HorarioServico;
import br.com.loudfeliz.servico.ProdutoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. Catálogo e loja (público)")
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final ProdutoServico produtos;
    private final CadastroServico cadastros;
    private final HorarioServico horarios;
    private final EntregaServico entregas;

    public CatalogoController(ProdutoServico produtos, CadastroServico cadastros, HorarioServico horarios, EntregaServico entregas) {
        this.produtos = produtos;
        this.cadastros = cadastros;
        this.horarios = horarios;
        this.entregas = entregas;
    }

    @Operation(summary = "Lista produtos ativos, com filtro opcional por categoria e busca")
    @GetMapping("/produtos")
    public List<ProdutoResponse> listarProdutos(@RequestParam(required = false) Long categoriaId,
                                                @RequestParam(required = false) String busca) {
        return produtos.listarAtivos(categoriaId, busca);
    }

    @GetMapping("/produtos/{id}")
    public ProdutoResponse buscarProduto(@PathVariable Long id) {
        return produtos.buscarAtivo(id);
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return cadastros.listarCategorias();
    }

    @Operation(summary = "Combos ativos com desconto automático (RN03)")
    @GetMapping("/combos")
    public List<ComboResponse> listarCombos() {
        return cadastros.listarCombos(true);
    }

    @Operation(summary = "Loja aberta, delivery disponível (RN04) e valor mínimo (RN05)")
    @GetMapping("/loja/status")
    public LojaStatusResponse status() {
        return horarios.status();
    }

    @Operation(summary = "Bairros atendidos e taxas de entrega (RN09)")
    @GetMapping("/entrega/regioes")
    public List<TaxaEntregaResponse> regioes() {
        return entregas.listar();
    }

    @Operation(summary = "Consulta a taxa de entrega de um bairro (RN09)")
    @GetMapping("/entrega/taxa")
    public TaxaEntregaResponse taxa(@RequestParam String bairro) {
        return entregas.consultar(bairro);
    }
}
