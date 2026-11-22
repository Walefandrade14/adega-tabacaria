import { HttpContext, HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AvisosService } from '../avisos.service';
import { AuthService } from './auth.service';

const AREA_RESTRITA = /^\/(minha-conta|pedidos|admin(?!\/entrar))(\/|\?|$)/;
const METODOS_SEGUROS = ['GET', 'HEAD', 'OPTIONS'];
const REPETIDA = new HttpContextToken<boolean>(() => false);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl)) return next(req);
  const auth = inject(AuthService);
  const avisos = inject(AvisosService);
  const router = inject(Router);
  const xsrf = METODOS_SEGUROS.includes(req.method) ? null : lerCookie('XSRF-TOKEN');
  const pedido = req.clone({ withCredentials: true, setHeaders: xsrf ? { 'X-XSRF-TOKEN': xsrf } : {} });
  return next(pedido).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status !== 401 || req.url.startsWith(`${environment.apiUrl}/auth/`) || req.context.get(REPETIDA)) {
        return throwError(() => erro);
      }
      if (auth.sessao()) {
        const funcionario = auth.funcionario();
        const url = router.url;
        auth.encerrarLocal();
        avisos.mostrar({ texto: 'Sua sessão expirou. Entre de novo para continuar.' });
        if (AREA_RESTRITA.test(url)) {
          router.navigate([funcionario ? '/admin/entrar' : '/entrar'], { queryParams: { voltar: url } });
        }
      }
      if (!METODOS_SEGUROS.includes(req.method)) return throwError(() => erro);
      return next(pedido.clone({ context: new HttpContext().set(REPETIDA, true) }));
    })
  );
};

function lerCookie(nome: string): string | null {
  try {
    const achado = document.cookie.split('; ').find(parte => parte.startsWith(`${nome}=`));
    return achado ? decodeURIComponent(achado.slice(nome.length + 1)) : null;
  } catch {
    return null;
  }
}
