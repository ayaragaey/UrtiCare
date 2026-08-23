import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { LogEntry, EntryType, UserProfile } from '../types/tracker.types';
import { parseISO } from 'date-fns';

export const isProfileComplete = (profile?: Partial<UserProfile> | null): boolean => {
  if (!profile) return false;
  const hasName = Boolean(profile.name && profile.name.trim().length > 0);
  const hasAge = Boolean(profile.age && String(profile.age).trim().length > 0);
  const hasGender = Boolean(profile.gender && profile.gender.trim().length > 0);
  return hasName && hasAge && hasGender;
};

interface TrackerState {
  entries: LogEntry[];
  collisionWarning: {
    type: EntryType;
    timestamp: string;
    isEdit: boolean;
    entryId?: string;
  } | null;
  recentlyUsedConsumptions: string[];
  favoriteConsumptions: string[];
  favoriteAntihistamine: string;
  defaultFlareUpSymptom: string;
  defaultFlareUpSeverity: string;
  profile: UserProfile;
  setCollisionWarning: (warning: TrackerState['collisionWarning']) => void;
  setProfile: (profile: Partial<UserProfile>) => void;
  addEntry: (type: EntryType, timestamp: string, force?: boolean, itemName?: string, notes?: string) => { success: boolean; collision: boolean };
  updateEntry: (id: string, newTimestamp: string, force?: boolean, itemName?: string, notes?: string) => { success: boolean; collision: boolean };
  deleteEntry: (id: string) => void;
  clearEntries: () => void;
  addFavoriteConsumption: (name: string) => void;
  removeFavoriteConsumption: (name: string) => void;
  setFavoriteAntihistamine: (name: string) => void;
  setDefaultFlareUpSymptom: (symptom: string) => void;
  setDefaultFlareUpSeverity: (severity: string) => void;
  addConsumptionEntry: (itemName: string, category: string, amount: string, notes: string, timestamp: string, force?: boolean, status?: 'Logged' | 'Trigger') => { success: boolean; collision: boolean };
  updateConsumptionEntry: (id: string, itemName: string, category: string, amount: string, notes: string, timestamp: string, force?: boolean, status?: 'Logged' | 'Trigger') => { success: boolean; collision: boolean };
  undoConsumptionLogs: (ids: string[], previousRecentlyUsed: string[]) => void;
  toggleConsumptionTriggerStatus: (id: string) => void;
  hasSeenProfileOnboarding: boolean;
  setHasSeenProfileOnboarding: (seen: boolean) => void;
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
  if (type === 'CONSUMPTION') return false;
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
    // Strip legacy seeded mock entries
    if (e.id.includes('_mock_')) return false;
    if (typeof e.timestamp !== 'string') return false;
    if (e.type !== 'FLARE_UP' && e.type !== 'ANTIHISTAMINE' && e.type !== 'CORTISONE' && e.type !== 'CONSUMPTION') return false;
    
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
      recentlyUsedConsumptions: [],
      favoriteConsumptions: [],
      favoriteAntihistamine: 'Telefast (Fexofenadine 180 mg)',
      defaultFlareUpSymptom: 'Symptom Flare-up',
      defaultFlareUpSeverity: 'Moderate',
      profile: {
        name: '',
        age: '',
        gender: '',
        email: '',
        physician: '',
      },
      hasSeenProfileOnboarding: false,

      setCollisionWarning: (warning) => set({ collisionWarning: warning }),
      setFavoriteAntihistamine: (name) => set({ favoriteAntihistamine: name }),
      setDefaultFlareUpSymptom: (symptom) => set({ defaultFlareUpSymptom: symptom }),
      setDefaultFlareUpSeverity: (severity) => set({ defaultFlareUpSeverity: severity }),
      setHasSeenProfileOnboarding: (seen) => set({ hasSeenProfileOnboarding: seen }),
      setProfile: (profileUpdates) =>
        set((state) => ({
          profile: {
            ...state.profile,
            ...profileUpdates,
          },
        })),


      addEntry: (type, timestamp, force = false, itemName, notes) => {
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
          itemName,
          notes,
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

      updateEntry: (id, newTimestamp, force = false, itemName, notes) => {
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
          e.id === id ? { 
            ...e, 
            timestamp: newTimestamp,
            itemName: itemName !== undefined ? itemName : e.itemName,
            notes: notes !== undefined ? notes : e.notes
          } : e
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
            e.id === id ? { 
              ...e, 
              timestamp: newTimestamp,
              itemName: itemName !== undefined ? itemName : e.itemName,
              notes: notes !== undefined ? notes : e.notes
            } : e
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

      addFavoriteConsumption: (name) => {
        const { favoriteConsumptions } = get();
        if (!favoriteConsumptions.includes(name)) {
          set({ favoriteConsumptions: [...favoriteConsumptions, name] });
        }
      },

      removeFavoriteConsumption: (name) => {
        const { favoriteConsumptions } = get();
        set({ favoriteConsumptions: favoriteConsumptions.filter(f => f !== name) });
      },

      addConsumptionEntry: (itemName, category, amount, notes, timestamp, force = false, status = 'Logged') => {
        const { entries, recentlyUsedConsumptions } = get();

        // Collision check
        if (!force && checkMinuteCollision(entries, 'CONSUMPTION', timestamp)) {
          return { success: false, collision: true };
        }

        // Update recently used list
        let updatedRecents = [...recentlyUsedConsumptions];
        updatedRecents = updatedRecents.filter(item => item !== itemName);
        updatedRecents.unshift(itemName);
        if (updatedRecents.length > 8) {
          updatedRecents = updatedRecents.slice(0, 8);
        }

        const newEntry: LogEntry = {
          id: `CONSUMPTION_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
          timestamp,
          type: 'CONSUMPTION',
          itemName,
          category,
          amount,
          notes,
          status,
        };

        let updatedEntries = [...entries];

        updatedEntries.push(newEntry);
        updatedEntries.sort((a, b) => b.timestamp.localeCompare(a.timestamp));

        set({
          entries: updatedEntries,
          recentlyUsedConsumptions: updatedRecents,
          collisionWarning: null,
        });

        return { success: true, collision: false };
      },

      updateConsumptionEntry: (id, itemName, category, amount, notes, timestamp, force = false, status = 'Logged') => {
        const { entries } = get();
        const targetEntry = entries.find(e => e.id === id);
        if (!targetEntry) {
          return { success: false, collision: false };
        }

        if (!force && checkMinuteCollision(entries, 'CONSUMPTION', timestamp, id)) {
          return { success: false, collision: true };
        }

        let updatedEntries = entries.map(e =>
          e.id === id ? { ...e, itemName, category, amount, notes, timestamp, status } : e
        );

        if (force) {
          updatedEntries = updatedEntries.map(e =>
            e.id === id ? { ...e, itemName, category, amount, notes, timestamp, status } : e
          );
        }

        updatedEntries.sort((a, b) => b.timestamp.localeCompare(a.timestamp));
        set({ entries: updatedEntries, collisionWarning: null });
        return { success: true, collision: false };
      },

      undoConsumptionLogs: (ids, previousRecentlyUsed) => {
        set(state => ({
          entries: state.entries.filter(e => !ids.includes(e.id)),
          recentlyUsedConsumptions: previousRecentlyUsed
        }));
      },

      toggleConsumptionTriggerStatus: (id) => {
        set(state => ({
          entries: state.entries.map(e => {
            if (e.id === id && e.type === 'CONSUMPTION') {
              const currentStatus = e.status === 'Trigger' ? 'Logged' : 'Trigger';
              return { ...e, status: currentStatus };
            }
            return e;
          })
        }));
      },
    }),
    {
      name: 'urticare-tracker-storage',
      storage: createJSONStorage(() => crossPlatformStorage),
      version: 2,
      migrate: (persistedState: any, version: number) => {
        if (version < 2) {
          return {
            ...persistedState,
            entries: [],
            recentlyUsedConsumptions: [],
          };
        }
        return persistedState;
      },
      merge: (persistedState: any, currentState: TrackerState) => {
        const persisted = (persistedState as any) || {};
        return {
          ...currentState,
          ...persisted,
          entries: sanitizeEntries(persisted.entries || currentState.entries),
          profile: {
            ...currentState.profile,
            ...(persisted.profile || {}),
          },
        };
      },
    }
  )
);
