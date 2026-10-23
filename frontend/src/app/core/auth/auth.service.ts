import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CadastroCliente, Sessao } from '../models/auth';

const CHAVE = 'loudfeliz.sessao';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/auth`;

  readonly sessao = signal<Sessao | null>(lerSessao());
  readonly cliente = computed(() => this.sessao()?.perfil === 'CLIENTE');
  readonly funcionario = computed(() => {
    const perfil = this.sessao()?.perfil;
    return !!perfil && perfil !== 'CLIENTE';
  });
  readonly primeiroNome = computed(() => (this.sessao()?.nome ?? '').trim().split(/\s+/)[0] ?? '');

  cadastrar(dados: CadastroCliente): Observable<Sessao> {
    return this.http.post<Sessao>(`${this.api}/cadastro`, dados).pipe(tap(s => this.guardar(s)));
  }

  entrar(email: string, senha: string): Observable<Sessao> {
    return this.http.post<Sessao>(`${this.api}/login`, { email, senha }).pipe(tap(s => this.guardar(s)));
  }

  entrarFuncionario(usuario: string, senha: string): Observable<Sessao> {
    return this.http.post<Sessao>(`${this.api}/funcionarios/login`, { usuario, senha }).pipe(tap(s => this.guardar(s)));
  }

  token(): string | null {
    const sessao = this.sessao();
    if (!sessao) return null;
    if (expirada(sessao)) {
      this.sair();
      return null;
    }
    return sessao.token;
  }

  sair(): void {
    this.sessao.set(null);
    try {
      localStorage.removeItem(CHAVE);
    } catch {}
  }

  private guardar(sessao: Sessao): void {
    this.sessao.set(sessao);
    try {
      localStorage.setItem(CHAVE, JSON.stringify(sessao));
    } catch {}
  }
}

function expirada(sessao: Sessao): boolean {
  return new Date(sessao.expiraEm).getTime() <= Date.now();
}

function lerSessao(): Sessao | null {
  try {
    const bruto = localStorage.getItem(CHAVE);
    if (!bruto) return null;
    const sessao = JSON.parse(bruto) as Sessao;
    return sessao?.token && !expirada(sessao) ? sessao : null;
  } catch {
    return null;
  }
}
