import { Routes } from '@angular/router';
import { AvailabilityPage } from './availability/availability-page';
import { TitleDetail } from './titles/title-detail';
import { TitleList } from './titles/title-list';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'titles' },
  { path: 'titles', component: TitleList },
  { path: 'titles/:id', component: TitleDetail },
  { path: 'availability', component: AvailabilityPage },
  { path: '**', redirectTo: 'titles' },
];
