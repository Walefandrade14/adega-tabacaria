import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Cliente, Endereco, EnderecoViaCep, NovoEndereco, TaxaEntrega } from '../models/conta';

@Injectable({ providedIn: 'root' })
export class ContaService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  perfil(): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.api}/clientes/me`);
  }

  enderecos(): Observable<Endereco[]> {
    return this.http.get<Endereco[]>(`${this.api}/clientes/me/enderecos`);
  }

  adicionarEndereco(endereco: NovoEndereco): Observable<Endereco> {
    return this.http.post<Endereco>(`${this.api}/clientes/me/enderecos`, endereco);
  }

  removerEndereco(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/clientes/me/enderecos/${id}`);
  }

  taxaEntrega(bairro: string): Observable<TaxaEntrega> {
    return this.http.get<TaxaEntrega>(`${this.api}/entrega/taxa`, { params: { bairro } });
  }

  buscarCep(cep: string): Observable<EnderecoViaCep> {
    return this.http.get<EnderecoViaCep>(`https://viacep.com.br/ws/${cep}/json/`);
  }
}
