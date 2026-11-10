import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AvisoGlobal } from './shared/aviso-global/aviso-global';
import { Footer } from './shared/footer/footer';
import { Header } from './shared/header/header';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Header, Footer, AvisoGlobal],
  template: `
    <app-header />
    <main>
      <router-outlet />
    </main>
    <app-footer />
    <app-aviso-global />
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    main {
      flex: 1;
    }
  `
})
export class App {}
