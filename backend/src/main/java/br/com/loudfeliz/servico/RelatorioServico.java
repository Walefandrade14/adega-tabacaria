package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Fuso;
import br.com.loudfeliz.dominio.Produto;
import br.com.loudfeliz.dominio.StatusPedido;
import br.com.loudfeliz.dominio.TipoEntrega;
import br.com.loudfeliz.dto.RelatorioResponse;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioServico {

    private static final Set<StatusPedido> VENDIDO = EnumSet.of(StatusPedido.PAGO, StatusPedido.EM_SEPARACAO, StatusPedido.EM_ENTREGA, StatusPedido.CONCLUIDO);
    private static final int MAXIMO_DIAS = 92;

    private final EntityManager banco;
    private final ConfiguracaoServico configuracoes;

    public RelatorioServico(EntityManager banco, ConfiguracaoServico configuracoes) {
        this.banco = banco;
        this.configuracoes = configuracoes;
    }

    @Transactional(readOnly = true)
    public RelatorioResponse resumo(LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : Fuso.hoje();
        LocalDate inicio = de != null ? de : fim.minusDays(6);
        if (inicio.isAfter(fim)) {
            throw new RequisicaoInvalidaException("A data inicial precisa ser antes da final.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAXIMO_DIAS) {
            throw new RequisicaoInvalidaException("Escolha um período de até " + MAXIMO_DIAS + " dias.");
        }
        LocalDateTime desde = inicio.atStartOfDay();
        LocalDateTime antes = fim.plusDays(1).atStartOfDay();

        List<Object[]> vendas = banco.createQuery("""
                select p.criadoEm, p.total, p.tipoEntrega from Pedido p
                where p.status in :vendido and p.criadoEm >= :desde and p.criadoEm < :antes""", Object[].class)
                .setParameter("vendido", VENDIDO)
                .setParameter("desde", desde)
                .setParameter("antes", antes)
                .getResultList();

        Map<LocalDate, long[]> pedidosPorDia = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> valorPorDia = new LinkedHashMap<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            pedidosPorDia.put(dia, new long[1]);
            valorPorDia.put(dia, BigDecimal.ZERO);
        }
        Map<TipoEntrega, long[]> pedidosPorCanal = new EnumMap<>(TipoEntrega.class);
        Map<TipoEntrega, BigDecimal> valorPorCanal = new EnumMap<>(TipoEntrega.class);
        for (TipoEntrega tipo : TipoEntrega.values()) {
            pedidosPorCanal.put(tipo, new long[1]);
            valorPorCanal.put(tipo, BigDecimal.ZERO);
        }
        BigDecimal faturamento = BigDecimal.ZERO;
        for (Object[] venda : vendas) {
            LocalDate dia = ((LocalDateTime) venda[0]).toLocalDate();
            BigDecimal total = (BigDecimal) venda[1];
            TipoEntrega tipo = (TipoEntrega) venda[2];
            faturamento = faturamento.add(total);
            pedidosPorDia.get(dia)[0]++;
            valorPorDia.merge(dia, total, BigDecimal::add);
            pedidosPorCanal.get(tipo)[0]++;
            valorPorCanal.merge(tipo, total, BigDecimal::add);
        }
        long quantidade = vendas.size();
        BigDecimal ticketMedio = quantidade == 0 ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);

        long cancelados = banco.createQuery("""
                select count(p) from Pedido p
                where p.status = :cancelado and p.criadoEm >= :desde and p.criadoEm < :antes""", Long.class)
                .setParameter("cancelado", StatusPedido.CANCELADO)
                .setParameter("desde", desde)
                .setParameter("antes", antes)
                .getSingleResult();

        List<RelatorioResponse.ProdutoVendido> maisVendidos = banco.createQuery("""
                select i.produto.id, i.produto.nome, sum(i.quantidade), sum(i.precoUnitario * i.quantidade - i.descontoAplicado)
                from ItemPedido i
                where i.pedido.status in :vendido and i.pedido.criadoEm >= :desde and i.pedido.criadoEm < :antes
                group by i.produto.id, i.produto.nome
                order by sum(i.quantidade) desc, i.produto.nome asc""", Object[].class)
                .setParameter("vendido", VENDIDO)
                .setParameter("desde", desde)
                .setParameter("antes", antes)
                .setMaxResults(5)
                .getResultList()
                .stream()
                .map(l -> new RelatorioResponse.ProdutoVendido((Long) l[0], (String) l[1], ((Number) l[2]).longValue(),
                        ((BigDecimal) l[3]).setScale(2, RoundingMode.HALF_UP)))
                .toList();

        int limite = configuracoes.estoqueBaixo();
        List<RelatorioResponse.EstoqueBaixo> estoqueBaixo = banco.createQuery("""
                select p from Produto p where p.ativo = true and p.estoqueQuantidade <= :limite
                order by p.estoqueQuantidade asc, p.nome asc""", Produto.class)
                .setParameter("limite", limite)
                .setMaxResults(20)
                .getResultList()
                .stream()
                .map(p -> new RelatorioResponse.EstoqueBaixo(p.getId(), p.getNome(), p.getEstoqueQuantidade()))
                .toList();

        List<RelatorioResponse.Dia> dias = new ArrayList<>();
        pedidosPorDia.forEach((dia, total) -> dias.add(new RelatorioResponse.Dia(dia, total[0], valorPorDia.get(dia))));
        List<RelatorioResponse.Canal> canais = new ArrayList<>();
        pedidosPorCanal.forEach((tipo, total) -> canais.add(new RelatorioResponse.Canal(tipo, total[0], valorPorCanal.get(tipo))));

        return new RelatorioResponse(inicio, fim, quantidade, faturamento, ticketMedio, cancelados, canais, dias,
                maisVendidos, limite, estoqueBaixo);
    }
}
