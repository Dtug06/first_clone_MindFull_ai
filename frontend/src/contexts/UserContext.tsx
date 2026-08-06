import { createContext, useContext, useEffect, useState, ReactNode, useCallback } from 'react';
import type { MhafProfile, DailyCheckIn, UserGoal } from '../types/user';

interface UserContextValue {
  mhafProfile: MhafProfile | null;
  hasCompletedOnboarding: boolean;
  saveMhafProfile: (profile: Omit<MhafProfile, 'completed_at'>) => void;
  resetMhafProfile: () => void;

  dailyCheckIns: DailyCheckIn[];
  saveDailyCheckIn: (entry: DailyCheckIn) => void;
  getTodayCheckIn: () => DailyCheckIn | null;
  hasCheckedInToday: boolean;

  goals: UserGoal[];
  addGoal: (goal: Omit<UserGoal, 'id' | 'created_at' | 'progress' | 'completed'>) => void;
  updateGoalProgress: (id: string, progress: number) => void;
  removeGoal: (id: string) => void;

  clearAllData: () => void;
}

const UserContext = createContext<UserContextValue | undefined>(undefined);

const STORAGE_KEYS = {
  mhaf: 'mb_mhaf_profile_v1',
  daily: 'mb_daily_checkins_v1',
  goals: 'mb_user_goals_v1',
} as const;

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function writeJson(key: string, value: unknown): void {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage unavailable (private mode / quota). Fail silently — context
    // stays in-memory for this session.
  }
}

function todayKey(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function UserProvider({ children }: { children: ReactNode }) {
  const [mhafProfile, setMhafProfile] = useState<MhafProfile | null>(
    () => readJson<MhafProfile | null>(STORAGE_KEYS.mhaf, null)
  );
  const [dailyCheckIns, setDailyCheckIns] = useState<DailyCheckIn[]>(
    () => readJson<DailyCheckIn[]>(STORAGE_KEYS.daily, [])
  );
  const [goals, setGoals] = useState<UserGoal[]>(
    () => readJson<UserGoal[]>(STORAGE_KEYS.goals, [])
  );

  useEffect(() => {
    writeJson(STORAGE_KEYS.mhaf, mhafProfile);
  }, [mhafProfile]);

  useEffect(() => {
    writeJson(STORAGE_KEYS.daily, dailyCheckIns);
  }, [dailyCheckIns]);

  useEffect(() => {
    writeJson(STORAGE_KEYS.goals, goals);
  }, [goals]);

  const saveMhafProfile = useCallback((profile: Omit<MhafProfile, 'completed_at'>) => {
    setMhafProfile({ ...profile, completed_at: new Date().toISOString() });
  }, []);

  const resetMhafProfile = useCallback(() => {
    setMhafProfile(null);
  }, []);

  const saveDailyCheckIn = useCallback((entry: DailyCheckIn) => {
    setDailyCheckIns((prev) => {
      const filtered = prev.filter((c) => c.date !== entry.date);
      return [...filtered, entry].sort((a, b) => a.date.localeCompare(b.date));
    });
  }, []);

  const getTodayCheckIn = useCallback((): DailyCheckIn | null => {
    const key = todayKey();
    return dailyCheckIns.find((c) => c.date === key) ?? null;
  }, [dailyCheckIns]);

  const hasCheckedInToday = getTodayCheckIn() !== null;

  const addGoal = useCallback((goal: Omit<UserGoal, 'id' | 'created_at' | 'progress' | 'completed'>) => {
    const newGoal: UserGoal = {
      ...goal,
      id: `goal_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      created_at: new Date().toISOString(),
      progress: 0,
      completed: false,
    };
    setGoals((prev) => [...prev, newGoal]);
  }, []);

  const updateGoalProgress = useCallback((id: string, progress: number) => {
    const clamped = Math.max(0, Math.min(100, progress));
    setGoals((prev) =>
      prev.map((g) =>
        g.id === id ? { ...g, progress: clamped, completed: clamped >= 100 } : g
      )
    );
  }, []);

  const removeGoal = useCallback((id: string) => {
    setGoals((prev) => prev.filter((g) => g.id !== id));
  }, []);

  const clearAllData = useCallback(() => {
    setMhafProfile(null);
    setDailyCheckIns([]);
    setGoals([]);
  }, []);

  const value: UserContextValue = {
    mhafProfile,
    hasCompletedOnboarding: mhafProfile !== null,
    saveMhafProfile,
    resetMhafProfile,
    dailyCheckIns,
    saveDailyCheckIn,
    getTodayCheckIn,
    hasCheckedInToday,
    goals,
    addGoal,
    updateGoalProgress,
    removeGoal,
    clearAllData,
  };

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
}

export function useUser(): UserContextValue {
  const ctx = useContext(UserContext);
  if (!ctx) {
    throw new Error('useUser must be used within UserProvider');
  }
  return ctx;
}

export function useTodayCheckIn(): DailyCheckIn | null {
  const { getTodayCheckIn } = useUser();
  return getTodayCheckIn();
}
