import { Component, ElementRef, OnInit, afterNextRender, inject, input, output, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { mensagemDeErro } from '../../../core/erro-api';
import { Categoria } from '../../../core/models/categoria';
import { Produto, ProdutoCadastro } from '../../../core/models/produto';
import { AdminService } from '../../../core/services/admin.service';

type Campo = 'nome' | 'categoriaId' | 'preco' | 'estoqueQuantidade' | 'limiteDiarioOferta' | 'imagemUrl';

@Component({
  selector: 'app-produto-formulario',
  imports: [ReactiveFormsModule],
  templateUrl: './produto-formulario.html',
  styleUrl: './produto-formulario.css',
  host: { '(document:keydown.escape)': 'fechar.emit()' }
})
export class ProdutoFormulario implements OnInit {
  private readonly admin = inject(AdminService);
  private readonly fb = inject(FormBuilder);

  readonly produto = input<Produto | null>(null);
  readonly categorias = input.required<Categoria[]>();
  readonly salvo = output<Produto>();
  readonly fechar = output<void>();

  private readonly primeiro = viewChild<ElementRef<HTMLInputElement>>('primeiro');

  readonly salvando = signal(false);
  readonly tentou = signal(false);
  readonly erro = signal<string | null>(null);

  readonly form = this.fb.group({
    nome: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(160)]),
    categoriaId: this.fb.control<number | null>(null, Validators.required),
    preco: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    estoqueQuantidade: this.fb.control<number | null>(0, [Validators.required, Validators.min(0)]),
    descricao: this.fb.nonNullable.control(''),
    imagemUrl: this.fb.nonNullable.control('', [Validators.maxLength(255), Validators.pattern(/^(https?:\/\/\S+|img\/\S+)?$/)]),
    restritoMaiorIdade: this.fb.nonNullable.control(false),
    ofertaEspecial: this.fb.nonNullable.control(false),
    limiteDiarioOferta: this.fb.control<number | null>(null, Validators.min(1)),
    ativo: this.fb.nonNullable.control(true)
  });

  constructor() {
    afterNextRender(() => this.primeiro()?.nativeElement.focus());
  }

  ngOnInit(): void {
    const p = this.produto();
    if (p) {
      this.form.setValue({
        nome: p.nome,
        categoriaId: p.categoria.id,
        preco: p.preco,
        estoqueQuantidade: p.estoqueQuantidade,
        descricao: p.descricao ?? '',
        imagemUrl: p.imagemUrl ?? '',
        restritoMaiorIdade: p.restritoMaiorIdade,
        ofertaEspecial: p.ofertaEspecial,
        limiteDiarioOferta: p.limiteDiarioOferta ?? null,
        ativo: p.ativo
      });
    } else if (this.categorias().length) {
      this.form.controls.categoriaId.setValue(this.categorias()[0].id);
    }
  }

  invalido(campo: Campo): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.touched || this.tentou());
  }

  salvar(): void {
    this.tentou.set(true);
    if (this.form.invalid || this.salvando()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const dados: ProdutoCadastro = {
      nome: v.nome.trim(),
      descricao: v.descricao.trim() || null,
      preco: Number(v.preco),
      estoqueQuantidade: Number(v.estoqueQuantidade),
      categoriaId: Number(v.categoriaId),
      fornecedorId: this.produto()?.fornecedorId ?? null,
      restritoMaiorIdade: v.restritoMaiorIdade,
      ofertaEspecial: v.ofertaEspecial,
      limiteDiarioOferta: v.ofertaEspecial && v.limiteDiarioOferta ? Number(v.limiteDiarioOferta) : null,
      ativo: v.ativo,
      imagemUrl: v.imagemUrl.trim() || null
    };
    const atual = this.produto();
    const pedido = atual ? this.admin.atualizarProduto(atual.id, dados) : this.admin.criarProduto(dados);
    this.salvando.set(true);
    this.erro.set(null);
    pedido.pipe(finalize(() => this.salvando.set(false))).subscribe({
      next: p => this.salvo.emit(p),
      error: e => this.erro.set(mensagemDeErro(e))
    });
  }
}
