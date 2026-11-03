import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../core/erro-api';
import {
  COR_STATUS,
  FORMAS_PAGAMENTO,
  FormaPagamento,
  Pedido,
  STATUS_PAGAMENTO,
  STATUS_PEDIDO,
  clientePodeCancelar
} from '../../core/models/pedido';
import { PedidoService } from '../../core/services/pedido.service';

interface Mensagem {
  tipo: 'ok' | 'erro';
  texto: string;
}

interface Etapa {
  rotulo: string;
  feita: boolean;
  atual: boolean;
}

const ORDEM = ['ABERTO', 'PAGO', 'EM_SEPARACAO', 'EM_ENTREGA', 'CONCLUIDO'];

@Component({
  selector: 'app-pedido-detalhe',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './pedido-detalhe.html',
  styleUrl: './pedidos.css'
})
export class PedidoDetalhe implements OnInit {
  private readonly pedidos = inject(PedidoService);

  readonly id = input.required<string>();

  readonly pedido = signal<Pedido | null>(null);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly mensagem = signal<Mensagem | null>(null);
  readonly forma = signal<FormaPagamento>('CARTAO_CREDITO');
  readonly simularRecusa = signal(false);
  readonly pagando = signal(false);
  readonly confirmandoPix = signal(false);
  readonly confirmarCancelamento = signal(false);
  readonly cancelando = signal(false);
  readonly copiado = signal(false);

  readonly formas = Object.entries(FORMAS_PAGAMENTO) as [FormaPagamento, string][];
  readonly rotulo = STATUS_PEDIDO;
  readonly rotuloPagamento = STATUS_PAGAMENTO;
  readonly rotuloForma = FORMAS_PAGAMENTO;
  readonly cor = COR_STATUS;
  readonly icones: Record<FormaPagamento, string> = {
    CARTAO_CREDITO: 'bi-credit-card',
    CARTAO_DEBITO: 'bi-credit-card-2-front',
    PIX: 'bi-qr-code',
    DINHEIRO: 'bi-cash-coin'
  };

  readonly pixPendente = computed(() => this.pedido()?.pagamentos.find(p => p.forma === 'PIX' && p.status === 'PENDENTE') ?? null);
  readonly podePagar = computed(() => this.pedido()?.status === 'ABERTO');
  readonly podeCancelar = computed(() => {
    const p = this.pedido();
    return !!p && clientePodeCancelar(p);
  });
  readonly cartao = computed(() => this.forma() === 'CARTAO_CREDITO' || this.forma() === 'CARTAO_DEBITO');
  readonly quantidade = computed(() => this.pedido()?.itens.reduce((soma, item) => soma + item.quantidade, 0) ?? 0);

  readonly etapas = computed<Etapa[]>(() => {
    const p = this.pedido();
    if (!p || p.status === 'CANCELADO') return [];
    const nomes: [string, string][] = [
      ['ABERTO', 'Pedido feito'],
      ['PAGO', 'Pago'],
      ['EM_SEPARACAO', 'Em separação'],
      ['EM_ENTREGA', p.tipoEntrega === 'DELIVERY' ? 'Saiu para entrega' : ''],
      ['CONCLUIDO', p.tipoEntrega === 'DELIVERY' ? 'Entregue' : 'Retirado']
    ];
    const atual = ORDEM.indexOf(p.status === 'AGUARDANDO_PAGAMENTO' ? 'ABERTO' : p.status);
    return nomes
      .map(([status, rotulo]) => ({ indice: ORDEM.indexOf(status), rotulo }))
      .filter(etapa => etapa.rotulo)
      .map(etapa => ({ rotulo: etapa.rotulo, feita: etapa.indice <= atual, atual: etapa.indice === atual }));
  });

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.pedidos
      .pedido(Number(this.id()))
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: pedido => this.pedido.set(pedido),
        error: e => this.erro.set(mensagemDeErro(e, 'Não foi possível carregar o pedido.'))
      });
  }

  pagar(): void {
    const p = this.pedido();
    if (!p || this.pagando()) return;
    this.pagando.set(true);
    this.mensagem.set(null);
    this.pedidos
      .pagar(p.id, this.forma(), this.cartao() && this.simularRecusa())
      .pipe(finalize(() => this.pagando.set(false)))
      .subscribe({
        next: pagamento => {
          if (pagamento.status === 'RECUSADO') {
            this.mensagem.set({ tipo: 'erro', texto: 'Pagamento recusado pela operadora. O pedido continua aberto: tente outra forma.' });
          } else if (pagamento.status === 'APROVADO') {
            this.mensagem.set({ tipo: 'ok', texto: 'Pagamento aprovado! Agora é com a loja.' });
          }
          this.carregar();
        },
        error: e => this.mensagem.set({ tipo: 'erro', texto: mensagemDeErro(e) })
      });
  }

  confirmarPix(): void {
    const pix = this.pixPendente();
    if (!pix || this.confirmandoPix()) return;
    this.confirmandoPix.set(true);
    this.mensagem.set(null);
    this.pedidos
      .confirmarPix(pix.id)
      .pipe(finalize(() => this.confirmandoPix.set(false)))
      .subscribe({
        next: () => {
          this.mensagem.set({ tipo: 'ok', texto: 'Pix confirmado! Agora é com a loja.' });
          this.carregar();
        },
        error: e => this.mensagem.set({ tipo: 'erro', texto: mensagemDeErro(e) })
      });
  }

  copiarPix(codigo: string): void {
    navigator.clipboard
      ?.writeText(codigo)
      .then(() => {
        this.copiado.set(true);
        setTimeout(() => this.copiado.set(false), 2000);
      })
      .catch(() => {});
  }

  cancelar(): void {
    const p = this.pedido();
    if (!p || this.cancelando()) return;
    if (!this.confirmarCancelamento()) {
      this.confirmarCancelamento.set(true);
      return;
    }
    this.cancelando.set(true);
    this.mensagem.set(null);
    this.pedidos
      .cancelar(p.id)
      .pipe(
        finalize(() => {
          this.cancelando.set(false);
          this.confirmarCancelamento.set(false);
        })
      )
      .subscribe({
        next: atualizado => {
          this.pedido.set(atualizado);
          this.mensagem.set({ tipo: 'ok', texto: 'Pedido cancelado.' });
        },
        error: e => this.mensagem.set({ tipo: 'erro', texto: mensagemDeErro(e) })
      });
  }
}
