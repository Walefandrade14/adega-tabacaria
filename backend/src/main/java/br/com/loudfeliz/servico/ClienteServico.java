package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Cliente;
import br.com.loudfeliz.dominio.Endereco;
import br.com.loudfeliz.dominio.Fuso;
import br.com.loudfeliz.dto.ClienteResponse;
import br.com.loudfeliz.dto.EnderecoRequest;
import br.com.loudfeliz.dto.EnderecoResponse;
import br.com.loudfeliz.dto.PerfilRequest;
import br.com.loudfeliz.dto.SenhaRequest;
import br.com.loudfeliz.dto.TokenResponse;
import br.com.loudfeliz.excecao.ConflitoException;
import br.com.loudfeliz.excecao.NaoEncontradoException;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import br.com.loudfeliz.repositorio.ClienteRepositorio;
import br.com.loudfeliz.repositorio.EnderecoRepositorio;
import br.com.loudfeliz.repositorio.PedidoRepositorio;
import br.com.loudfeliz.util.Cpf;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteServico {

    private final ClienteRepositorio clientes;
    private final EnderecoRepositorio enderecos;
    private final PedidoRepositorio pedidos;
    private final PasswordEncoder encoder;
    private final TokenServico tokens;

    public ClienteServico(ClienteRepositorio clientes, EnderecoRepositorio enderecos, PedidoRepositorio pedidos,
            PasswordEncoder encoder, TokenServico tokens) {
        this.clientes = clientes;
        this.enderecos = enderecos;
        this.pedidos = pedidos;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public ClienteResponse perfil(Long clienteId) {
        return ClienteResponse.de(cliente(clienteId));
    }

    @Transactional
    public ClienteResponse atualizarPerfil(Long clienteId, PerfilRequest requisicao) {
        Cliente cliente = cliente(clienteId);
        cliente.setNome(requisicao.nome().trim());
        String cpf = Cpf.somenteDigitos(requisicao.cpf());
        if (!cpf.isEmpty() && !cpf.equals(cliente.getCpf())) {
            if (cliente.getCpf() != null) {
                throw new RequisicaoInvalidaException("O CPF já cadastrado não pode ser trocado.");
            }
            if (!Cpf.valido(cpf)) {
                throw new RequisicaoInvalidaException("CPF inválido.");
            }
            if (clientes.existsByCpf(cpf)) {
                throw new ConflitoException("Este CPF já está cadastrado em outra conta.");
            }
            cliente.setCpf(cpf);
        }
        String telefone = requisicao.telefone() == null ? null : requisicao.telefone().trim();
        cliente.setTelefone(telefone == null || telefone.isEmpty() ? null : telefone);
        return ClienteResponse.de(cliente);
    }

    @Transactional
    public TokenResponse alterarSenha(Long clienteId, SenhaRequest requisicao) {
        Cliente cliente = cliente(clienteId);
        if (!encoder.matches(requisicao.senhaAtual(), cliente.getSenhaHash())) {
            throw new RequisicaoInvalidaException("A senha atual não confere.");
        }
        if (requisicao.senhaAtual().equals(requisicao.novaSenha())) {
            throw new RequisicaoInvalidaException("A nova senha precisa ser diferente da atual.");
        }
        cliente.setSenhaHash(encoder.encode(requisicao.novaSenha()));
        LocalDateTime agora = Fuso.agora().truncatedTo(ChronoUnit.SECONDS);
        if (agora.equals(cliente.getSenhaAlteradaEm())) {
            agora = agora.plusSeconds(1);
        }
        cliente.setSenhaAlteradaEm(agora);
        return tokens.gerar(cliente.getId(), cliente.getNome(), "cliente", "CLIENTE", agora);
    }

    @Transactional(readOnly = true)
    public List<EnderecoResponse> listarEnderecos(Long clienteId) {
        return ativos(clienteId).stream().map(EnderecoResponse::de).toList();
    }

    @Transactional
    public EnderecoResponse adicionarEndereco(Long clienteId, EnderecoRequest requisicao) {
        Cliente cliente = cliente(clienteId);
        List<Endereco> existentes = ativos(clienteId);
        boolean principal = Boolean.TRUE.equals(requisicao.principal()) || existentes.isEmpty();
        if (principal) {
            existentes.forEach(e -> e.setPrincipal(false));
        }
        Endereco endereco = new Endereco();
        endereco.setCliente(cliente);
        preencher(endereco, requisicao);
        endereco.setPrincipal(principal);
        return EnderecoResponse.de(enderecos.save(endereco));
    }

    @Transactional
    public EnderecoResponse atualizarEndereco(Long clienteId, Long enderecoId, EnderecoRequest requisicao) {
        Endereco atual = endereco(clienteId, enderecoId);
        boolean principal = atual.isPrincipal() || Boolean.TRUE.equals(requisicao.principal());
        if (principal) {
            ativos(clienteId).stream().filter(e -> !e.getId().equals(enderecoId)).forEach(e -> e.setPrincipal(false));
        }
        Endereco alvo = atual;
        if (pedidos.existsByEnderecoEntregaId(enderecoId)) {
            atual.setArquivado(true);
            atual.setPrincipal(false);
            alvo = new Endereco();
            alvo.setCliente(atual.getCliente());
        }
        preencher(alvo, requisicao);
        alvo.setPrincipal(principal);
        return EnderecoResponse.de(enderecos.save(alvo));
    }

    @Transactional
    public void removerEndereco(Long clienteId, Long enderecoId) {
        Endereco endereco = endereco(clienteId, enderecoId);
        boolean eraPrincipal = endereco.isPrincipal();
        if (pedidos.existsByEnderecoEntregaId(enderecoId)) {
            endereco.setArquivado(true);
            endereco.setPrincipal(false);
        } else {
            enderecos.delete(endereco);
        }
        enderecos.flush();
        if (eraPrincipal) {
            ativos(clienteId).stream().findFirst().ifPresent(e -> e.setPrincipal(true));
        }
    }

    private void preencher(Endereco endereco, EnderecoRequest requisicao) {
        endereco.setApelido(requisicao.apelido());
        endereco.setLogradouro(requisicao.logradouro().trim());
        endereco.setNumero(requisicao.numero().trim());
        endereco.setComplemento(requisicao.complemento());
        endereco.setBairro(requisicao.bairro().trim());
        endereco.setCidade(requisicao.cidade().trim());
        endereco.setUf(requisicao.uf().toUpperCase(Locale.ROOT));
        endereco.setCep(requisicao.cep());
    }

    private List<Endereco> ativos(Long clienteId) {
        return enderecos.findByClienteIdAndArquivadoFalseOrderByPrincipalDescIdAsc(clienteId);
    }

    private Endereco endereco(Long clienteId, Long enderecoId) {
        return enderecos.findByIdAndClienteIdAndArquivadoFalse(enderecoId, clienteId)
                .orElseThrow(() -> new NaoEncontradoException("Endereço não encontrado."));
    }

    private Cliente cliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado."));
    }
}
