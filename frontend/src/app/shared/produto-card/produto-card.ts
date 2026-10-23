import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { imagemDoProduto } from '../../core/imagem-produto';
import { linkWhatsapp } from '../../core/loja';
import { Produto } from '../../core/models/produto';

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

  readonly linkPedido = computed(() => linkWhatsapp(`Olá! Quero pedir: ${this.produto().nome}`));
}
