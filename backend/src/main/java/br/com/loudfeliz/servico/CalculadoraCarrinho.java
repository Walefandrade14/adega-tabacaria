package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.Combo;
import br.com.loudfeliz.dominio.ComboItem;
import br.com.loudfeliz.dominio.Endereco;
import br.com.loudfeliz.dominio.Fuso;
import br.com.loudfeliz.dominio.Produto;
import br.com.loudfeliz.dominio.TaxaEntregaRegiao;
import br.com.loudfeliz.dominio.TipoEntrega;
import br.com.loudfeliz.dto.ItemRequest;
import br.com.loudfeliz.repositorio.ComboRepositorio;
import br.com.loudfeliz.repositorio.ProdutoRepositorio;
import br.com.loudfeliz.util.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CalculadoraCarrinho {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final ProdutoRepositorio produtos;
    private final ComboRepositorio combos;
    private final OfertaDiariaServico ofertas;
    private final HorarioServico horarios;
    private final EntregaServico entregas;
    private final ConfiguracaoServico configuracoes;

    public CalculadoraCarrinho(ProdutoRepositorio produtos, ComboRepositorio combos, OfertaDiariaServico ofertas,
                               HorarioServico horarios, EntregaServico entregas, ConfiguracaoServico configuracoes) {
        this.produtos = produtos;
        this.combos = combos;
        this.ofertas = ofertas;
        this.horarios = horarios;
        this.entregas = entregas;
        this.configuracoes = configuracoes;
    }

    public static Map<Long, Integer> agrupar(List<ItemRequest> itens) {
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ItemRequest item : itens) {
            quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }
        return quantidades;
    }

    public Calculo calcular(Cliente cliente, Map<Long, Integer> quantidades, TipoEntrega tipo, Endereco endereco, boolean documentoConferido) {
        Calculo calculo = new Calculo();
        LocalDate hoje = Fuso.hoje();
        Map<Long, Produto> porId = new HashMap<>();
        produtos.findAllById(quantidades.keySet()).forEach(p -> porId.put(p.getId(), p));

        boolean temRestrito = false;
        Map<Long, Integer> validos = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entrada : quantidades.entrySet()) {
            Produto produto = porId.get(entrada.getKey());
            int quantidade = entrada.getValue();
            if (produto == null || !produto.isAtivo()) {
                calculo.adicionarProblema("CATALOGO", "O produto " + entrada.getKey() + " não está disponível.");
                continue;
            }
            if (quantidade > produto.getEstoqueQuantidade()) {
                calculo.adicionarProblema("RN02", "Estoque insuficiente para '" + produto.getNome() + "': solicitado "
                        + quantidade + ", disponível " + produto.getEstoqueQuantidade() + ".");
            }
            if (produto.isRestritoMaiorIdade()) {
                temRestrito = true;
            }
            if (produto.isOfertaEspecial() && cliente != null) {
                int limite = configuracoes.limiteOferta(produto);
                int jaComprado = ofertas.compradoNoDia(cliente.getId(), produto.getId(), hoje);
                if (jaComprado + quantidade > limite) {
                    calculo.adicionarProblema("RN06", "'" + produto.getNome() + "' é oferta especial: limite de " + limite
                            + " unidade(s) por CPF por dia. Hoje você já comprou " + jaComprado + ".");
                }
            }
            validos.put(produto.getId(), quantidade);
        }

        Map<Long, BigDecimal> descontos = new HashMap<>();
        Map<Long, Combo> comboDoProduto = new HashMap<>();
        aplicarCombos(validos, porId, descontos, comboDoProduto);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descontoTotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entrada : validos.entrySet()) {
            Produto produto = porId.get(entrada.getKey());
            BigDecimal desconto = descontos.getOrDefault(produto.getId(), BigDecimal.ZERO);
            calculo.adicionarLinha(new Calculo.Linha(produto, entrada.getValue(), produto.getPreco(), desconto, comboDoProduto.get(produto.getId())));
            subtotal = subtotal.add(produto.getPreco().multiply(BigDecimal.valueOf(entrada.getValue())));
            descontoTotal = descontoTotal.add(desconto);
        }

        if (temRestrito) {
            if (cliente != null && cliente.idadeEm(hoje) < 18) {
                calculo.adicionarProblema("RN01", "Bebidas alcoólicas, essências, cigarros e carvão não podem ser vendidos para menores de 18 anos.");
            }
            if (tipo == TipoEntrega.BALCAO) {
                calculo.exigirConferenciaDocumento();
                if (!documentoConferido) {
                    calculo.adicionarProblema("RN01", "Venda com itens para maiores de 18 anos: confira o RG do cliente antes de finalizar.");
                }
            }
        }

        BigDecimal taxa = BigDecimal.ZERO;
        if (tipo == TipoEntrega.DELIVERY) {
            if (!horarios.deliveryDisponivel(Fuso.agora())) {
                calculo.adicionarProblema("RN04", "Delivery indisponível neste horário. Confira o horário de funcionamento da loja.");
            }
            if (endereco == null) {
                calculo.adicionarProblema("ENDERECO", "Cadastre um endereço para pedir delivery.");
            } else {
                Optional<TaxaEntregaRegiao> regiao = entregas.buscarRegiao(endereco.getBairro(), endereco.getCep());
                if (regiao.isPresent()) {
                    taxa = regiao.get().getValor();
                } else {
                    calculo.adicionarProblema("RN09", "Ainda não entregamos no bairro " + endereco.getBairro() + ".");
                }
            }
            BigDecimal minimo = configuracoes.valorMinimoDelivery();
            BigDecimal valorProdutos = subtotal.subtract(descontoTotal);
            if (valorProdutos.compareTo(minimo) < 0) {
                calculo.adicionarProblema("RN05", "O pedido mínimo para delivery é " + Dinheiro.formatar(minimo)
                        + " em produtos (a taxa de entrega não entra na conta). Faltam " + Dinheiro.formatar(minimo.subtract(valorProdutos)) + ".");
            }
        }

        calculo.definirTotais(subtotal, descontoTotal, taxa);
        return calculo;
    }

    private void aplicarCombos(Map<Long, Integer> quantidades, Map<Long, Produto> porId,
                               Map<Long, BigDecimal> descontos, Map<Long, Combo> comboDoProduto) {
        Map<Long, Integer> restante = new HashMap<>(quantidades);
        for (Combo combo : combos.findByAtivoTrueOrderByIdAsc()) {
            List<ComboItem> itens = combo.getItens();
            if (itens.isEmpty()) {
                continue;
            }
            int vezes = Integer.MAX_VALUE;
            for (ComboItem item : itens) {
                vezes = Math.min(vezes, restante.getOrDefault(item.getProduto().getId(), 0) / item.getQuantidade());
            }
            if (vezes <= 0) {
                continue;
            }
            BigDecimal valorCombo = BigDecimal.ZERO;
            for (ComboItem item : itens) {
                valorCombo = valorCombo.add(preco(item, porId).multiply(BigDecimal.valueOf(item.getQuantidade())));
            }
            BigDecimal descontoPorCombo = combo.getDescontoPercentual() != null
                    ? valorCombo.multiply(combo.getDescontoPercentual()).divide(CEM, 2, RoundingMode.HALF_UP)
                    : combo.getDescontoValorFixo().setScale(2, RoundingMode.HALF_UP);
            descontoPorCombo = descontoPorCombo.min(valorCombo);
            BigDecimal descontoDoCombo = descontoPorCombo.multiply(BigDecimal.valueOf(vezes));
            BigDecimal distribuido = BigDecimal.ZERO;
            for (int i = 0; i < itens.size(); i++) {
                ComboItem item = itens.get(i);
                Long produtoId = item.getProduto().getId();
                BigDecimal parte;
                if (i == itens.size() - 1) {
                    parte = descontoDoCombo.subtract(distribuido);
                } else {
                    BigDecimal peso = preco(item, porId).multiply(BigDecimal.valueOf(item.getQuantidade()));
                    parte = descontoDoCombo.multiply(peso).divide(valorCombo, 2, RoundingMode.HALF_UP);
                }
                distribuido = distribuido.add(parte);
                descontos.merge(produtoId, parte, BigDecimal::add);
                comboDoProduto.putIfAbsent(produtoId, combo);
                restante.merge(produtoId, -item.getQuantidade() * vezes, Integer::sum);
            }
        }
    }

    private BigDecimal preco(ComboItem item, Map<Long, Produto> porId) {
        Produto produto = porId.get(item.getProduto().getId());
        return produto != null ? produto.getPreco() : item.getProduto().getPreco();
    }
}
