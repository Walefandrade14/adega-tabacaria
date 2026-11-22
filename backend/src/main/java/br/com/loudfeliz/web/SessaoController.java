package br.com.loudfeliz.web;

import br.com.loudfeliz.dto.SessaoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. Autenticação")
@RestController
@SecurityRequirement(name = "bearer")
public class SessaoController {

    @Operation(summary = "Confere a sessão atual (cookie ou Bearer) e devolve nome, perfil e validade")
    @GetMapping("/api/sessao")
    public SessaoResponse sessao(@AuthenticationPrincipal Jwt jwt) {
        return SessaoResponse.de(jwt);
    }
}
