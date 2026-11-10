import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-nao-encontrada',
  imports: [RouterLink],
  template: `
    <section class="container nao-encontrada">
      <span class="eyebrow ruled">Erro 404</span>
      <h1 class="gothic">Página não encontrada</h1>
      <p>O endereço <code>{{ caminho }}</code> não existe ou mudou de lugar.</p>
      <div class="acoes">
        <a routerLink="/" class="btn btn-primary">Ir para o início</a>
        <a routerLink="/produtos" class="btn btn-outline">Ver produtos</a>
      </div>
    </section>
  `,
  styles: `
    .nao-encontrada {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding-block: 96px;
      text-align: center;
    }
    h1 {
      margin: 0;
    }
    p {
      margin: 0;
      color: var(--muted);
      overflow-wrap: anywhere;
    }
    code {
      color: var(--text);
    }
    .acoes {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 12px;
      margin-top: 8px;
    }
  `
})
export class NaoEncontrada {
  readonly caminho = inject(Router).url;
}
