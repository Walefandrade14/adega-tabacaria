import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, throwError, timeout } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PRODUTOS_EXEMPLO } from '../data/produtos-exemplo';
import { Produto, ResultadoProdutos } from '../models/produto';

@Injectable({ providedIn: 'root' })
export class ProdutoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/produtos`;

  listar(): Observable<ResultadoProdutos> {
    return this.http.get<Produto[]>(this.url).pipe(
      timeout(environment.timeoutMs),
      map(produtos => ({ produtos: produtos.filter(p => p.ativo !== false), origem: 'api' as const })),
      catchError(erro =>
        environment.usarDadosExemploSeApiOffline
          ? of({ produtos: PRODUTOS_EXEMPLO, origem: 'exemplo' as const })
          : throwError(() => erro)
      )
    );
  }

  buscarPorId(id: number): Observable<Produto> {
    return this.http.get<Produto>(`${this.url}/${id}`);
  }
}
