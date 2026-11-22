import { CurrencyPipe } from '@angular/common';
import { Component, inject, output, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { mensagemDeErro } from '../../../core/erro-api';
import { somenteDigitos } from '../../../core/mascaras';
import { Configuracao, Horario, TaxaEntrega, TaxaEntregaCadastro } from '../../../core/models/loja';
import { AdminService } from '../../../core/services/admin.service';

interface Ajuste {
  chave: string;
  rotulo: string;
  dica: string;
}

const AJUSTES: Ajuste[] = [
  { chave: 'valor_minimo_delivery', rotulo: 'Pedido mínimo do delivery (R$)', dica: 'RN05: a taxa de entrega não entra na conta' },
  { chave: 'limite_padrao_oferta_dia', rotulo: 'Limite diário por CPF nas ofertas', dica: 'RN06: vale quando o produto não tem limite próprio' },
  { chave: 'minutos_expiracao_pedido', rotulo: 'Minutos até o pedido sem pagamento expirar', dica: '0 desliga a expiração' },
  { chave: 'estoque_baixo', rotulo: 'Alerta de estoque baixo (unidades)', dica: 'Abaixo disso o produto aparece como estoque baixo' }
];

type GrupoHorario = FormGroup<{
  abre: FormControl<string>;
  fecha: FormControl<string>;
  delivery: FormControl<boolean>;
}>;

function grupoTaxa(t?: TaxaEntrega) {
  return new FormGroup({
    bairro: new FormControl(t?.bairro ?? '', { nonNullable: true, validators: [Validators.required, Validators.maxLength(80)] }),
    cepPrefixo: new FormControl(t?.cepPrefixo ?? '', { nonNullable: true, validators: [Validators.pattern(/^\d{5}$/)] }),
    valor: new FormControl(t ? String(t.valor) : '', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^\d+([.,]\d{1,2})?$/)] })
  });
}

@Component({
  selector: 'app-painel-loja',
  imports: [CurrencyPipe, ReactiveFormsModule],
  templateUrl: './painel-loja.html',
  styleUrl: './painel-loja.css'
})
export class PainelLoja {
  private readonly admin = inject(AdminService);

  readonly aviso = output<string>();

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  readonly dias = signal<Horario[]>([]);
  readonly formHorarios = new FormGroup({ dias: new FormArray<GrupoHorario>([]) });
  readonly salvandoHorarios = signal(false);
  readonly erroHorarios = signal<string | null>(null);

  readonly taxas = signal<TaxaEntrega[]>([]);
  readonly formNovaTaxa = grupoTaxa();
  readonly formEdicaoTaxa = grupoTaxa();
  readonly editandoTaxa = signal<number | null>(null);
  readonly confirmarRemocao = signal<number | null>(null);
  readonly salvandoTaxa = signal(false);
  readonly erroTaxas = signal<string | null>(null);

  readonly ajustes = signal<Ajuste[]>([]);
  readonly formAjustes = new FormGroup<Record<string, FormControl<string>>>({});
  readonly salvandoAjustes = signal(false);
  readonly erroAjustes = signal<string | null>(null);

  constructor() {
    this.carregar();
  }

  get linhas(): FormArray<GrupoHorario> {
    return this.formHorarios.controls.dias;
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    forkJoin({ horarios: this.admin.horarios(), taxas: this.admin.taxasEntrega(), configuracoes: this.admin.configuracoes() })
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: ({ horarios, taxas, configuracoes }) => {
          this.montarHorarios(horarios);
          this.taxas.set(this.ordenarTaxas(taxas));
          this.montarAjustes(configuracoes);
        },
        error: e => this.erro.set(mensagemDeErro(e, 'Não foi possível carregar as configurações da loja.'))
      });
  }

  situacao(i: number): string {
    const { abre, fecha } = this.linhas.at(i).getRawValue();
    if (!abre || !fecha) return '';
    if (abre === fecha) return 'Aberto 24 horas';
    if (fecha < abre) return `Fecha às ${fecha} do dia seguinte`;
    return '';
  }

  salvarHorarios(): void {
    if (this.salvandoHorarios()) return;
    const mudados = this.linhas.controls
      .map((grupo, i) => ({ grupo, dia: this.dias()[i] }))
      .filter(({ grupo }) => grupo.dirty);
    if (!mudados.length) {
      this.aviso.emit('Nenhum horário mudou.');
      return;
    }
    if (mudados.some(({ grupo }) => grupo.invalid)) {
      this.erroHorarios.set('Preencha abertura e fechamento de todos os dias alterados.');
      return;
    }
    this.salvandoHorarios.set(true);
    this.erroHorarios.set(null);
    forkJoin(
      mudados.map(({ grupo, dia }) => {
        const v = grupo.getRawValue();
        return this.admin.atualizarHorario(dia.diaSemana, v.abre, v.fecha, v.delivery);
      })
    )
      .pipe(finalize(() => this.salvandoHorarios.set(false)))
      .subscribe({
        next: salvos => {
          const porDia = new Map(salvos.map(h => [h.diaSemana, h]));
          this.dias.update(lista => lista.map(d => porDia.get(d.diaSemana) ?? d));
          this.linhas.markAsPristine();
          this.aviso.emit(salvos.length === 1 ? 'Horário salvo.' : `${salvos.length} horários salvos.`);
        },
        error: e => this.erroHorarios.set(mensagemDeErro(e))
      });
  }

  criarTaxa(): void {
    if (this.formNovaTaxa.invalid) {
      this.formNovaTaxa.markAllAsTouched();
      return;
    }
    if (this.salvandoTaxa()) return;
    this.salvandoTaxa.set(true);
    this.erroTaxas.set(null);
    this.admin
      .criarTaxaEntrega(this.cadastroTaxa(this.formNovaTaxa))
      .pipe(finalize(() => this.salvandoTaxa.set(false)))
      .subscribe({
        next: t => {
          this.taxas.update(lista => this.ordenarTaxas([...lista, t]));
          this.formNovaTaxa.reset();
          this.aviso.emit(`Agora a loja entrega em ${t.bairro}.`);
        },
        error: e => this.erroTaxas.set(mensagemDeErro(e))
      });
  }

  editarTaxa(t: TaxaEntrega): void {
    this.editandoTaxa.set(t.id);
    this.confirmarRemocao.set(null);
    this.formEdicaoTaxa.setValue({ bairro: t.bairro, cepPrefixo: t.cepPrefixo ?? '', valor: String(t.valor) });
  }

  salvarTaxa(t: TaxaEntrega): void {
    if (this.formEdicaoTaxa.invalid) {
      this.formEdicaoTaxa.markAllAsTouched();
      return;
    }
    if (this.salvandoTaxa()) return;
    this.salvandoTaxa.set(true);
    this.erroTaxas.set(null);
    this.admin
      .atualizarTaxaEntrega(t.id, this.cadastroTaxa(this.formEdicaoTaxa))
      .pipe(finalize(() => this.salvandoTaxa.set(false)))
      .subscribe({
        next: atualizada => {
          this.taxas.update(lista => this.ordenarTaxas(lista.map(x => (x.id === atualizada.id ? atualizada : x))));
          this.editandoTaxa.set(null);
          this.aviso.emit(`Taxa de ${atualizada.bairro} atualizada.`);
        },
        error: e => this.erroTaxas.set(mensagemDeErro(e))
      });
  }

  removerTaxa(t: TaxaEntrega): void {
    if (this.confirmarRemocao() !== t.id) {
      this.confirmarRemocao.set(t.id);
      return;
    }
    this.salvandoTaxa.set(true);
    this.erroTaxas.set(null);
    this.admin
      .removerTaxaEntrega(t.id)
      .pipe(
        finalize(() => {
          this.salvandoTaxa.set(false);
          this.confirmarRemocao.set(null);
        })
      )
      .subscribe({
        next: () => {
          this.taxas.update(lista => lista.filter(x => x.id !== t.id));
          this.aviso.emit(`A loja deixou de entregar em ${t.bairro}.`);
        },
        error: e => this.erroTaxas.set(mensagemDeErro(e))
      });
  }

  salvarAjustes(): void {
    if (this.salvandoAjustes()) return;
    const mudados = this.ajustes().filter(a => this.formAjustes.controls[a.chave]?.dirty);
    if (!mudados.length) {
      this.aviso.emit('Nenhum ajuste mudou.');
      return;
    }
    this.salvandoAjustes.set(true);
    this.erroAjustes.set(null);
    forkJoin(mudados.map(a => this.admin.atualizarConfiguracao(a.chave, this.formAjustes.controls[a.chave].value.trim().replace(',', '.'))))
      .pipe(finalize(() => this.salvandoAjustes.set(false)))
      .subscribe({
        next: salvos => {
          salvos.forEach(c => this.formAjustes.controls[c.chave]?.setValue(c.valor));
          this.formAjustes.markAsPristine();
          this.aviso.emit('Ajustes salvos.');
        },
        error: e => this.erroAjustes.set(mensagemDeErro(e))
      });
  }

  taxaInvalida(form: ReturnType<typeof grupoTaxa>, campo: 'bairro' | 'cepPrefixo' | 'valor'): boolean {
    const controle = form.controls[campo];
    return controle.invalid && (controle.touched || controle.dirty);
  }

  private montarHorarios(horarios: Horario[]): void {
    const ordenados = [...horarios].sort((a, b) => a.diaSemana - b.diaSemana);
    this.dias.set(ordenados);
    this.linhas.clear();
    for (const h of ordenados) {
      this.linhas.push(
        new FormGroup({
          abre: new FormControl(h.horaAbertura.slice(0, 5), { nonNullable: true, validators: [Validators.required] }),
          fecha: new FormControl(h.horaFechamento.slice(0, 5), { nonNullable: true, validators: [Validators.required] }),
          delivery: new FormControl(h.aceitaDelivery, { nonNullable: true })
        })
      );
    }
    this.linhas.markAsPristine();
  }

  private montarAjustes(configuracoes: Configuracao[]): void {
    const valores = new Map(configuracoes.map(c => [c.chave, c.valor]));
    const presentes = AJUSTES.filter(a => valores.has(a.chave));
    for (const a of presentes) {
      this.formAjustes.setControl(a.chave, new FormControl(valores.get(a.chave) ?? '', { nonNullable: true, validators: [Validators.required] }));
    }
    this.ajustes.set(presentes);
    this.formAjustes.markAsPristine();
  }

  private cadastroTaxa(form: ReturnType<typeof grupoTaxa>): TaxaEntregaCadastro {
    const v = form.getRawValue();
    const cep = somenteDigitos(v.cepPrefixo);
    return { bairro: v.bairro.trim(), cepPrefixo: cep || null, valor: Number(v.valor.replace(',', '.')) };
  }

  private ordenarTaxas(lista: TaxaEntrega[]): TaxaEntrega[] {
    return [...lista].sort((a, b) => a.bairro.localeCompare(b.bairro, 'pt-BR'));
  }
}
