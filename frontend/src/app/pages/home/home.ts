import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CATEGORIAS_VITRINE } from '../../core/models/categoria';
import { CategoryIcon } from '../../shared/category-icon/category-icon';
import { LojaInfo } from '../../shared/loja-info/loja-info';

@Component({
  selector: 'app-home',
  imports: [RouterLink, CategoryIcon, LojaInfo],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {
  readonly categorias = CATEGORIAS_VITRINE;
}
