import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CatalogApi, errorMessage } from '../core/catalog-api';
import { TitleKind, TitleSummary } from '../core/models';

@Component({
  selector: 'app-title-list',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h2>Titles</h2>
    <form class="row" (ngSubmit)="search()">
      <input [formControl]="query" placeholder="Search titles" aria-label="Search titles" />
      <button type="submit">Search</button>
    </form>

    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }

    <table>
      <thead><tr><th>Name</th><th>Kind</th><th>Year</th></tr></thead>
      <tbody>
        @for (t of titles(); track t.id) {
          <tr>
            <td><a [routerLink]="['/titles', t.id]">{{ t.name }}</a></td>
            <td>{{ t.kind }}</td>
            <td>{{ t.releaseYear ?? '-' }}</td>
          </tr>
        } @empty {
          <tr><td colspan="3">No titles found.</td></tr>
        }
      </tbody>
    </table>

    @if (canCreate) {
      <h3>Add a title</h3>
      <form class="grid" [formGroup]="form" (ngSubmit)="create()">
        <label>Name <input formControlName="name" /></label>
        <label>Kind
          <select formControlName="kind">
            @for (k of kinds; track k) { <option [value]="k">{{ k }}</option> }
          </select>
        </label>
        <label>Release year <input type="number" formControlName="releaseYear" /></label>
        <label>External id <input formControlName="externalId" /></label>
        <button type="submit" [disabled]="form.invalid">Create</button>
      </form>
    }
  `,
})
export class TitleList {
  private readonly api = inject(CatalogApi);
  private readonly fb = inject(FormBuilder);
  readonly canCreate = inject(AuthService).hasRole('editor');

  readonly kinds: TitleKind[] = ['FILM', 'SERIES', 'DOCUMENTARY'];
  readonly titles = signal<TitleSummary[]>([]);
  readonly error = signal('');
  readonly query = this.fb.nonNullable.control('');
  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    kind: ['FILM' as TitleKind, Validators.required],
    releaseYear: [null as number | null, [Validators.min(1888), Validators.max(2100)]],
    externalId: ['', [Validators.maxLength(64), Validators.pattern(/^[A-Za-z0-9_-]*$/)]],
  });

  constructor() {
    this.search();
  }

  search(): void {
    this.api.search(this.query.value.trim()).subscribe({
      next: (rows) => { this.titles.set(rows); this.error.set(''); },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  create(): void {
    const v = this.form.getRawValue();
    this.api.create({
      name: v.name!.trim(),
      kind: v.kind!,
      releaseYear: v.releaseYear ?? null,
      externalId: v.externalId ? v.externalId : null,
    }).subscribe({
      next: () => { this.form.reset({ kind: 'FILM' }); this.search(); },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
