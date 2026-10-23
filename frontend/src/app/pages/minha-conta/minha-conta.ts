import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { mensagemDeErro } from '../../core/erro-api';
import { formatarCep, formatarTelefone, somenteDigitos } from '../../core/mascaras';
import { Cliente, Endereco, TaxaEntrega } from '../../core/models/conta';
import { ContaService } from '../../core/services/conta.service';

type CampoEndereco = 'cep' | 'logradouro' | 'numero' | 'bairro' | 'cidade' | 'uf';

@Component({
  selector: 'app-minha-conta',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './minha-conta.html',
  styleUrl: './minha-conta.css'
})
export class MinhaConta {
  private readonly conta = inject(ContaService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly bemvindo = input<string>();

  readonly perfil = signal<Cliente | null>(null);
  readonly enderecos = signal<Endereco[]>([]);
  readonly taxas = signal<Record<number, TaxaEntrega | null>>({});
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  readonly formAberto = signal(false);
  readonly salvando = signal(false);
  readonly tentou = signal(false);
  readonly erroEndereco = signal<string | null>(null);
  readonly buscandoCep = signal(false);
  readonly cepNaoEncontrado = signal(false);
  readonly removendo = signal<number | null>(null);
  readonly confirmarRemocao = signal<number | null>(null);

  readonly telefone = computed(() => {
    const t = this.perfil()?.telefone;
    return t ? formatarTelefone(t) : null;
  });

  readonly form = this.fb.nonNullable.group({
    cep: ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
    logradouro: ['', [Validators.required, Validators.maxLength(160)]],
    numero: ['', [Validators.required, Validators.maxLength(10)]],
    complemento: ['', Validators.maxLength(80)],
    bairro: ['', [Validators.required, Validators.maxLength(80)]],
    cidade: ['', [Validators.required, Validators.maxLength(80)]],
    uf: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    apelido: ['', Validators.maxLength(40)],
    principal: [false]
  });

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    forkJoin({ perfil: this.conta.perfil(), enderecos: this.conta.enderecos() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ perfil, enderecos }) => {
          this.perfil.set(perfil);
          this.definirEnderecos(enderecos);
          this.carregando.set(false);
          if (!enderecos.length) this.formAberto.set(true);
        },
        error: e => {
          this.erro.set(mensagemDeErro(e, 'Não foi possível carregar sua conta.'));
          this.carregando.set(false);
        }
      });
  }

  invalido(campo: CampoEndereco): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.touched || this.tentou());
  }

  entrega(id: number): TaxaEntrega | null | undefined {
    return this.taxas()[id];
  }

  cepFormatado(cep: string): string {
    return formatarCep(cep);
  }

  aoDigitarCep(): void {
    const c = this.form.controls.cep;
    c.setValue(formatarCep(c.value));
    this.cepNaoEncontrado.set(false);
    const cep = somenteDigitos(c.value);
    if (cep.length !== 8) return;
    this.buscandoCep.set(true);
    this.conta
      .buscarCep(cep)
      .pipe(finalize(() => this.buscandoCep.set(false)))
      .subscribe({
        next: r => {
          if (r.erro) {
            this.cepNaoEncontrado.set(true);
            return;
          }
          this.form.patchValue({
            logradouro: r.logradouro || this.form.controls.logradouro.value,
            bairro: r.bairro || this.form.controls.bairro.value,
            cidade: r.localidade || this.form.controls.cidade.value,
            uf: r.uf || this.form.controls.uf.value
          });
        },
        error: () => this.cepNaoEncontrado.set(true)
      });
  }

  abrirFormulario(): void {
    this.form.reset({ principal: !this.enderecos().length });
    this.tentou.set(false);
    this.erroEndereco.set(null);
    this.formAberto.set(true);
  }

  salvarEndereco(): void {
    this.tentou.set(true);
    if (this.form.invalid || this.salvando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.erroEndereco.set(null);
    this.conta
      .adicionarEndereco({
        apelido: v.apelido.trim() || null,
        logradouro: v.logradouro.trim(),
        numero: v.numero.trim(),
        complemento: v.complemento.trim() || null,
        bairro: v.bairro.trim(),
        cidade: v.cidade.trim(),
        uf: v.uf.trim().toUpperCase(),
        cep: somenteDigitos(v.cep),
        principal: v.principal
      })
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: () => {
          this.formAberto.set(false);
          this.recarregarEnderecos();
        },
        error: e => this.erroEndereco.set(mensagemDeErro(e))
      });
  }

  remover(endereco: Endereco): void {
    if (this.confirmarRemocao() !== endereco.id) {
      this.confirmarRemocao.set(endereco.id);
      return;
    }
    this.removendo.set(endereco.id);
    this.conta
      .removerEndereco(endereco.id)
      .pipe(
        finalize(() => {
          this.removendo.set(null);
          this.confirmarRemocao.set(null);
        })
      )
      .subscribe({
        next: () => this.recarregarEnderecos(),
        error: e =>
          this.erro.set(
            e?.status === 409
              ? 'Esse endereço já foi usado em um pedido e não pode ser removido.'
              : mensagemDeErro(e)
          )
      });
  }

  sair(): void {
    this.auth.sair();
    this.router.navigateByUrl('/');
  }

  private recarregarEnderecos(): void {
    this.conta.enderecos().subscribe({
      next: lista => this.definirEnderecos(lista),
      error: e => this.erro.set(mensagemDeErro(e))
    });
  }

  private definirEnderecos(lista: Endereco[]): void {
    this.enderecos.set(lista);
    for (const endereco of lista) {
      if (endereco.id in this.taxas()) continue;
      this.conta.taxaEntrega(endereco.bairro).subscribe({
        next: t => this.taxas.update(m => ({ ...m, [endereco.id]: t })),
        error: () => this.taxas.update(m => ({ ...m, [endereco.id]: null }))
      });
    }
  }
}
