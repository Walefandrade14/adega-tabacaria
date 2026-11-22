import { Component, computed, inject, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../../core/erro-api';
import { formatarCpf, somenteDigitos, validadorCpf } from '../../../core/mascaras';
import { Cargo, Funcionario } from '../../../core/models/loja';
import { AdminService } from '../../../core/services/admin.service';

const ROTULOS: Record<Cargo, string> = {
  DONO: 'Dono',
  CAIXA: 'Caixa',
  ESTOQUE: 'Estoque',
  ENTREGADOR: 'Entregador'
};

@Component({
  selector: 'app-painel-equipe',
  imports: [ReactiveFormsModule],
  templateUrl: './painel-equipe.html',
  styleUrl: './painel-equipe.css'
})
export class PainelEquipe {
  private readonly admin = inject(AdminService);

  readonly aviso = output<string>();
  readonly rotulos = ROTULOS;
  readonly cargos: Cargo[] = ['CAIXA', 'ESTOQUE', 'ENTREGADOR'];

  readonly funcionarios = signal<Funcionario[]>([]);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly salvando = signal(false);
  readonly ocupado = signal<number | null>(null);
  readonly trocandoSenha = signal<number | null>(null);

  readonly ordenados = computed(() =>
    [...this.funcionarios()].sort(
      (a, b) =>
        Number(b.ativo) - Number(a.ativo) ||
        Number(b.cargo === 'DONO') - Number(a.cargo === 'DONO') ||
        a.nome.localeCompare(b.nome, 'pt-BR')
    )
  );

  readonly form = new FormGroup({
    nome: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    cpf: new FormControl('', { nonNullable: true, validators: [Validators.required, validadorCpf] }),
    cargo: new FormControl<Cargo>('CAIXA', { nonNullable: true, validators: [Validators.required] }),
    usuario: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(60)] }),
    senha: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(6), Validators.maxLength(72)] })
  });

  readonly formSenha = new FormGroup({
    senha: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(6), Validators.maxLength(72)] })
  });

  constructor() {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.admin
      .funcionarios()
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: lista => this.funcionarios.set(lista),
        error: e => this.erro.set(mensagemDeErro(e, 'Não foi possível carregar a equipe.'))
      });
  }

  aoDigitarCpf(): void {
    const controle = this.form.controls.cpf;
    controle.setValue(formatarCpf(controle.value), { emitEvent: false });
  }

  invalido(campo: keyof typeof this.form.controls): boolean {
    const controle = this.form.controls[campo];
    return controle.invalid && (controle.touched || controle.dirty);
  }

  criar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.salvando()) return;
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.erro.set(null);
    this.admin
      .criarFuncionario({ ...v, nome: v.nome.trim(), usuario: v.usuario.trim(), cpf: somenteDigitos(v.cpf) })
      .pipe(finalize(() => this.salvando.set(false)))
      .subscribe({
        next: f => {
          this.funcionarios.update(lista => [...lista, f]);
          this.form.reset();
          this.aviso.emit(`${f.nome} entrou na equipe como ${ROTULOS[f.cargo]}.`);
        },
        error: e => this.erro.set(mensagemDeErro(e))
      });
  }

  alternarAtivo(f: Funcionario): void {
    if (f.cargo === 'DONO' || this.ocupado()) return;
    this.ocupado.set(f.id);
    this.erro.set(null);
    this.admin
      .alterarAtivoFuncionario(f.id, !f.ativo)
      .pipe(finalize(() => this.ocupado.set(null)))
      .subscribe({
        next: atualizado => {
          this.substituir(atualizado);
          this.aviso.emit(atualizado.ativo ? `${f.nome} pode entrar de novo.` : `${f.nome} foi desativado e perdeu o acesso.`);
        },
        error: e => this.erro.set(mensagemDeErro(e))
      });
  }

  abrirSenha(f: Funcionario): void {
    this.trocandoSenha.set(f.id);
    this.formSenha.reset();
  }

  salvarSenha(f: Funcionario): void {
    if (this.formSenha.invalid || this.ocupado()) {
      this.formSenha.markAllAsTouched();
      return;
    }
    this.ocupado.set(f.id);
    this.erro.set(null);
    this.admin
      .alterarSenhaFuncionario(f.id, this.formSenha.controls.senha.value)
      .pipe(finalize(() => this.ocupado.set(null)))
      .subscribe({
        next: () => {
          this.trocandoSenha.set(null);
          this.aviso.emit(`Senha de ${f.nome} alterada.`);
        },
        error: e => this.erro.set(mensagemDeErro(e))
      });
  }

  private substituir(f: Funcionario): void {
    this.funcionarios.update(lista => lista.map(x => (x.id === f.id ? f : x)));
  }
}
