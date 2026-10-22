import { Component } from '@angular/core';
import { LOJA, linkWhatsapp } from '../../core/loja';

@Component({
  selector: 'app-loja-info',
  templateUrl: './loja-info.html',
  styleUrl: './loja-info.css'
})
export class LojaInfo {
  readonly loja = LOJA;
  readonly whatsapp = linkWhatsapp();
}
