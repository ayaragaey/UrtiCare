import { differenceInMinutes, differenceInSeconds, differenceInHours, parseISO } from 'date-fns';
import { LogEntry, EntryType } from '../types/tracker.types';

export const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

/**
 * Calculates the readable time gap between two timestamps.
 * Typically used to display inline gap metadata (e.g., "+17h 12m" or "+72h").
 */
export function getTimeGap(t1: string, t2: string): string {
  try {
    const d1 = parseISO(t1);
    const d2 = parseISO(t2);
    const diffMinutes = Math.abs(differenceInMinutes(d1, d2));
    const hrs = Math.floor(diffMinutes / 60);
    const mins = diffMinutes % 60;

    if (hrs > 0) {
      return `+${hrs}h${mins > 0 ? ` ${mins}m` : ''}`;
    }
    return `+${mins}m`;
  } catch (error) {
    return '';
  }
}

/**
 * Renders high-precision ticking elapsed time since a given timestamp.
 * Returns in format "[X]h [Y]m [Z]s"
 */
export function getLiveElapsed(lastTimestamp: string, now: Date): string {
  try {
    const lastDate = parseISO(lastTimestamp);
    const diffSecs = differenceInSeconds(now, lastDate);
    if (diffSecs < 0) return '0h 0m 0s';

    const hrs = Math.floor(diffSecs / 3600);
    const mins = Math.floor((diffSecs % 3600) / 60);
    const secs = diffSecs % 60;

    return `${hrs}h ${mins}m ${secs}s`;
  } catch (error) {
    return '0h 0m 0s';
  }
}

/**
 * Computes how many entries of a given type fall within the sliding window of X hours.
 */
export function getMovingWindowCount(entries: LogEntry[], type: EntryType, hours: number, now: Date): number {
  try {
    return entries.filter(e => {
      if (e.type !== type) return false;
      const diff = differenceInHours(now, parseISO(e.timestamp));
      return diff >= 0 && diff < hours;
    }).length;
  } catch (error) {
    return 0;
  }
}

/**
 * Computes the number of occurrences of a given type in a specific month and year.
 */
export function getMonthCounts(entries: LogEntry[], type: EntryType, year: number, month: number): number {
  try {
    return entries.filter(e => {
      if (e.type !== type) return false;
      const date = parseISO(e.timestamp);
      return date.getFullYear() === year && date.getMonth() === month;
    }).length;
  } catch (error) {
    return 0;
  }
}
