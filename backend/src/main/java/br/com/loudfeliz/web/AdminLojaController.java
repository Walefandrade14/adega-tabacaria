package br.com.loudfeliz.web;

import br.com.loudfeliz.dto.ComboRequest;
import br.com.loudfeliz.dto.ComboResponse;
import br.com.loudfeliz.dto.ConfiguracaoRequest;
import br.com.loudfeliz.dto.ConfiguracaoResponse;
import br.com.loudfeliz.dto.FuncionarioRequest;
import br.com.loudfeliz.dto.FuncionarioResponse;
import br.com.loudfeliz.dto.HorarioRequest;
import br.com.loudfeliz.dto.HorarioResponse;
import br.com.loudfeliz.dto.TaxaEntregaRequest;
import br.com.loudfeliz.dto.TaxaEntregaResponse;
import br.com.loudfeliz.servico.CadastroServico;
import br.com.loudfeliz.servico.ConfiguracaoServico;
import br.com.loudfeliz.servico.EntregaServico;
import br.com.loudfeliz.servico.FuncionarioServico;
import br.com.loudfeliz.servico.HorarioServico;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "5. Admin (Dono): combos, horários, entrega, configurações e equipe")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/admin")
public class AdminLojaController {

    private final CadastroServico cadastros;
    private final HorarioServico horarios;
    private final EntregaServico entregas;
    private final ConfiguracaoServico configuracoes;
    private final FuncionarioServico funcionarios;

    public AdminLojaController(CadastroServico cadastros, HorarioServico horarios, EntregaServico entregas,
                               ConfiguracaoServico configuracoes, FuncionarioServico funcionarios) {
        this.cadastros = cadastros;
        this.horarios = horarios;
        this.entregas = entregas;
        this.configuracoes = configuracoes;
        this.funcionarios = funcionarios;
    }

    @GetMapping("/combos")
    public List<ComboResponse> listarCombos() {
        return cadastros.listarCombos(false);
    }

    @Operation(summary = "Cria combo com desconto percentual ou valor fixo (RN03)")
    @PostMapping("/combos")
    @ResponseStatus(HttpStatus.CREATED)
    public ComboResponse criarCombo(@Valid @RequestBody ComboRequest requisicao) {
        return cadastros.criarCombo(requisicao);
    }

    @PutMapping("/combos/{id}")
    public ComboResponse atualizarCombo(@PathVariable Long id, @Valid @RequestBody ComboRequest requisicao) {
        return cadastros.atualizarCombo(id, requisicao);
    }

    @DeleteMapping("/combos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativarCombo(@PathVariable Long id) {
        cadastros.desativarCombo(id);
    }

    @GetMapping("/horarios")
    public List<HorarioResponse> listarHorarios() {
        return horarios.listar();
    }

    @Operation(summary = "Define o horário de um dia (1 = segunda … 7 = domingo). Fechamento antes da abertura cruza a meia-noite (RN04)")
    @PutMapping("/horarios/{diaSemana}")
    public HorarioResponse atualizarHorario(@PathVariable int diaSemana, @Valid @RequestBody HorarioRequest requisicao) {
        return horarios.atualizar(diaSemana, requisicao);
    }

    @GetMapping("/taxas-entrega")
    public List<TaxaEntregaResponse> listarTaxas() {
        return entregas.listar();
    }

    @PostMapping("/taxas-entrega")
    @ResponseStatus(HttpStatus.CREATED)
    public TaxaEntregaResponse criarTaxa(@Valid @RequestBody TaxaEntregaRequest requisicao) {
        return entregas.criar(requisicao);
    }

    @PutMapping("/taxas-entrega/{id}")
    public TaxaEntregaResponse atualizarTaxa(@PathVariable Long id, @Valid @RequestBody TaxaEntregaRequest requisicao) {
        return entregas.atualizar(id, requisicao);
    }

    @DeleteMapping("/taxas-entrega/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerTaxa(@PathVariable Long id) {
        entregas.remover(id);
    }

    @GetMapping("/configuracoes")
    public List<ConfiguracaoResponse> listarConfiguracoes() {
        return configuracoes.listar();
    }

    @Operation(summary = "Altera valor_minimo_delivery (RN05) ou limite_padrao_oferta_dia (RN06)")
    @PutMapping("/configuracoes/{chave}")
    public ConfiguracaoResponse atualizarConfiguracao(@PathVariable String chave, @Valid @RequestBody ConfiguracaoRequest requisicao) {
        return configuracoes.atualizar(chave, requisicao.valor());
    }

    @GetMapping("/funcionarios")
    public List<FuncionarioResponse> listarFuncionarios() {
        return funcionarios.listar();
    }

    @PostMapping("/funcionarios")
    @ResponseStatus(HttpStatus.CREATED)
    public FuncionarioResponse criarFuncionario(@Valid @RequestBody FuncionarioRequest requisicao) {
        return funcionarios.criar(requisicao);
    }

    @PatchMapping("/funcionarios/{id}/ativo")
    public FuncionarioResponse alterarAtivo(@PathVariable Long id, @RequestParam boolean ativo) {
        return funcionarios.alterarAtivo(id, ativo);
    }
}
