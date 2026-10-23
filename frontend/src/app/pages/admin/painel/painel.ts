import { CurrencyPipe } from '@angular/common';
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
import { Produto } from '../../../core/models/produto';
import { AdminService, paraCadastro } from '../../../core/services/admin.service';
import { ProdutoFormulario } from './produto-formulario';

const CARGOS: Record<Perfil, string> = {
  DONO: 'Dono',
  CAIXA: 'Caixa',
  ESTOQUE: 'Estoque',
  ENTREGADOR: 'Entregador',
  CLIENTE: 'Cliente'
};

@Component({
  selector: 'app-painel',
  imports: [CurrencyPipe, ReactiveFormsModule, ProdutoFormulario],
  templateUrl: './painel.html',
  styleUrl: './painel.css'
})
export class Painel {
  private readonly admin = inject(AdminService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private timerAviso?: ReturnType<typeof setTimeout>;

  readonly sessao = this.auth.sessao;
  readonly cargo = computed(() => CARGOS[this.sessao()?.perfil ?? 'DONO']);

  readonly aba = signal<'produtos' | 'categorias'>('produtos');
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

  sair(): void {
    this.auth.sair();
    this.router.navigateByUrl('/');
  }

  private substituir(p: Produto): void {
    this.produtos.update(lista => lista.map(x => (x.id === p.id ? p : x)));
  }

  private mostrarAviso(mensagem: string): void {
    this.aviso.set(mensagem);
    clearTimeout(this.timerAviso);
    this.timerAviso = setTimeout(() => this.aviso.set(null), 3500);
  }
}
