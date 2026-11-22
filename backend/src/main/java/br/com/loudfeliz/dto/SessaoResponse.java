package br.com.loudfeliz.dto;

import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

public record SessaoResponse(String tipo, Instant expiraEm, String nome, String perfil) {

    public static SessaoResponse de(Jwt jwt) {
        List<String> perfis = jwt.getClaimAsStringList("roles");
        String perfil = perfis == null || perfis.isEmpty() ? null : perfis.get(0);
        return new SessaoResponse("Bearer", jwt.getExpiresAt(), jwt.getClaimAsString("nome"), perfil);
    }
}
