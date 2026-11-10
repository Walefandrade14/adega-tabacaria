import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, finalize, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { mensagemDeErro } from '../../core/erro-api';
import { imagemDoProduto } from '../../core/imagem-produto';
import { Endereco } from '../../core/models/conta';
import { Calculo, CarrinhoRequisicao, ItemCalculado, TipoEntrega } from '../../core/models/pedido';
import { Produto } from '../../core/models/produto';
import { CarrinhoService, ItemCarrinho } from '../../core/services/carrinho.service';
import { ContaService } from '../../core/services/conta.service';
import { PedidoService } from '../../core/services/pedido.service';

@Component({
  selector: 'app-carrinho',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './carrinho.html',
  styleUrl: './carrinho.css'
})
export class Carrinho {
  readonly carrinho = inject(CarrinhoService);
  readonly auth = inject(AuthService);
  private readonly conta = inject(ContaService);
  private readonly pedidos = inject(PedidoService);
  private readonly router = inject(Router);

  readonly tipoEntrega = signal<TipoEntrega>('DELIVERY');
  readonly enderecos = signal<Endereco[]>([]);
  readonly enderecoId = signal<number | null>(null);
  readonly carregandoEnderecos = signal(false);
  readonly calculo = signal<Calculo | null>(null);
  readonly simulando = signal(false);
  readonly finalizando = signal(false);
  readonly erro = signal<string | null>(null);

  readonly requisicao = computed<CarrinhoRequisicao | null>(() => {
    const itens = this.carrinho.paraRequisicao();
    if (!this.auth.cliente() || !itens.length) return null;
    const tipoEntrega = this.tipoEntrega();
    const enderecoId = tipoEntrega === 'DELIVERY' ? this.enderecoId() : null;
    if (tipoEntrega === 'DELIVERY' && enderecoId === null) return null;
    return { tipoEntrega, enderecoId, itens };
  });

  readonly podeFinalizar = computed(
    () => !!this.requisicao() && !!this.calculo()?.podeFinalizar && !this.simulando() && !this.finalizando()
  );

  constructor() {
    if (this.auth.cliente()) this.carregarEnderecos();
    toObservable(this.requisicao)
      .pipe(
        tap(() => this.erro.set(null)),
        debounceTime(250),
        switchMap(requisicao => {
          if (!requisicao) return of(null);
          this.simulando.set(true);
          return this.pedidos.simular(requisicao).pipe(
            catchError(e => {
              this.erro.set(mensagemDeErro(e, 'Não foi possível calcular o pedido agora.'));
              return of(null);
            }),
            finalize(() => this.simulando.set(false))
          );
        }),
        takeUntilDestroyed()
      )
      .subscribe(calculo => this.calculo.set(calculo));
  }

  imagem(produto: Produto): string {
    return imagemDoProduto(produto);
  }

  linha(produtoId: number): ItemCalculado | undefined {
    return this.calculo()?.itens.find(item => item.produtoId === produtoId);
  }

  alterar(item: ItemCarrinho, ajuste: number): void {
    this.carrinho.alterar(item.produto.id, item.quantidade + ajuste);
  }

  escolherEndereco(evento: Event): void {
    this.enderecoId.set(Number((evento.target as HTMLSelectElement).value));
  }

  finalizar(): void {
    const requisicao = this.requisicao();
    if (!requisicao || !this.podeFinalizar()) return;
    this.finalizando.set(true);
    this.erro.set(null);
    this.pedidos
      .criar(requisicao)
      .pipe(finalize(() => this.finalizando.set(false)))
      .subscribe({
        next: pedido => {
          this.carrinho.limpar();
          this.router.navigate(['/pedidos', pedido.id]);
        },
        error: e => this.erro.set(mensagemDeErro(e))
      });
  }

  private carregarEnderecos(): void {
    this.carregandoEnderecos.set(true);
    this.conta
      .enderecos()
      .pipe(finalize(() => this.carregandoEnderecos.set(false)))
      .subscribe({
        next: lista => {
          this.enderecos.set(lista);
          const principal = lista.find(e => e.principal) ?? lista[0];
          this.enderecoId.set(principal?.id ?? null);
          if (!lista.length) this.tipoEntrega.set('RETIRADA');
        },
        error: e => this.erro.set(mensagemDeErro(e, 'Não foi possível carregar seus endereços.'))
      });
  }
}
