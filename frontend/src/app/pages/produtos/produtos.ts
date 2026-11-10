import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import { CATEGORIAS_VITRINE, slugCategoria } from '../../core/models/categoria';
import { OrigemDados, Produto } from '../../core/models/produto';
import { ProdutoService } from '../../core/services/produto.service';
import { ProdutoCard } from '../../shared/produto-card/produto-card';

@Component({
  selector: 'app-produtos',
  imports: [RouterLink, ProdutoCard],
  templateUrl: './produtos.html',
  styleUrl: './produtos.css'
})
export class Produtos {
  private readonly service = inject(ProdutoService);
  private readonly destroyRef = inject(DestroyRef);

  readonly categoria = input<string>();

  readonly categorias = CATEGORIAS_VITRINE;
  readonly apiUrl = environment.apiUrl;

  readonly produtos = signal<Produto[]>([]);
  readonly origem = signal<OrigemDados>('api');
  readonly carregando = signal(true);
  readonly erro = signal(false);
  readonly busca = signal('');
  readonly acordando = signal(false);

  readonly selecionada = computed(() => this.categoria() ?? 'todas');

  readonly filtrados = computed(() => {
    const cat = this.selecionada();
    const termo = this.busca().trim().toLowerCase();
    return this.produtos().filter(p => {
      const okCategoria = cat === 'todas' || slugCategoria(p.categoria?.nome ?? '') === cat;
      const okBusca = !termo || p.nome.toLowerCase().includes(termo) || (p.descricao ?? '').toLowerCase().includes(termo);
      return okCategoria && okBusca;
    });
  });

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(false);
    this.acordando.set(false);
    const aviso = setTimeout(() => this.acordando.set(true), 4000);
    this.destroyRef.onDestroy(() => clearTimeout(aviso));
    this.service
      .listar()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: r => {
          clearTimeout(aviso);
          this.produtos.set(r.produtos);
          this.origem.set(r.origem);
          this.carregando.set(false);
        },
        error: () => {
          clearTimeout(aviso);
          this.erro.set(true);
          this.carregando.set(false);
        }
      });
  }

  atualizarBusca(evento: Event): void {
    this.busca.set((evento.target as HTMLInputElement).value);
  }
}
