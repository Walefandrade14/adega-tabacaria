package br.com.loudfeliz.web;

import br.com.loudfeliz.dto.RelatorioResponse;
import br.com.loudfeliz.servico.RelatorioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "7. Admin (Dono): relatórios")
@SecurityRequirement(name = "bearer")
@RestController
@RequestMapping("/api/admin/relatorios")
public class AdminRelatorioController {

    private final RelatorioServico relatorios;

    public AdminRelatorioController(RelatorioServico relatorios) {
        this.relatorios = relatorios;
    }

    @Operation(summary = "Vendas, ticket médio, canais, mais vendidos e estoque baixo no período (padrão: últimos 7 dias)")
    @GetMapping("/resumo")
    public RelatorioResponse resumo(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return relatorios.resumo(de, ate);
    }
}
