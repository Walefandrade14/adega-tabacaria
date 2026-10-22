package br.com.loudfeliz.web;

import br.com.loudfeliz.excecao.ConflitoException;
import br.com.loudfeliz.excecao.CredenciaisInvalidasException;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RegraNegocioException;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class TratadorDeErros {

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

    private ProblemDetail problema(HttpStatusCode status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }
}
