export type EntryType = 'FLARE_UP' | 'ANTIHISTAMINE' | 'CORTISONE' | 'CONSUMPTION';

export interface LogEntry {
  id: string;
  timestamp: string; // ISO 8601 Extended Format
  type: EntryType;
  itemName?: string;
  category?: string;
  amount?: string;
  notes?: string;
  status?: string;
}

export interface SummaryMetrics {
  last24Hours: number;
  last48Hours: number;
  last72Hours: number;
  currentMonthCount: number;
}

export interface UserProfile {
  name: string;
  age: string;
  gender: string;
  email?: string;
  physician?: string;
}

