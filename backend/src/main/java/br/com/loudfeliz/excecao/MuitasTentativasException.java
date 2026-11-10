package br.com.loudfeliz.excecao;

public class MuitasTentativasException extends RuntimeException {

    private final long segundos;

    public MuitasTentativasException(long segundos) {
        super("Muitas tentativas de login. Tente de novo em " + Math.max(1, (segundos + 59) / 60) + " minuto(s).");
        this.segundos = segundos;
    }

    public long getSegundos() {
        return segundos;
    }
}
