import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { appSettings } from './config';
import {
  AvailableTitle, CreateTitle, CreateWindow, LicenseWindow, Platform, Problem, TitleSummary, TitleView,
} from './models';

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);
  private readonly base = appSettings.apiBaseUrl;

  search(q: string, limit = 25): Observable<TitleSummary[]> {
    const params = new HttpParams().set('q', q).set('limit', limit);
    return this.http.get<TitleSummary[]>(`${this.base}/titles`, { params });
  }

  get(id: number): Observable<TitleView> {
    return this.http.get<TitleView>(`${this.base}/titles/${id}`);
  }

  create(body: CreateTitle): Observable<TitleView> {
    return this.http.post<TitleView>(`${this.base}/titles`, body);
  }

  windows(titleId: number): Observable<LicenseWindow[]> {
    return this.http.get<LicenseWindow[]>(`${this.base}/titles/${titleId}/licenses`);
  }

  addWindow(titleId: number, body: CreateWindow): Observable<LicenseWindow> {
    return this.http.post<LicenseWindow>(`${this.base}/titles/${titleId}/licenses`, body);
  }

  availability(territory: string, platform: Platform, date: string): Observable<AvailableTitle[]> {
    const params = new HttpParams().set('territory', territory).set('platform', platform).set('date', date);
    return this.http.get<AvailableTitle[]>(`${this.base}/availability`, { params });
  }
}

/** A readable message from an API error (problem detail, or a generic fallback). */
export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const problem = err.error as Problem | null;
    if (problem?.detail) return problem.detail;
    if (err.status === 403) return 'You do not have permission for this action.';
    if (err.status === 0) return 'The API is not reachable.';
    return `Request failed (${err.status}).`;
  }
  return 'Unexpected error.';
}
