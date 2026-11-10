import { Component, input } from '@angular/core';
import { CategoriaSlug } from '../../core/models/categoria';

@Component({
  selector: 'app-category-icon',
  template: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      @switch (nome()) {
        @case ('bebidas') {
          <path d="M7 2h2.4v3.2L11 7.4V20a1 1 0 0 1-1 1H6.4a1 1 0 0 1-1-1V7.4L7 5.2z" />
          <path d="M5.4 11h5.6" />
          <path d="M13.5 12h6.5l-.7 8.1a1 1 0 0 1-1 .9h-3.1a1 1 0 0 1-1-.9z" />
          <path d="M13.8 15.5h5.9" />
        }
        @case ('tabacaria') {
          <path d="M20.5 3.5C11.5 3.5 5 8.7 5 15.8c0 1.6.4 3 1.1 4.1 1.1.7 2.5 1.1 4.1 1.1 7.1 0 10.3-7.2 10.3-17.5z" />
          <path d="M3.5 20.5 15.5 8.5" />
          <path d="M9.5 14.5h4.2M12 12v-3" />
        }
        @case ('narguile') {
          <path d="M10 2.5h4l-.6 2.2h-2.8z" />
          <path d="M7.5 6.5h9" />
          <path d="M12 4.7v8" />
          <path d="M10.2 12.7h3.6c.2 1.3 2.6 2.4 2.6 5a4.4 4.4 0 0 1-8.8 0c0-2.6 2.4-3.7 2.6-5z" />
          <path d="M13.5 10c3.6 0 6 2.2 6 5.8V21" />
        }
        @case ('acessorios') {
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2.8v2.6M12 18.6v2.6M2.8 12h2.6M18.6 12h2.6M5.5 5.5l1.9 1.9M16.6 16.6l1.9 1.9M5.5 18.5l1.9-1.9M16.6 7.4l1.9-1.9" />
          <circle cx="12" cy="12" r="6.4" />
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      color: var(--red);
    }
    svg {
      width: 100%;
      height: 100%;
      fill: none;
      stroke: currentColor;
      stroke-width: 1.5;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  `
})
export class CategoryIcon {
  readonly nome = input.required<CategoriaSlug>();
}
