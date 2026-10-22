import { CurrencyPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { linkWhatsapp } from '../../core/loja';
import { CATEGORIAS_VITRINE, slugCategoria } from '../../core/models/categoria';
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

  readonly imagem = computed(() => {
    const p = this.produto();
    if (p.imagemUrl) return p.imagemUrl;
    const slug = slugCategoria(p.categoria?.nome ?? '');
    return CATEGORIAS_VITRINE.find(c => c.slug === slug)?.imagem ?? 'img/cat-bebidas.jpg';
  });

  readonly linkPedido = computed(() => linkWhatsapp(`Olá! Quero pedir: ${this.produto().nome}`));
}
