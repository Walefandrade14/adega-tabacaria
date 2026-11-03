import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { imagemDoProduto } from '../../core/imagem-produto';
import { Produto } from '../../core/models/produto';
import { CarrinhoService } from '../../core/services/carrinho.service';

@Component({
  selector: 'app-produto-card',
  imports: [CurrencyPipe],
  templateUrl: './produto-card.html',
  styleUrl: './produto-card.css'
})
export class ProdutoCard {
  readonly produto = input.required<Produto>();

  readonly esgotado = computed(() => this.produto().estoqueQuantidade <= 0);

  readonly imagem = computed(() => imagemDoProduto(this.produto()));

  private readonly carrinho = inject(CarrinhoService);
  private timer?: ReturnType<typeof setTimeout>;

  readonly adicionado = signal(false);
  readonly noCarrinho = computed(() => this.carrinho.quantidadeDe(this.produto().id));
  readonly limiteAtingido = computed(() => this.noCarrinho() >= this.produto().estoqueQuantidade);

  adicionar(): void {
    if (this.limiteAtingido()) return;
    this.carrinho.adicionar(this.produto());
    this.adicionado.set(true);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.adicionado.set(false), 1600);
  }
}
