import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { fakeAuth } from '../testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { appSettings } from './config';

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: fakeAuth(['viewer'], 'abc123') },
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('adds the bearer token to API requests', async () => {
    http.get(`${appSettings.apiBaseUrl}/titles`).subscribe();
    await Promise.resolve();
    const req = ctrl.expectOne(`${appSettings.apiBaseUrl}/titles`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc123');
    req.flush([]);
  });

  it('never sends the token to other origins', () => {
    http.get('https://cdn.example.com/poster.jpg').subscribe();
    const req = ctrl.expectOne('https://cdn.example.com/poster.jpg');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush('');
  });
});
