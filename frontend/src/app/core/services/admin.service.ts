import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Categoria } from '../models/categoria';
import { Configuracao, Funcionario, FuncionarioCadastro, Horario, TaxaEntrega, TaxaEntregaCadastro } from '../models/loja';
import { Produto, ProdutoCadastro } from '../models/produto';
import { Relatorio } from '../models/relatorio';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/admin`;

  relatorio(de: string, ate: string): Observable<Relatorio> {
    return this.http.get<Relatorio>(`${this.api}/relatorios/resumo`, { params: { de, ate } });
  }

  produtos(): Observable<Produto[]> {
    return this.http.get<Produto[]>(`${this.api}/produtos`);
  }

  criarProduto(produto: ProdutoCadastro): Observable<Produto> {
    return this.http.post<Produto>(`${this.api}/produtos`, produto);
  }

  atualizarProduto(id: number, produto: ProdutoCadastro): Observable<Produto> {
    return this.http.put<Produto>(`${this.api}/produtos/${id}`, produto);
  }

  ajustarEstoque(id: number, ajuste: number): Observable<Produto> {
    return this.http.patch<Produto>(`${this.api}/produtos/${id}/estoque`, { ajuste });
  }

  desativarProduto(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/produtos/${id}`);
  }

  categorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.api}/categorias`);
  }

  criarCategoria(nome: string): Observable<Categoria> {
    return this.http.post<Categoria>(`${this.api}/categorias`, { nome });
  }

  atualizarCategoria(id: number, nome: string): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.api}/categorias/${id}`, { nome });
  }

  removerCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/categorias/${id}`);
  }

  funcionarios(): Observable<Funcionario[]> {
    return this.http.get<Funcionario[]>(`${this.api}/funcionarios`);
  }

  criarFuncionario(funcionario: FuncionarioCadastro): Observable<Funcionario> {
    return this.http.post<Funcionario>(`${this.api}/funcionarios`, funcionario);
  }

  alterarAtivoFuncionario(id: number, ativo: boolean): Observable<Funcionario> {
    return this.http.patch<Funcionario>(`${this.api}/funcionarios/${id}/ativo`, null, { params: { ativo } });
  }

  alterarSenhaFuncionario(id: number, senha: string): Observable<Funcionario> {
    return this.http.patch<Funcionario>(`${this.api}/funcionarios/${id}/senha`, { senha });
  }

  horarios(): Observable<Horario[]> {
    return this.http.get<Horario[]>(`${this.api}/horarios`);
  }

  atualizarHorario(diaSemana: number, horaAbertura: string, horaFechamento: string, aceitaDelivery: boolean): Observable<Horario> {
    return this.http.put<Horario>(`${this.api}/horarios/${diaSemana}`, { horaAbertura, horaFechamento, aceitaDelivery });
  }

  taxasEntrega(): Observable<TaxaEntrega[]> {
    return this.http.get<TaxaEntrega[]>(`${this.api}/taxas-entrega`);
  }

  criarTaxaEntrega(taxa: TaxaEntregaCadastro): Observable<TaxaEntrega> {
    return this.http.post<TaxaEntrega>(`${this.api}/taxas-entrega`, taxa);
  }

  atualizarTaxaEntrega(id: number, taxa: TaxaEntregaCadastro): Observable<TaxaEntrega> {
    return this.http.put<TaxaEntrega>(`${this.api}/taxas-entrega/${id}`, taxa);
  }

  removerTaxaEntrega(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/taxas-entrega/${id}`);
  }

  configuracoes(): Observable<Configuracao[]> {
    return this.http.get<Configuracao[]>(`${this.api}/configuracoes`);
  }

  atualizarConfiguracao(chave: string, valor: string): Observable<Configuracao> {
    return this.http.put<Configuracao>(`${this.api}/configuracoes/${chave}`, { valor });
  }
}

export function paraCadastro(p: Produto, mudancas: Partial<ProdutoCadastro> = {}): ProdutoCadastro {
  return {
    nome: p.nome,
    descricao: p.descricao ?? null,
    preco: p.preco,
    estoqueQuantidade: p.estoqueQuantidade,
    categoriaId: p.categoria.id,
    fornecedorId: p.fornecedorId ?? null,
    restritoMaiorIdade: p.restritoMaiorIdade,
    ofertaEspecial: p.ofertaEspecial,
    limiteDiarioOferta: p.limiteDiarioOferta ?? null,
    ativo: p.ativo,
    imagemUrl: p.imagemUrl ?? null,
    ...mudancas
  };
}
