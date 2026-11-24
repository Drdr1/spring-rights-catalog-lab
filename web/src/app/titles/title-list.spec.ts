import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService, Role } from '../core/auth.service';
import { appSettings } from '../core/config';
import { fakeAuth } from '../testing';
import { TitleList } from './title-list';

async function render(roles: Role[]) {
  TestBed.configureTestingModule({
    imports: [TitleList],
    providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: AuthService, useValue: fakeAuth(roles) },
    ],
  });
  const fixture = TestBed.createComponent(TitleList);
  const http = TestBed.inject(HttpTestingController);
  http.expectOne((r) => r.url === `${appSettings.apiBaseUrl}/titles`)
    .flush([{ id: 1, name: 'Dune', kind: 'FILM', releaseYear: 2021 }]);
  await fixture.whenStable();
  return fixture.nativeElement as HTMLElement;
}

describe('TitleList', () => {
  it('lists titles with links to their detail page', async () => {
    const el = await render(['viewer']);
    const link = el.querySelector('tbody a') as HTMLAnchorElement;
    expect(link.textContent).toContain('Dune');
    expect(link.getAttribute('href')).toBe('/titles/1');
  });

  it('shows the create form only to editors', async () => {
    expect((await render(['viewer'])).textContent).not.toContain('Add a title');
    TestBed.resetTestingModule();
    expect((await render(['editor'])).textContent).toContain('Add a title');
  });
});
