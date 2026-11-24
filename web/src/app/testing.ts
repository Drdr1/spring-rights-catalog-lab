import { AuthService, Role } from './core/auth.service';

/** An AuthService stand-in with fixed roles and token. */
export function fakeAuth(roles: Role[], token = 'test-token'): Partial<AuthService> {
  return {
    hasRole: (r: Role) => roles.includes(r),
    token: async () => token,
    username: () => 'tester',
    logout: () => undefined,
  };
}
