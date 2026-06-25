export type EntryType = 'FLARE_UP' | 'ANTIHISTAMINE' | 'CORTISONE';

export interface LogEntry {
  id: string;
  timestamp: string; // ISO 8601 Extended Format
  type: EntryType;
}

export interface SummaryMetrics {
  last24Hours: number;
  last48Hours: number;
  last72Hours: number;
  currentMonthCount: number;
}
