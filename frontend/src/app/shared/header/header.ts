import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Logo } from '../logo/logo';

@Component({
  selector: 'app-header',
  imports: [RouterLink, RouterLinkActive, Logo],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class Header {
  readonly menuAberto = signal(false);

  readonly links = [
    { rota: '/', rotulo: 'Início', exato: true },
    { rota: '/produtos', rotulo: 'Produtos', exato: false },
    { rota: '/sobre', rotulo: 'Sobre', exato: false },
    { rota: '/contato', rotulo: 'Contato', exato: false }
  ];

  alternarMenu(): void {
    this.menuAberto.update(v => !v);
  }

  fecharMenu(): void {
    this.menuAberto.set(false);
  }
}
