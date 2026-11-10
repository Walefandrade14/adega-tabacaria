import { Component, inject } from '@angular/core';
import { AvisosService } from '../../core/avisos.service';

@Component({
  selector: 'app-aviso-global',
  template: `
    @if (avisos.atual(); as aviso) {
      <div class="alerta erro aviso-global" role="alert">
        <i class="bi bi-exclamation-octagon" aria-hidden="true"></i>
        <span class="texto">{{ aviso.texto }}</span>
        @if (aviso.recarregar) {
          <button type="button" class="btn btn-outline btn-sm recarregar" (click)="recarregar()">
            <i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Recarregar
          </button>
        }
        <button type="button" class="fechar" aria-label="Fechar aviso" (click)="avisos.fechar()">
          <i class="bi bi-x-lg" aria-hidden="true"></i>
        </button>
      </div>
    }
  `,
  styles: `
    .aviso-global {
      position: fixed;
      top: 16px;
      left: 50%;
      z-index: 100;
      width: min(560px, calc(100% - 32px));
      align-items: center;
      background: #1c0b0c;
      box-shadow: 0 12px 32px rgba(0, 0, 0, 0.55);
      transform: translateX(-50%);
    }
    .texto {
      flex: 1;
      min-width: 0;
    }
    .fechar {
      display: grid;
      place-items: center;
      width: 32px;
      height: 32px;
      flex: none;
      border: 0;
      border-radius: 4px;
      background: transparent;
      color: inherit;
      cursor: pointer;
    }
    .fechar:hover {
      background: rgba(255, 255, 255, 0.08);
    }
    @media (max-width: 480px) {
      .aviso-global {
        flex-wrap: wrap;
      }
      .texto {
        flex: 1 1 calc(100% - 80px);
      }
      .recarregar {
        order: 1;
        margin-left: 30px;
      }
    }
  `
})
export class AvisoGlobal {
  readonly avisos = inject(AvisosService);

  recarregar(): void {
    location.reload();
  }
}
