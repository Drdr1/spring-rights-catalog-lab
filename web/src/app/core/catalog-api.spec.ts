import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CatalogApi, errorMessage } from './catalog-api';
import { appSettings } from './config';

describe('CatalogApi', () => {
  let api: CatalogApi;
  let http: HttpTestingController;
  const base = appSettings.apiBaseUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(CatalogApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('searches with query and limit parameters', () => {
    api.search('dune').subscribe();
    const req = http.expectOne((r) => r.url === `${base}/titles`);
    expect(req.request.params.get('q')).toBe('dune');
    expect(req.request.params.get('limit')).toBe('25');
    req.flush([]);
  });

  it('posts a license window to the title', () => {
    api.addWindow(7, { territory: 'EG', platform: 'SVOD', startDate: '2026-01-01', endDate: '2026-06-30', exclusive: true })
      .subscribe();
    const req = http.expectOne(`${base}/titles/7/licenses`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.exclusive).toBe(true);
    req.flush({});
  });

  it('asks availability with territory, platform and date', () => {
    api.availability('EG', 'AVOD', '2026-02-01').subscribe();
    const req = http.expectOne((r) => r.url === `${base}/availability`);
    expect(req.request.params.toString()).toBe('territory=EG&platform=AVOD&date=2026-02-01');
    req.flush([]);
  });
});

describe('errorMessage', () => {
  it('prefers the problem detail', () => {
    const err = new HttpErrorResponse({ status: 409, error: { status: 409, detail: 'Overlaps an existing window' } });
    expect(errorMessage(err)).toBe('Overlaps an existing window');
  });

  it('explains 403 and network errors', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 403 }))).toContain('permission');
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toContain('not reachable');
  });
});
