package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.TaxaEntregaRegiao;
import br.com.loudfeliz.dto.TaxaEntregaRequest;
import br.com.loudfeliz.dto.TaxaEntregaResponse;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RegraNegocioException;
import br.com.loudfeliz.repositorio.TaxaEntregaRepositorio;
import br.com.loudfeliz.util.Texto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaServico {

    private final TaxaEntregaRepositorio taxas;

    public EntregaServico(TaxaEntregaRepositorio taxas) {
        this.taxas = taxas;
    }

    @Transactional(readOnly = true)
    public Optional<TaxaEntregaRegiao> buscarRegiao(String bairro, String cep) {
        String alvo = Texto.normalizar(bairro);
        List<TaxaEntregaRegiao> todas = taxas.findAll();
        Optional<TaxaEntregaRegiao> porBairro = todas.stream().filter(t -> Texto.normalizar(t.getBairro()).equals(alvo)).findFirst();
        if (porBairro.isPresent() || cep == null) {
            return porBairro;
        }
        return todas.stream().filter(t -> t.getCepPrefixo() != null && cep.startsWith(t.getCepPrefixo())).findFirst();
    }

    @Transactional(readOnly = true)
    public TaxaEntregaResponse consultar(String bairro) {
        return buscarRegiao(bairro, null).map(TaxaEntregaResponse::de)
                .orElseThrow(() -> new RegraNegocioException("RN09", "Ainda não entregamos no bairro " + bairro + "."));
    }

    @Transactional(readOnly = true)
    public List<TaxaEntregaResponse> listar() {
        return taxas.findAllByOrderByBairroAsc().stream().map(TaxaEntregaResponse::de).toList();
    }

    @Transactional
    public TaxaEntregaResponse criar(TaxaEntregaRequest requisicao) {
        TaxaEntregaRegiao taxa = new TaxaEntregaRegiao();
        aplicar(taxa, requisicao);
        return TaxaEntregaResponse.de(taxas.save(taxa));
    }

    @Transactional
    public TaxaEntregaResponse atualizar(Long id, TaxaEntregaRequest requisicao) {
        TaxaEntregaRegiao taxa = obter(id);
        aplicar(taxa, requisicao);
        return TaxaEntregaResponse.de(taxa);
    }

    @Transactional
    public void remover(Long id) {
        taxas.delete(obter(id));
    }

    private TaxaEntregaRegiao obter(Long id) {
        return taxas.findById(id).orElseThrow(() -> new NaoEncontradoException("Região de entrega não encontrada."));
    }

    private void aplicar(TaxaEntregaRegiao taxa, TaxaEntregaRequest requisicao) {
        taxa.setBairro(requisicao.bairro().trim());
        taxa.setCepPrefixo(requisicao.cepPrefixo());
        taxa.setValor(requisicao.valor());
    }
}
