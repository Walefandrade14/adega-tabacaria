import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LojaInfo } from '../../shared/loja-info/loja-info';

@Component({
  selector: 'app-sobre',
  imports: [RouterLink, LojaInfo],
  templateUrl: './sobre.html',
  styleUrl: './sobre.css'
})
export class Sobre {
  readonly destaques = [
    { icone: 'bi-moon-stars', titulo: 'Diurno e noturno', texto: 'Aberto quando você precisa, do happy hour à madrugada.' },
    { icone: 'bi-bicycle', titulo: 'Delivery na região', texto: 'Peça pelo site ou WhatsApp e receba gelado em casa.' },
    { icone: 'bi-person-check', titulo: 'Venda responsável', texto: 'Bebidas e tabacaria somente para maiores de 18 anos.' }
  ];
}
