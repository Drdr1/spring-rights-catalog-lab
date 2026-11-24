import { Component, inject, input, numberAttribute, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CatalogApi, errorMessage } from '../core/catalog-api';
import { LicenseWindow, Platform, PLATFORMS, TitleView } from '../core/models';

@Component({
  selector: 'app-title-detail',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <a routerLink="/titles">&larr; All titles</a>
    @if (title(); as t) {
      <h2>{{ t.name }}</h2>
      <p>{{ t.kind }} · {{ t.releaseYear ?? 'year unknown' }}</p>
    }

    <h3>License windows</h3>
    <table>
      <thead><tr><th>Territory</th><th>Platform</th><th>From</th><th>To</th><th>Exclusive</th></tr></thead>
      <tbody>
        @for (w of windows(); track w.id) {
          <tr>
            <td>{{ w.territory }}</td><td>{{ w.platform }}</td>
            <td>{{ w.startDate }}</td><td>{{ w.endDate }}</td>
            <td>{{ w.exclusive ? 'Yes' : 'No' }}</td>
          </tr>
        } @empty {
          <tr><td colspan="5">No windows yet.</td></tr>
        }
      </tbody>
    </table>

    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }

    @if (canManage) {
      <h3>Add a window</h3>
      <form class="grid" [formGroup]="form" (ngSubmit)="add()">
        <label>Territory <input formControlName="territory" maxlength="2" placeholder="EG" /></label>
        <label>Platform
          <select formControlName="platform">
            @for (p of platforms; track p) { <option [value]="p">{{ p }}</option> }
          </select>
        </label>
        <label>From <input type="date" formControlName="startDate" /></label>
        <label>To <input type="date" formControlName="endDate" /></label>
        <label class="inline"><input type="checkbox" formControlName="exclusive" /> Exclusive</label>
        <button type="submit" [disabled]="form.invalid">Add window</button>
      </form>
    }
  `,
})
export class TitleDetail implements OnInit {
  private readonly api = inject(CatalogApi);
  private readonly fb = inject(FormBuilder);
  readonly canManage = inject(AuthService).hasRole('rights-manager');

  /** Bound from the :id route parameter. */
  readonly id = input.required({ transform: numberAttribute });

  readonly platforms = PLATFORMS;
  readonly title = signal<TitleView | null>(null);
  readonly windows = signal<LicenseWindow[]>([]);
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({
    territory: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    platform: ['SVOD' as Platform, Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    exclusive: [false],
  });

  ngOnInit(): void {
    this.api.get(this.id()).subscribe({
      next: (t) => this.title.set(t),
      error: (e) => this.error.set(errorMessage(e)),
    });
    this.loadWindows();
  }

  private loadWindows(): void {
    this.api.windows(this.id()).subscribe({
      next: (w) => this.windows.set(w),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  add(): void {
    const v = this.form.getRawValue();
    if (v.startDate > v.endDate) {
      this.error.set('The start date must be on or before the end date.');
      return;
    }
    this.api.addWindow(this.id(), { ...v, territory: v.territory.toUpperCase() }).subscribe({
      next: () => { this.error.set(''); this.form.reset(); this.loadWindows(); },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
