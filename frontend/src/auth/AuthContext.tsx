import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { ApiClient } from '../api/client';
import { AuthApi, type LoginRequest, type RegisterRequest, type UserResponse } from '../api/auth';
import { ConsentsApi } from '../api/consents';

const STORAGE_KEY = 'mb:auth';
const FALLBACK_BASE_URL = '/api/v1';

interface PersistedAuth {
  token: string;
  user: UserResponse;
}

export interface AuthContextValue {
  token: string | null;
  user: UserResponse | null;
  loading: boolean;
  lastRequestId: string | null;
  api: ApiClient;
  authApi: AuthApi;
  consentsApi: ConsentsApi;
  login(payload: LoginRequest): Promise<void>;
  register(payload: RegisterRequest): Promise<void>;
  logout(): void;
  primeLastRequestId(requestId: string | null): void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readPersisted(): PersistedAuth | null {
  if (typeof window === 'undefined') {
    return null;
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as PersistedAuth;
    if (!parsed.token || !parsed.user) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function writePersisted(value: PersistedAuth | null): void {
  if (typeof window === 'undefined') {
    return;
  }
  if (value === null) {
    window.localStorage.removeItem(STORAGE_KEY);
    return;
  }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(value));
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [persisted, setPersisted] = useState<PersistedAuth | null>(() => readPersisted());
  const [loading, setLoading] = useState(false);
  const [lastRequestId, setLastRequestId] = useState<string | null>(null);

  const primeLastRequestId = useCallback((requestId: string | null) => {
    setLastRequestId(requestId);
  }, []);

  const handleUnauthorized = useCallback(() => {
    setPersisted(null);
    writePersisted(null);
  }, []);

  const baseUrl = useMemo(() => {
    const fromEnv = import.meta.env.VITE_API_BASE_URL;
    return (fromEnv && fromEnv.length > 0 ? fromEnv : FALLBACK_BASE_URL).replace(/\/+$/, '');
  }, []);

  const api = useMemo(
    () => new ApiClient(baseUrl, () => persisted?.token ?? null, handleUnauthorized),
    [baseUrl, persisted, handleUnauthorized],
  );

  const authApi = useMemo(() => new AuthApi(api), [api]);
  const consentsApi = useMemo(() => new ConsentsApi(api), [api]);

  const login = useCallback(async (payload: LoginRequest) => {
    setLoading(true);
    try {
      const response = await authApi.login(payload);
      const next: PersistedAuth = { token: response.accessToken, user: response.user };
      setPersisted(next);
      writePersisted(next);
    } finally {
      setLoading(false);
    }
  }, [authApi]);

  const register = useCallback(async (payload: RegisterRequest) => {
    setLoading(true);
    try {
      const response = await authApi.register(payload);
      const next: PersistedAuth = { token: response.accessToken, user: response.user };
      setPersisted(next);
      writePersisted(next);
    } finally {
      setLoading(false);
    }
  }, [authApi]);

  const logout = useCallback(() => {
    setPersisted(null);
    writePersisted(null);
  }, []);

  // Refresh `me()` once on mount if we have a token from a previous session.
  useEffect(() => {
    if (!persisted) {
      return;
    }
    let cancelled = false;
    authApi.me().then((user) => {
      if (cancelled) {
        return;
      }
      const next: PersistedAuth = { token: persisted.token, user };
      setPersisted(next);
      writePersisted(next);
    }).catch(() => {
      // 401 or transient failure — drop the cached session.
      if (!cancelled) {
        setPersisted(null);
        writePersisted(null);
      }
    });
    return () => {
      cancelled = true;
    };
  // We only want to refresh once on initial mount; authApi intentionally omitted.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const value: AuthContextValue = {
    token: persisted?.token ?? null,
    user: persisted?.user ?? null,
    loading,
    lastRequestId,
    api,
    authApi,
    consentsApi,
    login,
    register,
    logout,
    primeLastRequestId,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
