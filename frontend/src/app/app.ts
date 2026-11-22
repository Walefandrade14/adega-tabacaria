import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, take } from 'rxjs';
import { AuthService } from './core/auth/auth.service';
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
export class App {
  constructor() {
    const auth = inject(AuthService);
    inject(Router)
      .events.pipe(
        filter(evento => evento instanceof NavigationEnd),
        take(1)
      )
      .subscribe(() => auth.verificar());
  }
}
