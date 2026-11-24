import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { AuthService } from './app/core/auth.service';

// Sign in (Keycloak, PKCE) before the app starts, so every screen has a token.
const auth = new AuthService();
auth
  .init()
  .then(() =>
    bootstrapApplication(App, {
      ...appConfig,
      providers: [...appConfig.providers, { provide: AuthService, useValue: auth }],
    }),
  )
  .catch((err) => console.error('Sign-in failed', err));
