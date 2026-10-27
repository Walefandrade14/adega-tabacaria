import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AvisosService } from '../avisos.service';
import { AuthService } from './auth.service';

const AREA_RESTRITA = /^\/(minha-conta|admin(?!\/entrar))(\/|\?|$)/;

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const avisos = inject(AvisosService);
  const router = inject(Router);
  const precisaToken = req.url.startsWith(environment.apiUrl) && !req.url.startsWith(`${environment.apiUrl}/auth/`);
  const token = precisaToken ? auth.token() : null;
  const pedido = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(pedido).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status !== 401 || !token) return throwError(() => erro);
      if (auth.sessao()?.token === token) {
        const funcionario = auth.funcionario();
        const url = router.url;
        auth.sair();
        avisos.mostrar({ texto: 'Sua sessão expirou. Entre de novo para continuar.' });
        if (AREA_RESTRITA.test(url)) {
          router.navigate([funcionario ? '/admin/entrar' : '/entrar'], { queryParams: { voltar: url } });
        }
      }
      return next(req);
    })
  );
};
