package br.com.loudfeliz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.loudfeliz.web.TratadorDeErros;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.server.ResponseStatusException;

class TratadorDeErrosTest {

    private final TratadorDeErros tratador = new TratadorDeErros();

    @Test
    void erroInesperadoViraQuinhentosComCodigoESemVazarDetalhes() {
        ResponseEntity<ProblemDetail> resposta = tratador.inesperado(
                new IllegalStateException("senha do banco: segredo"), new MockHttpServletRequest("GET", "/api/produtos"));
        assertEquals(500, resposta.getStatusCode().value());
        ProblemDetail corpo = resposta.getBody();
        assertNotNull(corpo);
        assertNotNull(corpo.getProperties());
        String codigo = (String) corpo.getProperties().get("codigo");
        assertEquals(8, codigo.length());
        assertEquals("Erro no servidor", corpo.getTitle());
        assertTrue(corpo.getDetail().contains(codigo));
        assertFalse(corpo.getDetail().contains("segredo"));
    }

    @Test
    void erroDoSpringMantemOStatusOriginalEmPortugues() {
        ResponseEntity<ProblemDetail> resposta = tratador.inesperado(
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET")), new MockHttpServletRequest("DELETE", "/api/produtos"));
        assertEquals(405, resposta.getStatusCode().value());
        assertEquals("Método não permitido", resposta.getBody().getTitle());
        assertEquals("O método DELETE não é aceito em /api/produtos.", resposta.getBody().getDetail());
        assertTrue(resposta.getHeaders().getAllow().contains(HttpMethod.GET));
    }

    @Test
    void parametroObrigatorioAusente() {
        ResponseEntity<ProblemDetail> resposta = tratador.inesperado(
                new MissingServletRequestParameterException("bairro", "String"), new MockHttpServletRequest("GET", "/api/entrega/taxa"));
        assertEquals(400, resposta.getStatusCode().value());
        assertEquals("Parâmetro obrigatório 'bairro' não informado.", resposta.getBody().getDetail());
    }

    @Test
    void erroCincoXXDoSpringGanhaCodigoDeRastreio() {
        ResponseEntity<ProblemDetail> resposta = tratador.inesperado(
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE), new MockHttpServletRequest("GET", "/api/loja/status"));
        assertEquals(503, resposta.getStatusCode().value());
        assertNotNull(resposta.getBody().getProperties().get("codigo"));
    }
}
