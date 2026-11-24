import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';
import { appSettings } from './config';

/** Adds the bearer token to API calls only, never to other origins. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(appSettings.apiBaseUrl)) {
    return next(req);
  }
  const auth = inject(AuthService);
  return from(auth.token()).pipe(
    switchMap((token) => next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req)),
  );
};
