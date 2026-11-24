import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { appSettings } from './config';

export type Role = 'viewer' | 'editor' | 'rights-manager';

/** Wraps keycloak-js. Login uses the authorization-code flow with PKCE; tokens stay in memory. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private keycloak?: Keycloak;

  async init(): Promise<void> {
    this.keycloak = new Keycloak(appSettings.keycloak);
    await this.keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
  }

  /** For tests and for wiring a different client. */
  useClient(client: Keycloak): void {
    this.keycloak = client;
  }

  /** A valid access token, refreshed if it expires within 30 seconds. */
  async token(): Promise<string | undefined> {
    if (!this.keycloak?.authenticated) return undefined;
    await this.keycloak.updateToken(30);
    return this.keycloak.token;
  }

  username(): string {
    return (this.keycloak?.tokenParsed?.['preferred_username'] as string | undefined) ?? '';
  }

  hasRole(role: Role): boolean {
    return this.keycloak?.hasRealmRole(role) ?? false;
  }

  logout(): void {
    void this.keycloak?.logout();
  }
}
