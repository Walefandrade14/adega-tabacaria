package br.com.loudfeliz.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, JwtAuthenticationConverter conversor) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error", "/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**", "/api/categorias/**", "/api/combos/**", "/api/loja/**", "/api/entrega/**").permitAll()
                        .requestMatchers("/api/admin/funcionarios/**", "/api/admin/configuracoes/**", "/api/admin/horarios/**", "/api/admin/taxas-entrega/**", "/api/admin/combos/**").hasRole("DONO")
                        .requestMatchers("/api/admin/produtos/**", "/api/admin/categorias/**", "/api/admin/fornecedores/**").hasAnyRole("DONO", "ESTOQUE")
                        .requestMatchers("/api/admin/pdv/**").hasAnyRole("DONO", "CAIXA")
                        .requestMatchers("/api/admin/**").hasAnyRole("DONO", "CAIXA", "ESTOQUE", "ENTREGADOR")
                        .requestMatchers("/api/clientes/**", "/api/pedidos/**", "/api/carrinho/**", "/api/pagamentos/**").hasRole("CLIENTE")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(conversor)));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey chaveJwt(@Value("${app.jwt.segredo}") String segredo) {
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

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter perfis = new JwtGrantedAuthoritiesConverter();
        perfis.setAuthoritiesClaimName("roles");
        perfis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(perfis);
        return conversor;
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
