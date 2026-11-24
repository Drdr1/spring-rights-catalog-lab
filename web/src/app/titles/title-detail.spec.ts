import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService, Role } from '../core/auth.service';
import { appSettings } from '../core/config';
import { fakeAuth } from '../testing';
import { TitleDetail } from './title-detail';

const base = appSettings.apiBaseUrl;

async function render(roles: Role[]) {
  TestBed.configureTestingModule({
    imports: [TitleDetail],
    providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: AuthService, useValue: fakeAuth(roles) },
    ],
  });
  const fixture = TestBed.createComponent(TitleDetail);
  fixture.componentRef.setInput('id', 5);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  http.expectOne(`${base}/titles/5`).flush({ id: 5, name: 'Alpha', kind: 'FILM', releaseYear: 2024, externalId: null });
  http.expectOne(`${base}/titles/5/licenses`).flush([
    { id: 1, titleId: 5, territory: 'EG', platform: 'SVOD', startDate: '2026-01-01', endDate: '2026-06-30', exclusive: true },
  ]);
  await fixture.whenStable();
  return { fixture, http, el: fixture.nativeElement as HTMLElement };
}

describe('TitleDetail', () => {
  it('shows the title and its windows', async () => {
    const { el } = await render(['viewer']);
    expect(el.querySelector('h2')?.textContent).toContain('Alpha');
    expect(el.querySelector('tbody')?.textContent).toContain('EG');
    expect(el.querySelector('tbody')?.textContent).toContain('Yes');
  });

  it('hides the add form from viewers', async () => {
    const { el } = await render(['viewer']);
    expect(el.textContent).not.toContain('Add a window');
  });

  it('shows the API conflict message to rights managers', async () => {
    const { fixture, http, el } = await render(['rights-manager']);
    fixture.componentInstance.form.setValue({
      territory: 'eg', platform: 'SVOD', startDate: '2026-06-30', endDate: '2026-12-31', exclusive: true,
    });
    fixture.componentInstance.add();
    const req = http.expectOne(`${base}/titles/5/licenses`);
    expect(req.request.body.territory).toBe('EG');
    req.flush({ status: 409, detail: 'Overlaps an existing window for EG/SVOD where one of them is exclusive' },
      { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('Overlaps an existing window');
  });

  it('checks the date order before calling the API', async () => {
    const { fixture, http } = await render(['rights-manager']);
    fixture.componentInstance.form.setValue({
      territory: 'EG', platform: 'SVOD', startDate: '2026-12-31', endDate: '2026-01-01', exclusive: false,
    });
    fixture.componentInstance.add();
    http.expectNone(`${base}/titles/5/licenses`);
    expect(fixture.componentInstance.error()).toContain('start date');
  });
});
