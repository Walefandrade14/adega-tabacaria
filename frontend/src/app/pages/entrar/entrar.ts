import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { destinoSeguro } from '../../core/auth/destino';
import { mensagemDeErro } from '../../core/erro-api';

@Component({
  selector: 'app-entrar',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './entrar.html',
  styleUrl: './entrar.css'
})
export class Entrar {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly voltar = input<string>();

  readonly enviando = signal(false);
  readonly lento = signal(false);
  readonly tentou = signal(false);
  readonly erro = signal<string | null>(null);
  readonly verSenha = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required]
  });

  constructor() {
    if (this.auth.token() && this.auth.cliente()) {
      this.router.navigateByUrl('/minha-conta');
    }
  }

  invalido(campo: 'email' | 'senha'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.touched || this.tentou());
  }

  enviar(): void {
    this.tentou.set(true);
    if (this.form.invalid || this.enviando()) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, senha } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    const aviso = setTimeout(() => this.lento.set(true), 5000);
    this.auth
      .entrar(email.trim(), senha)
      .pipe(
        finalize(() => {
          clearTimeout(aviso);
          this.enviando.set(false);
          this.lento.set(false);
        })
      )
      .subscribe({
        next: () => this.router.navigateByUrl(destinoSeguro(this.voltar(), '/minha-conta')),
        error: e => this.erro.set(mensagemDeErro(e))
      });
  }
}
