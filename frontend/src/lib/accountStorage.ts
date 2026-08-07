const ACCOUNT_PREFIX = 'mb:account';

const LEGACY_CHAT_SESSION_KEYS = [
  'mb:chat:sessionId',
  'mb:chat:active-session-id',
] as const;

function scopedKey(userId: string, suffix: string): string {
  return `${ACCOUNT_PREFIX}:${encodeURIComponent(userId)}:${suffix}`;
}

export interface AccountLocalStorageKeys {
  mhaf: string;
  daily: string;
  goals: string;
}

export function accountLocalStorageKeys(
  userId: string | null,
): AccountLocalStorageKeys | null {
  if (!userId) {
    return null;
  }

  return {
    mhaf: scopedKey(userId, 'mhaf-profile:v1'),
    daily: scopedKey(userId, 'daily-checkins:v1'),
    goals: scopedKey(userId, 'goals:v1'),
  };
}

function chatSessionKey(userId: string): string {
  return scopedKey(userId, 'chat-session-id:v1');
}

export function readChatSessionId(userId: string | null): string | null {
  if (!userId || typeof window === 'undefined') {
    return null;
  }

  try {
    return window.sessionStorage.getItem(chatSessionKey(userId));
  } catch {
    return null;
  }
}

export function writeChatSessionId(
  userId: string | null,
  sessionId: string,
): void {
  if (!userId || typeof window === 'undefined') {
    return;
  }

  try {
    window.sessionStorage.setItem(chatSessionKey(userId), sessionId);
  } catch {
    // The chat still works in memory when browser storage is unavailable.
  }
}

export function clearChatSessionId(userId: string | null): void {
  if (typeof window === 'undefined') {
    return;
  }

  try {
    if (userId) {
      window.sessionStorage.removeItem(chatSessionKey(userId));
    }

    // These old shared keys were not account-safe. Never reuse them.
    for (const key of LEGACY_CHAT_SESSION_KEYS) {
      window.sessionStorage.removeItem(key);
    }
  } catch {
    // Storage cleanup is best-effort and must not block sign-out.
  }
}
