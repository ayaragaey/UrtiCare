import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { LogEntry, EntryType } from '../types/tracker.types';
import { parseISO } from 'date-fns';

interface TrackerState {
  entries: LogEntry[];
  collisionWarning: {
    type: EntryType;
    timestamp: string;
    isEdit: boolean;
    entryId?: string;
  } | null;
  setCollisionWarning: (warning: TrackerState['collisionWarning']) => void;
  addEntry: (type: EntryType, timestamp: string, force?: boolean) => { success: boolean; collision: boolean };
  updateEntry: (id: string, newTimestamp: string, force?: boolean) => { success: boolean; collision: boolean };
  deleteEntry: (id: string) => void;
  clearEntries: () => void;
}

// Memory fallback to support multi-platform environments where localStorage is not defined (e.g. headless runtimes)
const memoryStore: Record<string, string> = {};
const crossPlatformStorage = {
  getItem: (name: string): string | null => {
    if (typeof window !== 'undefined' && window.localStorage) {
      return window.localStorage.getItem(name);
    }
    return memoryStore[name] || null;
  },
  setItem: (name: string, value: string): void => {
    if (typeof window !== 'undefined' && window.localStorage) {
      window.localStorage.setItem(name, value);
    } else {
      memoryStore[name] = value;
    }
  },
  removeItem: (name: string): void => {
    if (typeof window !== 'undefined' && window.localStorage) {
      window.localStorage.removeItem(name);
    } else {
      delete memoryStore[name];
    }
  }
};

/**
 * Checks whether an entry of the same type already exists in the same minute timeframe.
 */
function checkMinuteCollision(entries: LogEntry[], type: EntryType, timestamp: string, excludeId?: string): boolean {
  try {
    const targetMin = timestamp.substring(0, 16); // Extract 'YYYY-MM-DDTHH:mm'
    return entries.some(e => {
      if (e.id === excludeId) return false;
      if (e.type !== type) return false;
      const entryMin = e.timestamp.substring(0, 16);
      return entryMin === targetMin;
    });
  } catch (error) {
    return false;
  }
}

/**
 * Sanitizes entries on store load to prevent boot loops due to corruption.
 */
function sanitizeEntries(loadedEntries: any): LogEntry[] {
  if (!Array.isArray(loadedEntries)) return [];
  return loadedEntries.filter((e: any) => {
    if (!e || typeof e !== 'object') return false;
    if (typeof e.id !== 'string' || !e.id) return false;
    if (typeof e.timestamp !== 'string') return false;
    if (e.type !== 'FLARE_UP' && e.type !== 'ANTIHISTAMINE' && e.type !== 'CORTISONE') return false;
    
    // Check if timestamp is a valid ISO date
    try {
      parseISO(e.timestamp);
      return true;
    } catch {
      return false;
    }
  });
}

export const useTrackerStore = create<TrackerState>()(
  persist(
    (set, get) => ({
      entries: [],
      collisionWarning: null,

      setCollisionWarning: (warning) => set({ collisionWarning: warning }),

      addEntry: (type, timestamp, force = false) => {
        const { entries } = get();

        // 1. Collision detection (duplicate input in exact same minute)
        if (!force && checkMinuteCollision(entries, type, timestamp)) {
          return { success: false, collision: true };
        }

        // 2. If forced or no collision, create or merge
        const newEntry: LogEntry = {
          id: `${type}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
          timestamp,
          type,
        };

        let updatedEntries = [...entries];
        
        if (force) {
          // Auto-merge: remove existing same-minute entry before adding new one
          const targetMin = timestamp.substring(0, 16);
          updatedEntries = updatedEntries.filter(e => {
            if (e.type !== type) return true;
            return e.timestamp.substring(0, 16) !== targetMin;
          });
        }

        updatedEntries.push(newEntry);
        // Keep sorted chronologically (newest first for standard visual parsing)
        updatedEntries.sort((a, b) => b.timestamp.localeCompare(a.timestamp));

        set({ entries: updatedEntries, collisionWarning: null });
        return { success: true, collision: false };
      },

      updateEntry: (id, newTimestamp, force = false) => {
        const { entries } = get();
        const targetEntry = entries.find(e => e.id === id);
        
        if (!targetEntry) {
          return { success: false, collision: false };
        }

        // Check collision against other entries of same type in that same minute
        if (!force && checkMinuteCollision(entries, targetEntry.type, newTimestamp, id)) {
          return { success: false, collision: true };
        }

        let updatedEntries = entries.map(e => 
          e.id === id ? { ...e, timestamp: newTimestamp } : e
        );

        if (force) {
          // Auto-merge: remove other entry in that same minute, then update
          const targetMin = newTimestamp.substring(0, 16);
          updatedEntries = updatedEntries.filter(e => {
            if (e.id === id) return true; // keep this one
            if (e.type !== targetEntry.type) return true;
            return e.timestamp.substring(0, 16) !== targetMin;
          });
          
          updatedEntries = updatedEntries.map(e => 
            e.id === id ? { ...e, timestamp: newTimestamp } : e
          );
        }

        // Keep sorted
        updatedEntries.sort((a, b) => b.timestamp.localeCompare(a.timestamp));

        set({ entries: updatedEntries, collisionWarning: null });
        return { success: true, collision: false };
      },

      deleteEntry: (id) => {
        set(state => ({
          entries: state.entries.filter(e => e.id !== id)
        }));
      },

      clearEntries: () => {
        set({ entries: [], collisionWarning: null });
      },
    }),
    {
      name: 'urticare-tracker-storage',
      storage: createJSONStorage(() => crossPlatformStorage),
      // Graceful degradation logic handling corrupted background states
      migrate: (persistedState: any, version: number) => {
        if (persistedState && persistedState.entries) {
          persistedState.entries = sanitizeEntries(persistedState.entries);
        }
        return persistedState;
      }
    }
  )
);
