import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogApi, errorMessage } from '../core/catalog-api';
import { AvailableTitle, Platform, PLATFORMS } from '../core/models';

@Component({
  selector: 'app-availability-page',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <h2>What can we show?</h2>
    <form class="row" [formGroup]="form" (ngSubmit)="check()">
      <input formControlName="territory" maxlength="2" placeholder="Territory (EG)" aria-label="Territory" />
      <select formControlName="platform" aria-label="Platform">
        @for (p of platforms; track p) { <option [value]="p">{{ p }}</option> }
      </select>
      <input type="date" formControlName="date" aria-label="Date" />
      <button type="submit" [disabled]="form.invalid">Check</button>
    </form>

    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }

    @if (results(); as rows) {
      <table>
        <thead><tr><th>Title</th><th>Licensed until</th><th>Exclusive</th></tr></thead>
        <tbody>
          @for (r of rows; track r.titleId + r.endDate) {
            <tr>
              <td><a [routerLink]="['/titles', r.titleId]">{{ r.name }}</a></td>
              <td>{{ r.endDate }}</td>
              <td>{{ r.exclusive ? 'Yes' : 'No' }}</td>
            </tr>
          } @empty {
            <tr><td colspan="3">Nothing is licensed for that territory, platform and date.</td></tr>
          }
        </tbody>
      </table>
    }
  `,
})
export class AvailabilityPage {
  private readonly api = inject(CatalogApi);
  readonly platforms = PLATFORMS;
  readonly results = signal<AvailableTitle[] | null>(null);
  readonly error = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    territory: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    platform: ['SVOD' as Platform, Validators.required],
    date: [new Date().toISOString().slice(0, 10), Validators.required],
  });

  check(): void {
    const v = this.form.getRawValue();
    this.api.availability(v.territory.toUpperCase(), v.platform, v.date).subscribe({
      next: (rows) => { this.results.set(rows); this.error.set(''); },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
