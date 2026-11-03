import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../core/erro-api';
import { COR_STATUS, Pedido, STATUS_PEDIDO } from '../../core/models/pedido';
import { PedidoService } from '../../core/services/pedido.service';

@Component({
  selector: 'app-meus-pedidos',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './meus-pedidos.html',
  styleUrl: './pedidos.css'
})
export class MeusPedidos {
  private readonly pedidos = inject(PedidoService);

  readonly lista = signal<Pedido[]>([]);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly rotulo = STATUS_PEDIDO;
  readonly cor = COR_STATUS;

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.pedidos
      .meus()
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: lista => this.lista.set([...lista].sort((a, b) => b.id - a.id)),
        error: e => this.erro.set(mensagemDeErro(e, 'Não foi possível carregar seus pedidos.'))
      });
  }

  quantidade(pedido: Pedido): number {
    return pedido.itens.reduce((soma, item) => soma + item.quantidade, 0);
  }
}
