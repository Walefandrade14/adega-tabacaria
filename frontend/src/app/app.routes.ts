import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', title: 'LouD Feliz no Simples | Adega & Tabacaria', loadComponent: () => import('./pages/home/home').then(m => m.Home) },
  { path: 'produtos', title: 'Produtos | LouD Feliz no Simples', loadComponent: () => import('./pages/produtos/produtos').then(m => m.Produtos) },
  { path: 'sobre', title: 'Sobre | LouD Feliz no Simples', loadComponent: () => import('./pages/sobre/sobre').then(m => m.Sobre) },
  { path: 'contato', title: 'Contato | LouD Feliz no Simples', loadComponent: () => import('./pages/contato/contato').then(m => m.Contato) },
  { path: '**', redirectTo: '' }
];
