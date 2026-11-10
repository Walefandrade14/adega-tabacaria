package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Fuso;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiracaoPedidos {

    private static final Logger log = LoggerFactory.getLogger(ExpiracaoPedidos.class);

    private final PedidoServico pedidos;
    private final ConfiguracaoServico configuracoes;

    public ExpiracaoPedidos(PedidoServico pedidos, ConfiguracaoServico configuracoes) {
        this.pedidos = pedidos;
        this.configuracoes = configuracoes;
    }

    @Scheduled(initialDelayString = "${app.pedidos.expiracao.intervalo-ms:60000}", fixedDelayString = "${app.pedidos.expiracao.intervalo-ms:60000}")
    public void executar() {
        expirar();
    }

    public int expirar() {
        int minutos = configuracoes.minutosExpiracaoPedido();
        if (minutos <= 0) {
            return 0;
        }
        LocalDateTime limite = Fuso.agora().minusMinutes(minutos);
        int expirados = 0;
        for (Long id : pedidos.pedidosParaExpirar(limite)) {
            try {
                if (pedidos.expirar(id, limite)) {
                    expirados++;
                }
            } catch (RuntimeException e) {
                log.warn("Não foi possível expirar o pedido {}", id, e);
            }
        }
        if (expirados > 0) {
            log.info("{} pedido(s) cancelado(s) por falta de pagamento em {} minutos", expirados, minutos);
        }
        return expirados;
    }
}
