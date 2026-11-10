import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const clienteGuard: CanActivateFn = (_rota, estado) => {
  const auth = inject(AuthService);
  return auth.token() && auth.cliente()
    ? true
    : inject(Router).createUrlTree(['/entrar'], { queryParams: { voltar: estado.url } });
};

export const funcionarioGuard: CanActivateFn = (_rota, estado) => {
  const auth = inject(AuthService);
  return auth.token() && auth.funcionario()
    ? true
    : inject(Router).createUrlTree(['/admin/entrar'], { queryParams: { voltar: estado.url } });
};
