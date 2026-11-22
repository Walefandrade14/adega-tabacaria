import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map } from 'rxjs';
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
    return this.http.post<Sessao>(`${this.api}/cadastro`, dados).pipe(map(s => this.guardar(s)));
  }

  entrar(email: string, senha: string): Observable<Sessao> {
    return this.http.post<Sessao>(`${this.api}/login`, { email, senha }).pipe(map(s => this.guardar(s)));
  }

  entrarFuncionario(usuario: string, senha: string): Observable<Sessao> {
    return this.http.post<Sessao>(`${this.api}/funcionarios/login`, { usuario, senha }).pipe(map(s => this.guardar(s)));
  }

  alterarSenha(senhaAtual: string, novaSenha: string): Observable<Sessao> {
    return this.http
      .put<Sessao>(`${environment.apiUrl}/clientes/me/senha`, { senhaAtual, novaSenha })
      .pipe(map(s => this.guardar(s)));
  }

  renomear(nome: string): void {
    const sessao = this.sessao();
    if (sessao) this.guardar({ ...sessao, nome });
  }

  logado(): boolean {
    const sessao = this.sessao();
    if (!sessao) return false;
    if (expirada(sessao)) {
      this.encerrarLocal();
      return false;
    }
    return true;
  }

  verificar(): void {
    if (!this.logado()) return;
    this.http.get<Sessao>(`${environment.apiUrl}/sessao`).subscribe({
      next: s => {
        if (this.sessao()) this.guardar(s);
      },
      error: () => {}
    });
  }

  sair(): void {
    this.encerrarLocal();
    this.http.post<void>(`${this.api}/sair`, null).subscribe({ error: () => {} });
  }

  encerrarLocal(): void {
    this.sessao.set(null);
    try {
      localStorage.removeItem(CHAVE);
    } catch {}
  }

  private guardar(resposta: Sessao): Sessao {
    const sessao: Sessao = { tipo: resposta.tipo, expiraEm: resposta.expiraEm, nome: resposta.nome, perfil: resposta.perfil };
    this.sessao.set(sessao);
    try {
      localStorage.setItem(CHAVE, JSON.stringify(sessao));
    } catch {}
    return sessao;
  }
}

function expirada(sessao: Sessao): boolean {
  return new Date(sessao.expiraEm).getTime() <= Date.now();
}

function lerSessao(): Sessao | null {
  try {
    const bruto = localStorage.getItem(CHAVE);
    if (!bruto) return null;
    const lido = JSON.parse(bruto) as Partial<Sessao> | null;
    if (!lido?.nome || !lido.perfil || !lido.expiraEm) return null;
    const sessao: Sessao = { tipo: lido.tipo ?? 'Bearer', expiraEm: lido.expiraEm, nome: lido.nome, perfil: lido.perfil };
    return expirada(sessao) ? null : sessao;
  } catch {
    return null;
  }
}
