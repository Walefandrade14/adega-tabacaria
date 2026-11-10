package br.com.loudfeliz.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SegurancaConfig {

    private static final Logger log = LoggerFactory.getLogger(SegurancaConfig.class);
    private static final String SEGREDO_ANTIGO = "troque-este-segredo-em-producao-loud-feliz-no-simples";

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, ValidadorDeSessao validador,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolvedor) throws Exception {
        AuthenticationEntryPoint naoAutenticado = (req, res, erro) -> {
            new BearerTokenAuthenticationEntryPoint().commence(req, res, erro);
            resolvedor.resolveException(req, res, null, erro);
        };
        AccessDeniedHandler acessoNegado = (req, res, erro) -> {
            new BearerTokenAccessDeniedHandler().handle(req, res, erro);
            resolvedor.resolveException(req, res, null, erro);
        };
        Converter<Jwt, AbstractAuthenticationToken> conversor = conversorDeSessao(validador);
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error", "/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**", "/api/categorias/**", "/api/combos/**", "/api/loja/**", "/api/entrega/**").permitAll()
                        .requestMatchers("/api/admin/relatorios/**", "/api/admin/funcionarios/**", "/api/admin/configuracoes/**", "/api/admin/horarios/**", "/api/admin/taxas-entrega/**", "/api/admin/combos/**").hasRole("DONO")
                        .requestMatchers("/api/admin/produtos/**", "/api/admin/categorias/**", "/api/admin/fornecedores/**").hasAnyRole("DONO", "ESTOQUE")
                        .requestMatchers("/api/admin/pdv/**").hasAnyRole("DONO", "CAIXA")
                        .requestMatchers("/api/admin/**").hasAnyRole("DONO", "CAIXA", "ESTOQUE", "ENTREGADOR")
                        .requestMatchers("/api/clientes/**", "/api/pedidos/**", "/api/carrinho/**", "/api/pagamentos/**").hasRole("CLIENTE")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(naoAutenticado).accessDeniedHandler(acessoNegado))
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(acessoNegado));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey chaveJwt(@Value("${app.jwt.segredo:}") String segredo) {
        if (segredo == null || segredo.isBlank() || SEGREDO_ANTIGO.equals(segredo)) {
            log.warn("JWT_SECRET não definido: usando uma chave aleatória. As sessões caem sempre que a API reinicia. "
                    + "Defina JWT_SECRET (ou app.jwt.segredo no local.properties) com pelo menos 32 caracteres.");
            byte[] aleatoria = new byte[32];
            new SecureRandom().nextBytes(aleatoria);
            return new SecretKeySpec(aleatoria, "HmacSHA256");
        }
        if (segredo.length() < 32) {
            log.warn("JWT_SECRET tem menos de 32 caracteres: use um segredo mais longo em produção.");
        }
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(segredo.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(bytes, "HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static Converter<Jwt, AbstractAuthenticationToken> conversorDeSessao(ValidadorDeSessao validador) {
        JwtGrantedAuthoritiesConverter perfis = new JwtGrantedAuthoritiesConverter();
        perfis.setAuthoritiesClaimName("roles");
        perfis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(perfis);
        return jwt -> {
            validador.validar(jwt);
            return conversor.convert(jwt);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origens}") String origens) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(Arrays.stream(origens.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", cors);
        return fonte;
    }
}
