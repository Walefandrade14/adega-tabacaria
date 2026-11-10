package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Fuso;
import br.com.loudfeliz.dto.TokenResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenServico {

    public static final String CLAIM_VERSAO_SENHA = "sv";

    private final JwtEncoder encoder;
    private final long validadeHoras;

    public TokenServico(JwtEncoder encoder, @Value("${app.jwt.validade-horas}") long validadeHoras) {
        this.encoder = encoder;
        this.validadeHoras = validadeHoras;
    }

    public TokenResponse gerar(Long id, String nome, String tipo, String perfil) {
        return gerar(id, nome, tipo, perfil, null);
    }

    public TokenResponse gerar(Long id, String nome, String tipo, String perfil, LocalDateTime senhaAlteradaEm) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(validadeHoras, ChronoUnit.HOURS);
        JwtClaimsSet.Builder construtor = JwtClaimsSet.builder()
                .issuer("loud-feliz-api")
                .subject(String.valueOf(id))
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("nome", nome)
                .claim("tipo", tipo)
                .claim("roles", List.of(perfil));
        String versao = versaoDaSenha(senhaAlteradaEm);
        if (versao != null) {
            construtor.claim(CLAIM_VERSAO_SENHA, versao);
        }
        JwtClaimsSet claims = construtor.build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiraEm, nome, perfil);
    }

    public static String versaoDaSenha(LocalDateTime senhaAlteradaEm) {
        return senhaAlteradaEm == null ? null : String.valueOf(senhaAlteradaEm.atZone(Fuso.PADRAO).toEpochSecond());
    }
}
