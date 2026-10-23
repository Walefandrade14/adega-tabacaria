import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { mensagemDeErro } from '../../core/erro-api';
import {
  formatarCpf,
  formatarTelefone,
  idadeEm,
  somenteDigitos,
  validadorCpf,
  validadorNascimento,
  validadorSenhasIguais
} from '../../core/mascaras';

type Campo = 'nome' | 'cpf' | 'dataNascimento' | 'email' | 'telefone' | 'senha' | 'confirmacao';

@Component({
  selector: 'app-cadastro',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './cadastro.html',
  styleUrl: './cadastro.css'
})
export class Cadastro {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly enviando = signal(false);
  readonly lento = signal(false);
  readonly tentou = signal(false);
  readonly erro = signal<string | null>(null);
  readonly hoje = new Date().toISOString().slice(0, 10);

  readonly form = this.fb.nonNullable.group(
    {
      nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
      cpf: ['', [Validators.required, validadorCpf]],
      dataNascimento: ['', [Validators.required, validadorNascimento]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
      telefone: ['', [Validators.minLength(14)]],
      senha: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
      confirmacao: ['', Validators.required]
    },
    { validators: validadorSenhasIguais }
  );

  private readonly nascimento = toSignal(this.form.controls.dataNascimento.valueChanges, { initialValue: '' });
  readonly menorDeIdade = computed(() => {
    const idade = idadeEm(this.nascimento());
    return idade !== null && idade >= 0 && idade < 18;
  });

  invalido(campo: Campo): boolean {
    const c = this.form.controls[campo];
    const mostrar = c.touched || this.tentou();
    if (campo === 'confirmacao') {
      return mostrar && (c.invalid || this.form.hasError('senhasDiferentes'));
    }
    return mostrar && c.invalid;
  }

  mascararCpf(): void {
    const c = this.form.controls.cpf;
    c.setValue(formatarCpf(c.value));
  }

  mascararTelefone(): void {
    const c = this.form.controls.telefone;
    c.setValue(formatarTelefone(c.value));
  }

  enviar(): void {
    this.tentou.set(true);
    if (this.form.invalid || this.enviando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set(null);
    const aviso = setTimeout(() => this.lento.set(true), 5000);
    this.auth
      .cadastrar({
        nome: v.nome.trim(),
        cpf: somenteDigitos(v.cpf),
        dataNascimento: v.dataNascimento,
        email: v.email.trim(),
        senha: v.senha,
        telefone: somenteDigitos(v.telefone) || null
      })
      .pipe(
        finalize(() => {
          clearTimeout(aviso);
          this.enviando.set(false);
          this.lento.set(false);
        })
      )
      .subscribe({
        next: () => this.router.navigate(['/minha-conta'], { queryParams: { bemvindo: 1 } }),
        error: e => this.erro.set(mensagemDeErro(e))
      });
  }
}
