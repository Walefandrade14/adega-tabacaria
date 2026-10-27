import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandler, Injectable, inject } from '@angular/core';
import { Aviso, AvisosService } from './avisos.service';
import { mensagemDeErro } from './erro-api';

const SITE_ATUALIZADO = /dynamically imported module|Importing a module script failed|ChunkLoadError|Loading chunk/i;

@Injectable()
export class ErroGlobal implements ErrorHandler {
  private readonly avisos = inject(AvisosService);

  handleError(erro: unknown): void {
    console.error(erro);
    this.avisos.mostrar(avisoPara(desembrulhar(erro)));
  }
}

function avisoPara(erro: unknown): Aviso {
  if (erro instanceof HttpErrorResponse) return { texto: mensagemDeErro(erro) };
  if (SITE_ATUALIZADO.test(textoDe(erro))) {
    return { texto: 'O site foi atualizado. Recarregue a página para continuar.', recarregar: true };
  }
  return { texto: 'Algo deu errado nesta tela. Se continuar, recarregue a página.', recarregar: true };
}

function desembrulhar(erro: unknown): unknown {
  if (erro && typeof erro === 'object' && 'rejection' in erro) return (erro as { rejection: unknown }).rejection;
  return erro;
}

function textoDe(erro: unknown): string {
  return erro instanceof Error ? `${erro.name} ${erro.message}` : String(erro);
}
