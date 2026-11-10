import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { mensagemDeErro } from '../../core/erro-api';
import { formatarCep, formatarCpf, formatarTelefone, somenteDigitos, validadorCpf } from '../../core/mascaras';
import { Cliente, Endereco, TaxaEntrega } from '../../core/models/conta';
import { ContaService } from '../../core/services/conta.service';

type CampoEndereco = 'cep' | 'logradouro' | 'numero' | 'bairro' | 'cidade' | 'uf';
type ModoDados = 'ver' | 'editar' | 'senha';
type Aviso = { tipo: 'ok' | 'erro'; texto: string };

function senhasIguais(grupo: AbstractControl): ValidationErrors | null {
  const nova = grupo.get('novaSenha')?.value;
  const confirmacao = grupo.get('confirmacao')?.value;
  return nova && confirmacao && nova !== confirmacao ? { senhasDiferentes: true } : null;
}

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
  readonly editando = signal<number | null>(null);

  readonly modoDados = signal<ModoDados>('ver');
  readonly salvandoDados = signal(false);
  readonly tentouDados = signal(false);
  readonly avisoDados = signal<Aviso | null>(null);

  readonly formPerfil = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    telefone: ['', [Validators.minLength(14)]],
    cpf: ['', [validadorCpf]]
  });

  readonly formSenha = this.fb.nonNullable.group(
    {
      senhaAtual: ['', Validators.required],
      novaSenha: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
      confirmacao: ['', Validators.required]
    },
    { validators: senhasIguais }
  );

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
    this.editando.set(null);
    this.tentou.set(false);
    this.erroEndereco.set(null);
    this.formAberto.set(true);
  }

  editar(endereco: Endereco): void {
    this.form.reset({
      cep: formatarCep(endereco.cep),
      logradouro: endereco.logradouro,
      numero: endereco.numero,
      complemento: endereco.complemento ?? '',
      bairro: endereco.bairro,
      cidade: endereco.cidade,
      uf: endereco.uf,
      apelido: endereco.apelido ?? '',
      principal: endereco.principal
    });
    this.editando.set(endereco.id);
    this.confirmarRemocao.set(null);
    this.tentou.set(false);
    this.erroEndereco.set(null);
    this.cepNaoEncontrado.set(false);
    this.formAberto.set(true);
  }

  fecharFormulario(): void {
    this.formAberto.set(false);
    this.editando.set(null);
  }

  abrirDados(modo: ModoDados): void {
    const p = this.perfil();
    this.avisoDados.set(null);
    this.tentouDados.set(false);
    if (modo === 'editar' && p) {
      this.formPerfil.reset({ nome: p.nome, telefone: p.telefone ? formatarTelefone(p.telefone) : '', cpf: '' });
    }
    if (modo === 'senha') {
      this.formSenha.reset();
    }
    this.modoDados.set(modo);
  }

  mascararCpf(): void {
    const c = this.formPerfil.controls.cpf;
    c.setValue(formatarCpf(c.value));
  }

  mascararTelefone(): void {
    const c = this.formPerfil.controls.telefone;
    c.setValue(formatarTelefone(c.value));
  }

  campoDadosInvalido(campo: 'nome' | 'telefone' | 'cpf'): boolean {
    const c = this.formPerfil.controls[campo];
    return c.invalid && (c.touched || this.tentouDados());
  }

  campoSenhaInvalido(campo: 'senhaAtual' | 'novaSenha' | 'confirmacao'): boolean {
    const c = this.formSenha.controls[campo];
    const mostrar = c.touched || this.tentouDados();
    if (campo === 'confirmacao') return mostrar && (c.invalid || this.formSenha.hasError('senhasDiferentes'));
    return mostrar && c.invalid;
  }

  salvarPerfil(): void {
    this.tentouDados.set(true);
    if (this.formPerfil.invalid || this.salvandoDados()) {
      this.formPerfil.markAllAsTouched();
      return;
    }
    const v = this.formPerfil.getRawValue();
    this.salvandoDados.set(true);
    this.avisoDados.set(null);
    this.conta
      .atualizarPerfil({ nome: v.nome.trim(), telefone: somenteDigitos(v.telefone) || null, cpf: somenteDigitos(v.cpf) || null })
      .pipe(finalize(() => this.salvandoDados.set(false)))
      .subscribe({
        next: perfil => {
          this.perfil.set(perfil);
          this.auth.renomear(perfil.nome);
          this.modoDados.set('ver');
          this.avisoDados.set({ tipo: 'ok', texto: 'Dados atualizados.' });
        },
        error: e => this.avisoDados.set({ tipo: 'erro', texto: mensagemDeErro(e) })
      });
  }

  salvarSenha(): void {
    this.tentouDados.set(true);
    if (this.formSenha.invalid || this.salvandoDados()) {
      this.formSenha.markAllAsTouched();
      return;
    }
    const v = this.formSenha.getRawValue();
    this.salvandoDados.set(true);
    this.avisoDados.set(null);
    this.auth
      .alterarSenha(v.senhaAtual, v.novaSenha)
      .pipe(finalize(() => this.salvandoDados.set(false)))
      .subscribe({
        next: () => {
          this.formSenha.reset();
          this.modoDados.set('ver');
          this.avisoDados.set({ tipo: 'ok', texto: 'Senha trocada. Quem estava logado com a senha antiga foi desconectado.' });
        },
        error: e => this.avisoDados.set({ tipo: 'erro', texto: mensagemDeErro(e) })
      });
  }

  salvarEndereco(): void {
    this.tentou.set(true);
    if (this.form.invalid || this.salvando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const dados = {
      apelido: v.apelido.trim() || null,
      logradouro: v.logradouro.trim(),
      numero: v.numero.trim(),
      complemento: v.complemento.trim() || null,
      bairro: v.bairro.trim(),
      cidade: v.cidade.trim(),
      uf: v.uf.trim().toUpperCase(),
      cep: somenteDigitos(v.cep),
      principal: v.principal
    };
    const id = this.editando();
    this.salvando.set(true);
    this.erroEndereco.set(null);
    (id === null ? this.conta.adicionarEndereco(dados) : this.conta.atualizarEndereco(id, dados))
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: () => {
          if (id !== null) {
            this.taxas.update(mapa => {
              const copia = { ...mapa };
              delete copia[id];
              return copia;
            });
          }
          this.fecharFormulario();
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
        next: () => {
          if (this.editando() === endereco.id) this.fecharFormulario();
          this.recarregarEnderecos();
        },
        error: e => this.erro.set(mensagemDeErro(e))
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
    if (!lista.length) this.formAberto.set(true);
    for (const endereco of lista) {
      if (endereco.id in this.taxas()) continue;
      this.conta.taxaEntrega(endereco.bairro).subscribe({
        next: t => this.taxas.update(m => ({ ...m, [endereco.id]: t })),
        error: () => this.taxas.update(m => ({ ...m, [endereco.id]: null }))
      });
    }
  }
}
