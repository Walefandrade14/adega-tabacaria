package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.Endereco;
import br.com.loudfeliz.dto.ClienteResponse;
import br.com.loudfeliz.dto.EnderecoRequest;
import br.com.loudfeliz.dto.EnderecoResponse;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.repositorio.ClienteRepositorio;
import br.com.loudfeliz.repositorio.EnderecoRepositorio;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServico {

    private final ClienteRepositorio clientes;
    private final EnderecoRepositorio enderecos;

    public ClienteServico(ClienteRepositorio clientes, EnderecoRepositorio enderecos) {
        this.clientes = clientes;
        this.enderecos = enderecos;
    }

    @Transactional(readOnly = true)
    public ClienteResponse perfil(Long clienteId) {
        return ClienteResponse.de(cliente(clienteId));
    }

    @Transactional(readOnly = true)
    public List<EnderecoResponse> listarEnderecos(Long clienteId) {
        return enderecos.findByClienteIdOrderByPrincipalDescIdAsc(clienteId).stream().map(EnderecoResponse::de).toList();
    }

    @Transactional
    public EnderecoResponse adicionarEndereco(Long clienteId, EnderecoRequest requisicao) {
        Cliente cliente = cliente(clienteId);
        List<Endereco> existentes = enderecos.findByClienteIdOrderByPrincipalDescIdAsc(clienteId);
        boolean principal = Boolean.TRUE.equals(requisicao.principal()) || existentes.isEmpty();
        if (principal) {
            existentes.forEach(e -> e.setPrincipal(false));
        }
        Endereco endereco = new Endereco();
        endereco.setCliente(cliente);
        endereco.setApelido(requisicao.apelido());
        endereco.setLogradouro(requisicao.logradouro().trim());
        endereco.setNumero(requisicao.numero().trim());
        endereco.setComplemento(requisicao.complemento());
        endereco.setBairro(requisicao.bairro().trim());
        endereco.setCidade(requisicao.cidade().trim());
        endereco.setUf(requisicao.uf().toUpperCase(Locale.ROOT));
        endereco.setCep(requisicao.cep());
        endereco.setPrincipal(principal);
        return EnderecoResponse.de(enderecos.save(endereco));
    }

    @Transactional
    public void removerEndereco(Long clienteId, Long enderecoId) {
        Endereco endereco = enderecos.findByIdAndClienteId(enderecoId, clienteId)
                .orElseThrow(() -> new NaoEncontradoException("Endereço não encontrado."));
        enderecos.delete(endereco);
        enderecos.flush();
    }

    private Cliente cliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado."));
    }
}
