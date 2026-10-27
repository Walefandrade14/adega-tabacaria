package br.com.loudfeliz.web;

import br.com.loudfeliz.excecao.ConflitoException;
import br.com.loudfeliz.excecao.CredenciaisInvalidasException;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RegraNegocioException;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraNegocioException e) {
        ProblemDetail problema = problema(HttpStatusCode.valueOf(422), "Regra de negócio", e.getMessage());
        problema.setProperty("regra", e.getRegra());
        return problema;
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ProblemDetail naoEncontrado(NaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail conflito(ConflitoException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ProblemDetail requisicaoInvalida(RequisicaoInvalidaException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciais(CredenciaisInvalidasException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não autorizado", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos", "Confira os campos destacados.");
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail corpoInvalido(HttpMessageNotReadableException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "O corpo da requisição não é um JSON válido para este endpoint.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tipoInvalido(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "Valor inválido para o parâmetro '" + e.getName() + "'.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridade(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "Conflito", "O registro está em uso ou já existe.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail naoAutenticado(AuthenticationException e) {
        String detalhe = e instanceof OAuth2AuthenticationException
                ? "Sessão expirada ou inválida. Entre de novo."
                : "Faça login para acessar este recurso.";
        return problema(HttpStatus.UNAUTHORIZED, "Não autorizado", detalhe);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegado(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não tem acesso a este recurso.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> inesperado(Exception e, HttpServletRequest requisicao) {
        if (e instanceof ErrorResponse resposta && resposta.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = resposta.getStatusCode();
            ProblemDetail problema = problema(status, titulo(status), detalhe(e, resposta, requisicao));
            return ResponseEntity.status(status).headers(resposta.getHeaders()).body(problema);
        }
        HttpStatusCode status = e instanceof ErrorResponse resposta ? resposta.getStatusCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        String codigo = UUID.randomUUID().toString().substring(0, 8);
        log.error("Erro inesperado [{}] em {} {}", codigo, requisicao.getMethod(), requisicao.getRequestURI(), e);
        ProblemDetail problema = problema(status, "Erro no servidor",
                "Erro inesperado no servidor. Se continuar, informe o código " + codigo + ".");
        problema.setProperty("codigo", codigo);
        return ResponseEntity.status(status).body(problema);
    }

    private static String titulo(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "Não encontrado";
            case 405 -> "Método não permitido";
            case 406 -> "Formato não suportado";
            case 413 -> "Requisição grande demais";
            case 415 -> "Tipo de conteúdo não suportado";
            default -> "Requisição inválida";
        };
    }

    private static String detalhe(Exception e, ErrorResponse resposta, HttpServletRequest requisicao) {
        if (e instanceof MissingServletRequestParameterException ausente) {
            return "Parâmetro obrigatório '" + ausente.getParameterName() + "' não informado.";
        }
        return switch (resposta.getStatusCode().value()) {
            case 404 -> "Nada encontrado em " + requisicao.getRequestURI() + ".";
            case 405 -> "O método " + requisicao.getMethod() + " não é aceito em " + requisicao.getRequestURI() + ".";
            case 406 -> "A API responde só em JSON.";
            case 413 -> "O corpo da requisição passou do tamanho permitido.";
            case 415 -> "Envie o corpo da requisição como application/json.";
            default -> "A requisição não pôde ser processada.";
        };
    }

    private static ProblemDetail problema(HttpStatusCode status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
