package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Configuracao;
import br.com.loudfeliz.dominio.Produto;
import br.com.loudfeliz.dto.ConfiguracaoResponse;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import br.com.loudfeliz.repositorio.ConfiguracaoRepositorio;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracaoServico {

    public static final String VALOR_MINIMO_DELIVERY = "valor_minimo_delivery";
    public static final String LIMITE_PADRAO_OFERTA = "limite_padrao_oferta_dia";

    private final ConfiguracaoRepositorio configuracoes;

    public ConfiguracaoServico(ConfiguracaoRepositorio configuracoes) {
        this.configuracoes = configuracoes;
    }

    @Transactional(readOnly = true)
    public BigDecimal valorMinimoDelivery() {
        return configuracoes.findById(VALOR_MINIMO_DELIVERY).map(c -> new BigDecimal(c.getValor())).orElse(new BigDecimal("35.00"));
    }

    @Transactional(readOnly = true)
    public int limitePadraoOferta() {
        return configuracoes.findById(LIMITE_PADRAO_OFERTA).map(c -> Integer.parseInt(c.getValor())).orElse(3);
    }

    public int limiteOferta(Produto produto) {
        return produto.getLimiteDiarioOferta() != null ? produto.getLimiteDiarioOferta() : limitePadraoOferta();
    }

    @Transactional(readOnly = true)
    public List<ConfiguracaoResponse> listar() {
        return configuracoes.findAllByOrderByChaveAsc().stream().map(ConfiguracaoResponse::de).toList();
    }

    @Transactional
    public ConfiguracaoResponse atualizar(String chave, String valor) {
        Configuracao configuracao = configuracoes.findById(chave)
                .orElseThrow(() -> new NaoEncontradoException("Configuração não encontrada: " + chave));
        String valorLimpo = valor.trim();
        try {
            if (VALOR_MINIMO_DELIVERY.equals(chave) && new BigDecimal(valorLimpo).signum() < 0) {
                throw new RequisicaoInvalidaException("O valor mínimo não pode ser negativo.");
            }
            if (LIMITE_PADRAO_OFERTA.equals(chave) && Integer.parseInt(valorLimpo) < 1) {
                throw new RequisicaoInvalidaException("O limite precisa ser de pelo menos 1 unidade.");
            }
        } catch (NumberFormatException e) {
            throw new RequisicaoInvalidaException("Valor numérico inválido para " + chave + ".");
        }
        configuracao.setValor(valorLimpo);
        return ConfiguracaoResponse.de(configuracao);
    }
}
