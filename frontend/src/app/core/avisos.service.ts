import { Injectable, signal } from '@angular/core';

export interface Aviso {
  texto: string;
  recarregar?: boolean;
}

@Injectable({ providedIn: 'root' })
export class AvisosService {
  readonly atual = signal<Aviso | null>(null);
  private ultimo = { texto: '', em: 0 };
  private timer?: ReturnType<typeof setTimeout>;

  mostrar(aviso: Aviso): void {
    const agora = Date.now();
    if (aviso.texto === this.ultimo.texto && agora - this.ultimo.em < 4000) return;
    this.ultimo = { texto: aviso.texto, em: agora };
    clearTimeout(this.timer);
    this.atual.set(aviso);
    if (!aviso.recarregar) this.timer = setTimeout(() => this.fechar(), 8000);
  }

  fechar(): void {
    clearTimeout(this.timer);
    this.atual.set(null);
  }
}
