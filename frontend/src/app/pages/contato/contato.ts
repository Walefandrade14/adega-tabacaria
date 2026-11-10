import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LOJA, linkWhatsapp } from '../../core/loja';

@Component({
  selector: 'app-contato',
  imports: [ReactiveFormsModule],
  templateUrl: './contato.html',
  styleUrl: './contato.css'
})
export class Contato {
  private readonly fb = inject(FormBuilder);

  readonly loja = LOJA;
  readonly whatsapp = linkWhatsapp();
  readonly enviado = signal(false);

  readonly assuntos = ['Fazer um pedido', 'Dúvida sobre produto', 'Delivery', 'Outro'];

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(2)]],
    assunto: [this.assuntos[0], Validators.required],
    mensagem: ['', [Validators.required, Validators.minLength(10)]]
  });

  invalido(campo: 'nome' | 'mensagem'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.touched || this.enviado());
  }

  enviar(): void {
    this.enviado.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nome, assunto, mensagem } = this.form.getRawValue();
    const texto = `Olá! Sou ${nome}.\nAssunto: ${assunto}\n\n${mensagem}`;
    window.open(linkWhatsapp(texto), '_blank', 'noopener');
    this.form.reset();
    this.enviado.set(false);
  }
}
