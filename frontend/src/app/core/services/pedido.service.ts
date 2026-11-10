import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Calculo, CarrinhoRequisicao, FormaPagamento, LojaStatus, Pagamento, Pagina, Pedido, StatusPedido } from '../models/pedido';

@Injectable({ providedIn: 'root' })
export class PedidoService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  simular(requisicao: CarrinhoRequisicao): Observable<Calculo> {
    return this.http.post<Calculo>(`${this.api}/carrinho/simular`, requisicao);
  }

  criar(requisicao: CarrinhoRequisicao): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.api}/pedidos`, requisicao);
  }

  meus(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.api}/pedidos`);
  }

  pedido(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.api}/pedidos/${id}`);
  }

  pagar(pedidoId: number, forma: FormaPagamento, simularRecusa: boolean): Observable<Pagamento> {
    return this.http.post<Pagamento>(`${this.api}/pedidos/${pedidoId}/pagamentos`, { forma, simularRecusa });
  }

  confirmarPix(pagamentoId: number): Observable<Pagamento> {
    return this.http.post<Pagamento>(`${this.api}/pagamentos/${pagamentoId}/confirmar-pix`, {});
  }

  cancelar(pedidoId: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.api}/pedidos/${pedidoId}/cancelar`, {});
  }

  loja(): Observable<LojaStatus> {
    return this.http.get<LojaStatus>(`${this.api}/loja/status`);
  }

  todos(status: StatusPedido[], pagina: number, tamanho = 20): Observable<Pagina<Pedido>> {
    let params = new HttpParams().set('pagina', pagina).set('tamanho', tamanho);
    for (const s of status) params = params.append('status', s);
    return this.http.get<Pagina<Pedido>>(`${this.api}/admin/pedidos`, { params });
  }

  confirmarPixComoLoja(pagamentoId: number): Observable<Pagamento> {
    return this.http.post<Pagamento>(`${this.api}/admin/pagamentos/${pagamentoId}/confirmar-pix`, {});
  }

  alterarStatus(pedidoId: number, status: StatusPedido): Observable<Pedido> {
    return this.http.patch<Pedido>(`${this.api}/admin/pedidos/${pedidoId}/status`, { status });
  }

  cancelarComoLoja(pedidoId: number, percentualEstorno: number): Observable<Pedido> {
    return this.http.post<Pedido>(`${this.api}/admin/pedidos/${pedidoId}/cancelar`, { percentualEstorno });
  }
}
