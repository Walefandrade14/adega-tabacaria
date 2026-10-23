import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Categoria } from '../models/categoria';
import { Produto, ProdutoCadastro } from '../models/produto';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/admin`;

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
