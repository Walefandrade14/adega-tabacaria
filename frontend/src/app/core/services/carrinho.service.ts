import { Injectable, computed, effect, signal } from '@angular/core';
import { ItemRequisicao } from '../models/pedido';
import { Produto } from '../models/produto';

const CHAVE = 'loudfeliz.carrinho';

export interface ItemCarrinho {
  produto: Produto;
  quantidade: number;
}

@Injectable({ providedIn: 'root' })
export class CarrinhoService {
  readonly itens = signal<ItemCarrinho[]>(lerCarrinho());
  readonly quantidadeTotal = computed(() => this.itens().reduce((soma, item) => soma + item.quantidade, 0));
  readonly valorProdutos = computed(() => this.itens().reduce((soma, item) => soma + item.produto.preco * item.quantidade, 0));
  readonly paraRequisicao = computed<ItemRequisicao[]>(() =>
    this.itens().map(item => ({ produtoId: item.produto.id, quantidade: item.quantidade }))
  );

  constructor() {
    effect(() => {
      const itens = this.itens();
      try {
        localStorage.setItem(CHAVE, JSON.stringify(itens));
      } catch {}
    });
  }

  quantidadeDe(produtoId: number): number {
    return this.itens().find(item => item.produto.id === produtoId)?.quantidade ?? 0;
  }

  adicionar(produto: Produto, quantidade = 1): void {
    if (produto.estoqueQuantidade <= 0) return;
    this.itens.update(lista => {
      const existente = lista.find(item => item.produto.id === produto.id);
      if (!existente) return [...lista, { produto, quantidade: limitar(produto, quantidade) }];
      return lista.map(item =>
        item.produto.id === produto.id ? { produto, quantidade: limitar(produto, item.quantidade + quantidade) } : item
      );
    });
  }

  alterar(produtoId: number, quantidade: number): void {
    if (quantidade < 1) {
      this.remover(produtoId);
      return;
    }
    this.itens.update(lista =>
      lista.map(item => (item.produto.id === produtoId ? { ...item, quantidade: limitar(item.produto, quantidade) } : item))
    );
  }

  remover(produtoId: number): void {
    this.itens.update(lista => lista.filter(item => item.produto.id !== produtoId));
  }

  limpar(): void {
    this.itens.set([]);
  }
}

function limitar(produto: Produto, quantidade: number): number {
  return Math.max(1, Math.min(quantidade, produto.estoqueQuantidade, 999));
}

function lerCarrinho(): ItemCarrinho[] {
  try {
    const bruto = localStorage.getItem(CHAVE);
    if (!bruto) return [];
    const itens = JSON.parse(bruto) as ItemCarrinho[];
    return Array.isArray(itens) ? itens.filter(item => item?.produto?.id && item.quantidade > 0) : [];
  } catch {
    return [];
  }
}
