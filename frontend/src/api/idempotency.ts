/**
 * Idempotency key helper for backend API calls.
 *
 * Per G2-T08: clients should send an `Idempotency-Key: <UUID>` header for
 * write operations that may be retried (network blip, user double-clicks).
 * The server returns the same response on replay, so the user never sees
 * duplicate side effects.
 *
 * Rules:
 * 1. Generate a fresh key for each NEW user action (new click, new submit).
 * 2. NEVER reuse a key for an unrelated action — even same endpoint.
 * 3. SAME action + SAME payload on retry → SAME key (the key is bound to the
 *    intent, not the network attempt).
 *
 * Storage: sessionStorage (cleared on tab close). NOT localStorage — replay
 * across sessions is not the goal; sessionStorage is the right scope.
 *
 * Usage in page submit logic:
 *   - Generate key on first click, store under action anchor.
 *   - On retry (network recovery, button re-enable), pull key from store.
 *   - On successful completion or status change, clear the entry.
 *
 * Note: the AIChat and MoodCheckIn pages are currently MOCK_ONLY (per
 * `docs/05_IMPLEMENTATION_STATUS.md`); this helper is ready for when
 * G2-T09+ wires them to real APIs.
 */

const STORAGE_PREFIX = "mba:idem:";

/**
 * Generates a new idempotency key. Uses crypto.randomUUID() — available in
 * all modern browsers (Chrome 92+, Firefox 95+, Safari 15.4+). Returns a
 * 36-char UUID v4 string.
 */
export function newIdempotencyKey(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  // Fallback for very old browsers / SSR — RFC4122 v4.
  // Used only if crypto.randomUUID is missing; MVP reasonable.
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/**
 * Stores an idempotency key under a local action anchor so retries can
 * reuse the same key. Pass the response's resource id on success to clear.
 *
 * @param actionAnchor opaque identifier for the user's intent
 *                     (e.g. "chat-message:session-123:client-uuid")
 * @param key          the idempotency key from {@link newIdempotencyKey}
 */
export function rememberIdempotencyKey(actionAnchor: string, key: string): void {
  try {
    sessionStorage.setItem(STORAGE_PREFIX + actionAnchor, key);
  } catch {
    // sessionStorage may be unavailable (private mode quota, etc.) — fall back
    // to no-op. The caller MUST generate a fresh key for the next attempt.
  }
}

/**
 * Returns the previously stored key for the given action anchor, or null if
 * no key is stored. The caller should generate a new key if this returns null.
 */
export function recallIdempotencyKey(actionAnchor: string): string | null {
  try {
    return sessionStorage.getItem(STORAGE_PREFIX + actionAnchor);
  } catch {
    return null;
  }
}

/**
 * Clears the stored key for the given action anchor. Call after the request
 * has succeeded or the user has explicitly cancelled.
 */
export function forgetIdempotencyKey(actionAnchor: string): void {
  try {
    sessionStorage.removeItem(STORAGE_PREFIX + actionAnchor);
  } catch {
    // no-op
  }
}