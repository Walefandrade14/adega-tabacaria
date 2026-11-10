import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LOJA, linkWhatsapp } from '../../core/loja';
import { Logo } from '../logo/logo';

@Component({
  selector: 'app-footer',
  imports: [RouterLink, Logo],
  templateUrl: './footer.html',
  styleUrl: './footer.css'
})
export class Footer {
  readonly loja = LOJA;
  readonly whatsapp = linkWhatsapp();
  readonly ano = new Date().getFullYear();
}
