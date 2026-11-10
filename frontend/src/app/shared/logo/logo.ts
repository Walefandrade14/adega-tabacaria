import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-logo',
  imports: [RouterLink],
  template: `
    <a routerLink="/" class="logo" [class.sm]="tamanho() === 'sm'" aria-label="LouD Feliz no Simples — página inicial">
      <span class="top gothic">Adega <span class="amp">&amp;</span> Tabacaria</span>
      <span class="main gothic">LouD Feliz</span>
      <span class="sub">
        <svg viewBox="0 0 44 14" aria-hidden="true"><path d="M43 7H18c-5 0-7-5-11-5-3 0-5 2-5 4.5S4 10 6 10s3-1.5 3-3" /></svg>
        <span class="gothic">no Simples</span>
        <svg class="flip" viewBox="0 0 44 14" aria-hidden="true"><path d="M43 7H18c-5 0-7-5-11-5-3 0-5 2-5 4.5S4 10 6 10s3-1.5 3-3" /></svg>
      </span>
    </a>
  `,
  styles: `
    .logo {
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      line-height: 0.95;
      color: var(--red);
      --s: 1;
    }
    .logo.sm {
      --s: 0.82;
    }
    .top {
      color: var(--text);
      font-size: calc(19px * var(--s));
    }
    .amp {
      color: var(--gold);
      font-size: 0.8em;
    }
    .main {
      font-size: calc(38px * var(--s));
      text-shadow: 0 0 18px rgba(224, 20, 27, 0.35);
    }
    .sub {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: calc(19px * var(--s));
    }
    svg {
      width: calc(26px * var(--s));
      height: auto;
      fill: none;
      stroke: currentColor;
      stroke-width: 1.4;
      stroke-linecap: round;
    }
    .flip {
      transform: scaleX(-1);
    }
  `
})
export class Logo {
  readonly tamanho = input<'sm' | 'md'>('md');
}
