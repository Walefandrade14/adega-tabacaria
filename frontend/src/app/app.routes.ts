import { Routes } from '@angular/router';
import { clienteGuard, funcionarioGuard } from './core/auth/guards';

export const routes: Routes = [
  { path: '', title: 'LouD Feliz no Simples | Adega & Tabacaria', loadComponent: () => import('./pages/home/home').then(m => m.Home) },
  { path: 'produtos', title: 'Produtos | LouD Feliz no Simples', loadComponent: () => import('./pages/produtos/produtos').then(m => m.Produtos) },
  { path: 'sobre', title: 'Sobre | LouD Feliz no Simples', loadComponent: () => import('./pages/sobre/sobre').then(m => m.Sobre) },
  { path: 'contato', title: 'Contato | LouD Feliz no Simples', loadComponent: () => import('./pages/contato/contato').then(m => m.Contato) },
  { path: 'entrar', title: 'Entrar | LouD Feliz no Simples', loadComponent: () => import('./pages/entrar/entrar').then(m => m.Entrar) },
  { path: 'cadastro', title: 'Cadastro | LouD Feliz no Simples', loadComponent: () => import('./pages/cadastro/cadastro').then(m => m.Cadastro) },
  {
    path: 'minha-conta',
    title: 'Minha conta | LouD Feliz no Simples',
    canActivate: [clienteGuard],
    loadComponent: () => import('./pages/minha-conta/minha-conta').then(m => m.MinhaConta)
  },
  {
    path: 'admin/entrar',
    title: 'Área do funcionário | LouD Feliz no Simples',
    loadComponent: () => import('./pages/admin/entrar/admin-entrar').then(m => m.AdminEntrar)
  },
  {
    path: 'admin',
    title: 'Painel | LouD Feliz no Simples',
    canActivate: [funcionarioGuard],
    loadComponent: () => import('./pages/admin/painel/painel').then(m => m.Painel)
  },
  { path: '**', redirectTo: '' }
];
