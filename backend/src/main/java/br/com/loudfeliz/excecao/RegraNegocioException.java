package br.com.loudfeliz.excecao;

public class RegraNegocioException extends RuntimeException {

    private final String regra;

    public RegraNegocioException(String regra, String mensagem) {
        super(mensagem);
        this.regra = regra;
    }

    public String getRegra() {
        return regra;
    }
}
