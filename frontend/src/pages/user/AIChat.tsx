import { useEffect, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import JellyfishMascot from '../../components/ui/JellyfishMascot';
import SafetyBadge from '../../components/ui/SafetyBadge';
import { suggestedPrompts } from '../../data';
import { Send, Loader2, AlertCircle, ChevronUp } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../api/client';
import {
  newIdempotencyKey,
  rememberIdempotencyKey,
  recallIdempotencyKey,
  forgetIdempotencyKey,
} from '../../api/idempotency';
import type {
  ChatMessageResponse,
  ChatSessionResponse,
} from '../../api/chat';

const SESSION_STORAGE_KEY = 'mb:chat:sessionId';
const HISTORY_PAGE_SIZE = 20;

type LoadState =
  | { kind: 'idle' }
  | { kind: 'loading-history' }
  | { kind: 'creating-session' };

export default function AIChat() {
  const { chatApi, lastRequestId, primeLastRequestId } = useAuth();

  const [session, setSession] = useState<ChatSessionResponse | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [loadState, setLoadState] = useState<LoadState>({ kind: 'loading-history' });
  const [error, setError] = useState<{ message: string; code: string } | null>(null);
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [historyPage, setHistoryPage] = useState(0);
  const [hasMoreHistory, setHasMoreHistory] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesTopRef = useRef<HTMLDivElement>(null);
  // Anchor for idempotency: one intent = one key. Even if the user retries
  // a specific pending send, we keep the key. New intent → new key.
  const inFlightAnchorRef = useRef<string | null>(null);

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  const ensureSession = useCallback(async (): Promise<ChatSessionResponse> => {
    if (session) {
      return session;
    }
    const stored = window.sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (stored) {
      // Try to load the existing session — preserves chat history across reload.
      try {
        const page = await chatApi.listMessages(stored, 0, HISTORY_PAGE_SIZE);
        const next: ChatSessionResponse = {
          id: stored,
          title: null,
          status: 'ACTIVE',
          createdAt: '',
          updatedAt: '',
        };
        setSession(next);
        // Backend returns newest-first in pagination. UI shows oldest at top
        // so the latest message is at the bottom — reverse for display.
        const ordered = [...page.items].sort(
          (a, b) => a.createdAt.localeCompare(b.createdAt),
        );
        setMessages(ordered);
        setHistoryPage(page.page);
        setHasMoreHistory(page.page + 1 < page.totalPages);
        return next;
      } catch (e) {
        if (e instanceof ApiError) {
          // 404 → session doesn't exist anymore (cleared server-side, or
          // owned by another user since we encode no principal in the id).
          if (e.status === 404) {
            window.sessionStorage.removeItem(SESSION_STORAGE_KEY);
          } else {
            throw e;
          }
        } else {
          throw e instanceof Error ? e : new Error(String(e));
        }
      }
    }
    setLoadState({ kind: 'creating-session' });
    const created = await chatApi.createSession({});
    window.sessionStorage.setItem(SESSION_STORAGE_KEY, created.id);
    setSession(created);
    setMessages([]);
    setHistoryPage(0);
    setHasMoreHistory(false);
    return created;
  }, [chatApi, session]);

  // Initial load: resolve session, then load history.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        setError(null);
        primeLastRequestId(null);
        const active = await ensureSession();
        if (cancelled) {
          return;
        }
        // ensureSession already populated messages when reusing a stored id.
        // Only fetch when we just created a brand-new (empty) session.
        if (window.sessionStorage.getItem(SESSION_STORAGE_KEY) !== active.id) {
          // shouldn't happen — defensive
        }
      } catch (e) {
        if (cancelled) {
          return;
        }
        if (e instanceof ApiError) {
          setError({ message: e.message, code: e.code });
          primeLastRequestId(e.requestId);
        } else if (e instanceof Error) {
          setError({ message: e.message || 'Unexpected error', code: 'UNEXPECTED_ERROR' });
        } else {
          setError({ message: 'Unexpected error', code: 'UNEXPECTED_ERROR' });
        }
      } finally {
        if (!cancelled) {
          setLoadState({ kind: 'idle' });
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  // ensureSession recreated when chatApi changes; we only want initial mount.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadOlderMessages = useCallback(async () => {
    if (!session || loadingMore || !hasMoreHistory) {
      return;
    }
    setLoadingMore(true);
    try {
      const nextPage = historyPage + 1;
      const page = await chatApi.listMessages(session.id, nextPage, HISTORY_PAGE_SIZE);
      // Merge and re-sort by createdAt ASC — server returns newest-first.
      const merged = [...page.items, ...messages].sort((a, b) =>
        a.createdAt.localeCompare(b.createdAt),
      );
      setMessages(merged);
      setHistoryPage(page.page);
      setHasMoreHistory(page.page + 1 < page.totalPages);
    } catch (e) {
      if (e instanceof ApiError) {
        setError({ message: e.message, code: e.code });
        primeLastRequestId(e.requestId);
      }
    } finally {
      setLoadingMore(false);
    }
  }, [session, loadingMore, hasMoreHistory, historyPage, messages, chatApi, primeLastRequestId]);

  const handleSend = async () => {
    if (!input.trim() || !session || isSending) {
      return;
    }
    const trimmed = input.trim();
    setInput('');
    setIsSending(true);
    setError(null);

    const anchor = `chat-message:${session.id}:${newIdempotencyKey()}`;
    inFlightAnchorRef.current = anchor;
    // Honor any in-flight retry: if we already stored a key for this anchor,
    // reuse it. (Fresh click → new anchor → new key; intentional.)
    const existing = recallIdempotencyKey(anchor);
    const idemKey = existing ?? newIdempotencyKey();
    if (!existing) {
      rememberIdempotencyKey(anchor, idemKey);
    }

    try {
      const saved = await chatApi.sendMessage(
        session.id,
        { content: trimmed },
        idemKey,
      );
      setMessages((prev) => [...prev, saved]);
      forgetIdempotencyKey(anchor);
    } catch (e) {
      if (e instanceof ApiError) {
        setError({ message: friendlyMessage(e), code: e.code });
        primeLastRequestId(e.requestId);
        // 404 on stale session — try to create a new one next send.
        if (e.status === 404) {
          window.sessionStorage.removeItem(SESSION_STORAGE_KEY);
          setSession(null);
          setMessages([]);
        }
      } else if (e instanceof Error) {
        setError({ message: e.message || 'Unexpected error', code: 'UNEXPECTED_ERROR' });
      } else {
        setError({ message: 'Unexpected error', code: 'UNEXPECTED_ERROR' });
      }
    } finally {
      setIsSending(false);
      inFlightAnchorRef.current = null;
    }
  };

  const handlePromptClick = (prompt: string) => {
    setInput(prompt);
  };

  const retry = async () => {
    setError(null);
    primeLastRequestId(null);
    setLoadState({ kind: 'loading-history' });
    try {
      // Re-attempt session resolution — clears stale sessionStorage if 404.
      window.sessionStorage.removeItem(SESSION_STORAGE_KEY);
      setSession(null);
      setMessages([]);
      await ensureSession();
    } catch (e) {
      if (e instanceof ApiError) {
        setError({ message: e.message, code: e.code });
        primeLastRequestId(e.requestId);
      }
    } finally {
      setLoadState({ kind: 'idle' });
    }
  };

  return (
    <div className="min-h-screen bg-background flex flex-col">
      {/* Header */}
      <div className="bg-surface border-b border-gray-100 px-4 py-4">
        <div className="max-w-3xl mx-auto flex items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-gradient-to-br from-primary/20 to-primaryDark/20 flex items-center justify-center">
            <JellyfishMascot size="sm" />
          </div>
          <div>
            <h1 className="font-semibold text-textMain">Jellyfish Companion</h1>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
              <span className="text-sm text-textMuted">Here to listen</span>
            </div>
          </div>
        </div>
      </div>

      {/* Error banner */}
      {error && (
        <div
          role="alert"
          className="bg-red-50 border-b border-red-100 px-4 py-3"
        >
          <div className="max-w-3xl mx-auto flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-red-500 mt-0.5 flex-shrink-0" />
            <div className="flex-1 text-sm text-red-700">
              <p>{error.message}</p>
              {lastRequestId && (
                <p className="mt-1 text-xs text-red-500">
                  Trace ID: <span className="font-mono">{lastRequestId}</span>
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={retry}
              className="text-xs font-medium text-red-700 hover:text-red-900 underline whitespace-nowrap"
            >
              Retry
            </button>
          </div>
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto">
        <div className="max-w-3xl mx-auto px-4 py-6 space-y-4">
          {/* Load more button */}
          {hasMoreHistory && session && (
            <div ref={messagesTopRef} className="flex justify-center">
              <button
                type="button"
                onClick={loadOlderMessages}
                disabled={loadingMore}
                className="inline-flex items-center gap-1 px-4 py-2 text-sm text-primary bg-primary/10 rounded-full hover:bg-primary/15 transition-colors disabled:opacity-60"
              >
                {loadingMore ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <ChevronUp className="w-4 h-4" />
                )}
                Load older messages
              </button>
            </div>
          )}

          {loadState.kind === 'loading-history' && messages.length === 0 && (
            <div className="flex justify-center py-12">
              <Loader2 className="w-6 h-6 text-primary animate-spin" />
            </div>
          )}

          {loadState.kind === 'creating-session' && messages.length === 0 && (
            <div className="flex justify-center py-12 text-textMuted text-sm">
              Preparing your chat space…
            </div>
          )}

          {loadState.kind === 'idle' && messages.length === 0 && !error && (
            <motion.div
              className="flex flex-col items-center justify-center py-12"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
            >
              <JellyfishMascot size="lg" animated />
              <p className="text-textMuted mt-4 text-center max-w-sm">
                I'm here to listen. Share what's on your mind, or try one of the suggestions below.
              </p>
            </motion.div>
          )}

          {messages.map((message) => (
            <motion.div
              key={message.id}
              className={`flex ${message.role === 'USER' ? 'justify-end' : 'justify-start'}`}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <div
                className={`max-w-[80%] px-4 py-3 rounded-2xl ${
                  message.role === 'USER'
                    ? 'bg-primary text-white rounded-br-md'
                    : 'bg-surface text-textMain rounded-bl-md shadow-soft'
                }`}
              >
                <p className="leading-relaxed whitespace-pre-wrap break-words">{message.content}</p>
                <p
                  className={`text-xs mt-1 ${
                    message.role === 'USER' ? 'text-white/60' : 'text-textMuted'
                  }`}
                >
                  {new Date(message.createdAt).toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </p>
              </div>
            </motion.div>
          ))}

          {/* Sending indicator — only while POST is in flight. */}
          <AnimatePresence>
            {isSending && (
              <motion.div
                className="flex justify-end"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
              >
                <div className="bg-primary/70 px-4 py-3 rounded-2xl rounded-br-md text-white text-sm">
                  <span className="inline-flex items-center gap-1">
                    <span className="opacity-80">{input || '…'}</span>
                    <span className="ml-2 inline-flex gap-1">
                      {[0, 1, 2].map((i) => (
                        <motion.span
                          key={i}
                          className="w-1.5 h-1.5 rounded-full bg-white/80"
                          animate={{ y: [0, -3, 0] }}
                          transition={{
                            duration: 0.6,
                            repeat: Infinity,
                            delay: i * 0.1,
                          }}
                        />
                      ))}
                    </span>
                  </span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Suggested prompts */}
      {messages.length === 0 && !error && loadState.kind === 'idle' && (
        <div className="px-4 pb-4">
          <div className="max-w-3xl mx-auto">
            <p className="text-sm text-textMuted mb-3">You could start with:</p>
            <div className="flex flex-wrap gap-2">
              {suggestedPrompts.map((prompt, i) => (
                <motion.button
                  key={i}
                  type="button"
                  className="px-4 py-2 bg-surfaceMuted rounded-full text-sm text-textMain hover:bg-primary/10 hover:text-primary transition-colors"
                  onClick={() => handlePromptClick(prompt)}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  {prompt}
                </motion.button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Input */}
      <div className="bg-surface border-t border-gray-100 px-4 py-4">
        <div className="max-w-3xl mx-auto">
          <div className="flex gap-3">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSend();
                }
              }}
              placeholder="Share what's on your mind..."
              className="flex-1 px-4 py-3 bg-surfaceMuted rounded-2xl text-textMain placeholder:text-textMuted/60 focus:outline-none focus:ring-2 focus:ring-primary/20"
              disabled={isSending || !session}
            />
            <motion.button
              type="button"
              className="px-5 py-3 bg-primary text-white rounded-2xl disabled:opacity-50 disabled:cursor-not-allowed"
              onClick={handleSend}
              disabled={!input.trim() || isSending || !session}
              whileHover={!isSending && input.trim() && session ? { scale: 1.05 } : {}}
              whileTap={!isSending && input.trim() && session ? { scale: 0.95 } : {}}
            >
              {isSending ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                <Send className="w-5 h-5" />
              )}
            </motion.button>
          </div>
          <div className="flex justify-center mt-4">
            <SafetyBadge variant="compact" />
          </div>
        </div>
      </div>
    </div>
  );
}

function friendlyMessage(e: ApiError): string {
  if (e.code === 'NETWORK_ERROR') {
    return "Can't reach MindBridge. Check your connection and try again.";
  }
  if (e.status === 401) {
    return 'Your session expired. Please sign in again.';
  }
  if (e.status === 403) {
    return "You don't have access to this chat.";
  }
  if (e.status === 404) {
    return 'This chat session is no longer available. A new one was created.';
  }
  if (e.status === 409) {
    return 'This message was already saved.';
  }
  if (e.status >= 500) {
    return 'Something went wrong on our end. Please try again in a moment.';
  }
  return e.message;
}
