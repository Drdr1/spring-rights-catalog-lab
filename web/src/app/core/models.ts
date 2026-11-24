export type TitleKind = 'FILM' | 'SERIES' | 'DOCUMENTARY';
export type Platform = 'SVOD' | 'AVOD' | 'TVOD' | 'LINEAR';
export const PLATFORMS: Platform[] = ['SVOD', 'AVOD', 'TVOD', 'LINEAR'];

export interface TitleSummary {
  id: number;
  name: string;
  kind: TitleKind;
  releaseYear: number | null;
}

export interface TitleView extends TitleSummary {
  externalId: string | null;
}

export interface CreateTitle {
  name: string;
  kind: TitleKind;
  releaseYear: number | null;
  externalId: string | null;
}

export interface LicenseWindow {
  id: number;
  titleId: number;
  territory: string;
  platform: Platform;
  startDate: string;
  endDate: string;
  exclusive: boolean;
}

export interface CreateWindow {
  territory: string;
  platform: Platform;
  startDate: string;
  endDate: string;
  exclusive: boolean;
}

export interface AvailableTitle {
  titleId: number;
  name: string;
  endDate: string;
  exclusive: boolean;
}

/** RFC 9457 problem body returned by the API. */
export interface Problem {
  status: number;
  detail?: string;
  title?: string;
}
