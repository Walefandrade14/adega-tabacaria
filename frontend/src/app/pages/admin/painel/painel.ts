import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, finalize, forkJoin } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { mensagemDeErro } from '../../../core/erro-api';
import { imagemDoProduto } from '../../../core/imagem-produto';
import { Perfil } from '../../../core/models/auth';
import { Categoria } from '../../../core/models/categoria';
import { COR_STATUS, Pedido, STATUS_PEDIDO, StatusPedido, proximoStatus } from '../../../core/models/pedido';
import { Produto } from '../../../core/models/produto';
import { AdminService, paraCadastro } from '../../../core/services/admin.service';
import { PedidoService } from '../../../core/services/pedido.service';
import { ProdutoFormulario } from './produto-formulario';

type FiltroPedidos = 'andamento' | 'todos' | StatusPedido;

const FINALIZADOS: StatusPedido[] = ['CONCLUIDO', 'CANCELADO'];

const CARGOS: Record<Perfil, string> = {
  DONO: 'Dono',
  CAIXA: 'Caixa',
  ESTOQUE: 'Estoque',
  ENTREGADOR: 'Entregador',
  CLIENTE: 'Cliente'
};

@Component({
  selector: 'app-painel',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, ProdutoFormulario],
  templateUrl: './painel.html',
  styleUrl: './painel.css'
})
export class Painel {
  private readonly admin = inject(AdminService);
  private readonly pedidosApi = inject(PedidoService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private timerAviso?: ReturnType<typeof setTimeout>;

  readonly sessao = this.auth.sessao;
  readonly cargo = computed(() => CARGOS[this.sessao()?.perfil ?? 'DONO']);

  readonly aba = signal<'produtos' | 'categorias' | 'pedidos'>('produtos');
  readonly produtos = signal<Produto[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly busca = signal('');
  readonly filtroCategoria = signal<number | null>(null);
  readonly somenteAtivos = signal(false);
  readonly editando = signal<Produto | 'novo' | null>(null);
  readonly ocupado = signal<number | null>(null);

  readonly formNovaCategoria = new FormGroup({
    nome: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(60)] })
  });
  readonly formEdicaoCategoria = new FormGroup({
    nome: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(60)] })
  });
  readonly editandoCategoria = signal<number | null>(null);
  readonly confirmarExclusao = signal<number | null>(null);
  readonly salvandoCategoria = signal(false);
  readonly erroCategoria = signal<string | null>(null);

  readonly filtrados = computed(() => {
    const termo = this.busca().trim().toLowerCase();
    const categoria = this.filtroCategoria();
    return this.produtos()
      .filter(p => !this.somenteAtivos() || p.ativo)
      .filter(p => categoria === null || p.categoria.id === categoria)
      .filter(p => !termo || p.nome.toLowerCase().includes(termo))
      .sort((a, b) => Number(b.ativo) - Number(a.ativo) || a.nome.localeCompare(b.nome, 'pt-BR'));
  });

  readonly resumo = computed(() => {
    const ativos = this.produtos().filter(p => p.ativo);
    return {
      ativos: ativos.length,
      semEstoque: ativos.filter(p => p.estoqueQuantidade <= 0).length,
      estoqueBaixo: ativos.filter(p => p.estoqueQuantidade > 0 && p.estoqueQuantidade <= 5).length,
      maiores: ativos.filter(p => p.restritoMaiorIdade).length
    };
  });

  readonly usoPorCategoria = computed(() => {
    const uso: Record<number, number | undefined> = {};
    for (const p of this.produtos()) uso[p.categoria.id] = (uso[p.categoria.id] ?? 0) + 1;
    return uso;
  });

  readonly pedidos = signal<Pedido[]>([]);
  readonly pedidosCarregados = signal(false);
  readonly carregandoPedidos = signal(false);
  readonly erroPedidos = signal<string | null>(null);
  readonly filtroPedidos = signal<FiltroPedidos>('andamento');
  readonly ocupadoPedido = signal<number | null>(null);
  readonly cancelandoPedido = signal<number | null>(null);
  readonly estorno = signal(100);
  readonly rotuloStatus = STATUS_PEDIDO;
  readonly corStatus = COR_STATUS;
  readonly opcoesStatus = Object.entries(STATUS_PEDIDO) as [StatusPedido, string][];

  readonly pedidosEmAndamento = computed(() => this.pedidos().filter(p => !FINALIZADOS.includes(p.status)).length);

  readonly pedidosFiltrados = computed(() => {
    const filtro = this.filtroPedidos();
    return this.pedidos().filter(p =>
      filtro === 'todos' ? true : filtro === 'andamento' ? !FINALIZADOS.includes(p.status) : p.status === filtro
    );
  });

  readonly produtoEmEdicao = computed(() => {
    const alvo = this.editando();
    return alvo === 'novo' ? null : alvo;
  });

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    forkJoin({ produtos: this.admin.produtos(), categorias: this.admin.categorias() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ produtos, categorias }) => {
          this.produtos.set(produtos);
          this.categorias.set(categorias);
          this.carregando.set(false);
        },
        error: e => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível carregar o cadastro.'));
          this.carregando.set(false);
        }
      });
  }

  imagem(p: Produto): string {
    return imagemDoProduto(p);
  }

  atualizarBusca(evento: Event): void {
    this.busca.set((evento.target as HTMLInputElement).value);
  }

  filtrarCategoria(evento: Event): void {
    const valor = (evento.target as HTMLSelectElement).value;
    this.filtroCategoria.set(valor ? Number(valor) : null);
  }

  ajustarEstoque(p: Produto, ajuste: number): void {
    if (p.estoqueQuantidade + ajuste < 0 || this.ocupado()) return;
    this.ocupado.set(p.id);
    this.admin
      .ajustarEstoque(p.id, ajuste)
      .pipe(finalize(() => this.ocupado.set(null)))
      .subscribe({
        next: atualizado => this.substituir(atualizado),
        error: e => this.erro.set(mensagemDeErro(e))
      });
  }

  alternarAtivo(p: Produto): void {
    if (this.ocupado()) return;
    this.ocupado.set(p.id);
    const pedido: Observable<unknown> = p.ativo
      ? this.admin.desativarProduto(p.id)
      : this.admin.atualizarProduto(p.id, paraCadastro(p, { ativo: true }));
    pedido.pipe(finalize(() => this.ocupado.set(null))).subscribe({
      next: () => {
        this.substituir({ ...p, ativo: !p.ativo });
        this.mostrarAviso(p.ativo ? `"${p.nome}" saiu do catálogo.` : `"${p.nome}" voltou para o catálogo.`);
      },
      error: e => this.erro.set(mensagemDeErro(e))
    });
  }

  aoSalvarProduto(p: Produto): void {
    const existia = this.produtos().some(x => x.id === p.id);
    if (existia) this.substituir(p);
    else this.produtos.update(lista => [...lista, p]);
    this.editando.set(null);
    this.mostrarAviso(existia ? 'Produto atualizado.' : `"${p.nome}" cadastrado.`);
  }

  criarCategoria(): void {
    const nome = this.formNovaCategoria.controls.nome.value.trim();
    if (!nome || this.salvandoCategoria()) return;
    this.salvandoCategoria.set(true);
    this.erroCategoria.set(null);
    this.admin
      .criarCategoria(nome)
      .pipe(finalize(() => this.salvandoCategoria.set(false)))
      .subscribe({
        next: c => {
          this.categorias.update(lista => [...lista, c].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR')));
          this.formNovaCategoria.reset();
          this.mostrarAviso(`Categoria "${c.nome}" criada.`);
        },
        error: e => this.erroCategoria.set(mensagemDeErro(e))
      });
  }

  editarCategoria(c: Categoria): void {
    this.editandoCategoria.set(c.id);
    this.confirmarExclusao.set(null);
    this.formEdicaoCategoria.setValue({ nome: c.nome });
  }

  salvarCategoria(c: Categoria): void {
    const nome = this.formEdicaoCategoria.controls.nome.value.trim();
    if (!nome || this.salvandoCategoria()) return;
    this.salvandoCategoria.set(true);
    this.erroCategoria.set(null);
    this.admin
      .atualizarCategoria(c.id, nome)
      .pipe(finalize(() => this.salvandoCategoria.set(false)))
      .subscribe({
        next: atualizada => {
          this.categorias.update(lista => lista.map(x => (x.id === atualizada.id ? atualizada : x)));
          this.produtos.update(lista => lista.map(p => (p.categoria.id === atualizada.id ? { ...p, categoria: atualizada } : p)));
          this.editandoCategoria.set(null);
        },
        error: e => this.erroCategoria.set(mensagemDeErro(e))
      });
  }

  excluirCategoria(c: Categoria): void {
    if (this.confirmarExclusao() !== c.id) {
      this.confirmarExclusao.set(c.id);
      return;
    }
    this.salvandoCategoria.set(true);
    this.erroCategoria.set(null);
    this.admin
      .removerCategoria(c.id)
      .pipe(
        finalize(() => {
          this.salvandoCategoria.set(false);
          this.confirmarExclusao.set(null);
        })
      )
      .subscribe({
        next: () => {
          this.categorias.update(lista => lista.filter(x => x.id !== c.id));
          this.mostrarAviso(`Categoria "${c.nome}" excluída.`);
        },
        error: e =>
          this.erroCategoria.set(
            e?.status === 409 ? `"${c.nome}" tem produtos cadastrados. Mude os produtos de categoria antes de excluir.` : mensagemDeErro(e)
          )
      });
  }

  abrirPedidos(): void {
    this.aba.set('pedidos');
    if (!this.pedidosCarregados()) this.carregarPedidos();
  }

  carregarPedidos(): void {
    this.carregandoPedidos.set(true);
    this.erroPedidos.set(null);
    this.pedidosApi
      .todos()
      .pipe(finalize(() => this.carregandoPedidos.set(false)))
      .subscribe({
        next: lista => {
          this.pedidos.set(lista);
          this.pedidosCarregados.set(true);
        },
        error: e => this.erroPedidos.set(mensagemDeErro(e, 'Não foi possível carregar os pedidos.'))
      });
  }

  filtrarPedidos(evento: Event): void {
    this.filtroPedidos.set((evento.target as HTMLSelectElement).value as FiltroPedidos);
  }

  proximo(p: Pedido): StatusPedido | null {
    return proximoStatus(p);
  }

  finalizado(p: Pedido): boolean {
    return FINALIZADOS.includes(p.status);
  }

  resumoItens(p: Pedido): string {
    return p.itens.map(item => `${item.quantidade}× ${item.produtoNome}`).join(', ');
  }

  avancarPedido(p: Pedido): void {
    const novo = proximoStatus(p);
    if (!novo || this.ocupadoPedido()) return;
    this.ocupadoPedido.set(p.id);
    this.erroPedidos.set(null);
    this.pedidosApi
      .alterarStatus(p.id, novo)
      .pipe(finalize(() => this.ocupadoPedido.set(null)))
      .subscribe({
        next: atualizado => {
          this.substituirPedido(atualizado);
          this.mostrarAviso(`Pedido #${p.id}: ${STATUS_PEDIDO[atualizado.status]}.`);
        },
        error: e => this.erroPedidos.set(mensagemDeErro(e))
      });
  }

  iniciarCancelamento(p: Pedido): void {
    this.cancelandoPedido.set(p.id);
    this.estorno.set(100);
  }

  atualizarEstorno(evento: Event): void {
    const valor = Number((evento.target as HTMLInputElement).value);
    this.estorno.set(Math.max(0, Math.min(100, Number.isFinite(valor) ? Math.round(valor) : 0)));
  }

  confirmarCancelamentoPedido(p: Pedido): void {
    if (this.ocupadoPedido()) return;
    this.ocupadoPedido.set(p.id);
    this.erroPedidos.set(null);
    this.pedidosApi
      .cancelarComoLoja(p.id, this.estorno())
      .pipe(finalize(() => this.ocupadoPedido.set(null)))
      .subscribe({
        next: atualizado => {
          this.substituirPedido(atualizado);
          this.cancelandoPedido.set(null);
          this.mostrarAviso(`Pedido #${p.id} cancelado com estorno de ${this.estorno()}%.`);
        },
        error: e => this.erroPedidos.set(mensagemDeErro(e))
      });
  }

  sair(): void {
    this.auth.sair();
    this.router.navigateByUrl('/');
  }

  private substituir(p: Produto): void {
    this.produtos.update(lista => lista.map(x => (x.id === p.id ? p : x)));
  }

  private substituirPedido(p: Pedido): void {
    this.pedidos.update(lista => lista.map(x => (x.id === p.id ? p : x)));
  }

  private mostrarAviso(mensagem: string): void {
    this.aviso.set(mensagem);
    clearTimeout(this.timerAviso);
    this.timerAviso = setTimeout(() => this.aviso.set(null), 3500);
  }
}
