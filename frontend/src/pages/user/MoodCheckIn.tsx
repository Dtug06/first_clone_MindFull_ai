import { useEffect, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import SafetyBadge from '../../components/ui/SafetyBadge';
import { Moon, Battery, AlertCircle, Loader2, Check } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../api/client';
import {
  newIdempotencyKey,
  rememberIdempotencyKey,
  recallIdempotencyKey,
  forgetIdempotencyKey,
} from '../../api/idempotency';
import {
  OPEN_NOTE_TEMPLATE_CODE,
  type DailyQuestionAssignmentResponse,
  type DailyAnswerRequest,
  type DailyAnswerType,
  type CheckinHistoryResponse,
} from '../../api/dailyquestion';

interface AnswerDraft {
  answerType: DailyAnswerType;
  numericValue?: number;
  textValue?: string;
  optionValue?: string;
}

interface AssignCard {
  assignment: DailyQuestionAssignmentResponse;
  draft: AnswerDraft | null;
  submitting: boolean;
  submitError: string | null;
  done: boolean;
  serverTimestamp?: string;
}

const SCALE_RANGE: Record<string, { min: number; max: number; step: number; defaultValue: number }> = {
  STRESS: { min: 1, max: 5, step: 1, defaultValue: 3 },
  ENERGY: { min: 1, max: 5, step: 1, defaultValue: 3 },
  SLEEP: { min: 0, max: 24, step: 0.5, defaultValue: 7 },
};

function friendlyMessage(e: ApiError): string {
  if (e.code === 'NETWORK_ERROR') {
    return "Can't reach MindBridge. Check your connection and try again.";
  }
  if (e.status === 401) {
    return 'Your session expired. Please sign in again.';
  }
  if (e.status === 403) {
    return "You don't have access to this resource.";
  }
  if (e.status === 404) {
    return 'This check-in is no longer available.';
  }
  if (e.status === 409) {
    return 'You already answered this today.';
  }
  if (e.status === 400) {
    return e.message || 'Please check your answer and try again.';
  }
  if (e.status >= 500) {
    return 'Something went wrong on our end. Please try again in a moment.';
  }
  return e.message;
}

function initialDraftFor(assignment: DailyQuestionAssignmentResponse): AnswerDraft | null {
  if (assignment.answered) {
    return null;
  }
  switch (assignment.questionType) {
    case 'SCALE': {
      const range = SCALE_RANGE[assignment.templateCode];
      return {
        answerType: 'NUMERIC',
        numericValue: range?.defaultValue ?? 3,
      };
    }
    case 'NUMBER': {
      const range = SCALE_RANGE[assignment.templateCode];
      return {
        answerType: 'NUMERIC',
        numericValue: range?.defaultValue ?? 7,
      };
    }
    case 'SINGLE_CHOICE': {
      const first = assignment.options?.[0];
      return {
        answerType: 'OPTION',
        optionValue: first?.value,
      };
    }
    case 'TEXT':
      return {
        answerType: 'TEXT',
        textValue: '',
      };
    default:
      return null;
  }
}

export default function MoodCheckIn() {
  const { dailyQuestionApi, lastRequestId, primeLastRequestId } = useAuth();

  const [cards, setCards] = useState<AssignCard[]>([]);
  const [loadStatus, setLoadStatus] = useState<'idle' | 'loading' | 'error' | 'ready'>(
    'loading',
  );
  const [loadError, setLoadError] = useState<string | null>(null);
  const [history, setHistory] = useState<CheckinHistoryResponse[]>([]);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [allDone, setAllDone] = useState(false);

  // -------------------- Load today's assignments --------------------

  const loadToday = useCallback(async () => {
    setLoadStatus('loading');
    setLoadError(null);
    primeLastRequestId(null);
    try {
      const assignments = await dailyQuestionApi.today();
      const next: AssignCard[] = assignments
        .filter((a) => a.templateCode !== OPEN_NOTE_TEMPLATE_CODE)
        .map((a) => ({
          assignment: a,
          draft: initialDraftFor(a),
          submitting: false,
          submitError: null,
          done: a.answered,
        }));
      setCards(next);
      setAllDone(next.every((c) => c.done || c.draft === null));
      setLoadStatus('ready');
    } catch (e) {
      if (e instanceof ApiError) {
        setLoadError(friendlyMessage(e));
        primeLastRequestId(e.requestId);
      } else if (e instanceof Error) {
        setLoadError(e.message || 'Unexpected error');
      } else {
        setLoadError('Unexpected error');
      }
      setLoadStatus('error');
    }
  }, [dailyQuestionApi, primeLastRequestId]);

  useEffect(() => {
    loadToday();
  }, [loadToday]);

  // -------------------- Load 7-day history (after first paint so it doesn't block main flow) --------------------

  useEffect(() => {
    let cancelled = false;
    setHistoryLoading(true);
    setHistoryError(null);
    dailyQuestionApi
      .history()
      .then((rows) => {
        if (!cancelled) {
          setHistory(rows);
        }
      })
      .catch((e) => {
        if (cancelled) {
          return;
        }
        if (e instanceof ApiError) {
          setHistoryError(friendlyMessage(e));
        } else {
          setHistoryError('Could not load recent check-ins.');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setHistoryLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [dailyQuestionApi]);

  // -------------------- Answer submission --------------------

  const updateDraft = useCallback((assignmentId: string, updater: (d: AnswerDraft) => AnswerDraft) => {
    setCards((prev) =>
      prev.map((c) => (c.assignment.assignmentId === assignmentId && c.draft
        ? { ...c, draft: updater(c.draft) }
        : c)),
    );
  }, []);

  const submitOne = useCallback(async (assignmentId: string) => {
    const cardIndex = cards.findIndex((c) => c.assignment.assignmentId === assignmentId);
    if (cardIndex < 0) {
      return;
    }
    const card = cards[cardIndex];
    if (!card.draft || card.draft.answerType === 'TEXT' && (!card.draft.textValue || !card.draft.textValue.trim())) {
      return;
    }
    setCards((prev) =>
      prev.map((c) => (c.assignment.assignmentId === assignmentId
        ? { ...c, submitting: true, submitError: null }
        : c)),
    );
    const idemKey = recallIdempotencyKey(`daily-checkin:answer:${assignmentId}`)
      ?? newIdempotencyKey();
    rememberIdempotencyKey(`daily-checkin:answer:${assignmentId}`, idemKey);
    const payload: DailyAnswerRequest = {
      answerType: card.draft.answerType,
      numericValue: card.draft.answerType === 'NUMERIC' ? card.draft.numericValue : undefined,
      textValue: card.draft.answerType === 'TEXT' ? card.draft.textValue : undefined,
      optionValue: card.draft.answerType === 'OPTION' ? card.draft.optionValue : undefined,
    };
    try {
      const response = await dailyQuestionApi.submitAnswer(assignmentId, payload, idemKey);
      forgetIdempotencyKey(`daily-checkin:answer:${assignmentId}`);
      setCards((prev) => {
        const next = prev.map((c) => (c.assignment.assignmentId === assignmentId
          ? { ...c, submitting: false, done: true, serverTimestamp: response.answeredAt }
          : c));
        setAllDone(next.every((c) => c.done));
        return next;
      });
    } catch (e) {
      if (e instanceof ApiError) {
        setCards((prev) =>
          prev.map((c) => (c.assignment.assignmentId === assignmentId
            ? { ...c, submitting: false, submitError: friendlyMessage(e) }
            : c)));
        primeLastRequestId(e.requestId);
      } else if (e instanceof Error) {
        setCards((prev) =>
          prev.map((c) => (c.assignment.assignmentId === assignmentId
            ? { ...c, submitting: false, submitError: e.message || 'Unexpected error' }
            : c)));
      } else {
        setCards((prev) =>
          prev.map((c) => (c.assignment.assignmentId === assignmentId
            ? { ...c, submitting: false, submitError: 'Unexpected error' }
            : c)));
      }
    }
  }, [cards, dailyQuestionApi, primeLastRequestId]);

  // -------------------- Render --------------------

  if (loadStatus === 'loading') {
    return (
      <div className="min-h-screen bg-background pb-24 lg:pb-8">
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <h1 className="text-2xl font-semibold text-textMain mb-1">
            How are you feeling?
          </h1>
          <p className="text-textMuted">Take a moment to check in with yourself.</p>
          <div className="flex justify-center mt-12">
            <Loader2 className="w-8 h-8 text-primary animate-spin" />
          </div>
        </div>
      </div>
    );
  }

  if (loadStatus === 'error') {
    return (
      <div className="min-h-screen bg-background pb-24 lg:pb-8">
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <h1 className="text-2xl font-semibold text-textMain mb-1">
            How are you feeling?
          </h1>
          <p className="text-textMuted">Take a moment to check in with yourself.</p>
          <div
            role="alert"
            className="mt-8 bg-red-50 border border-red-100 rounded-2xl p-6 flex items-start gap-3"
          >
            <AlertCircle className="w-5 h-5 text-red-500 mt-0.5 flex-shrink-0" />
            <div className="flex-1 text-sm text-red-700">
              <p>{loadError ?? 'Could not load your check-in for today.'}</p>
              {lastRequestId && (
                <p className="mt-1 text-xs text-red-500">
                  Trace ID: <span className="font-mono">{lastRequestId}</span>
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={loadToday}
              className="text-xs font-medium text-red-700 hover:text-red-900 underline whitespace-nowrap"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (allDone && cards.length > 0) {
    return (
      <div className="min-h-screen bg-background pb-24 lg:pb-8">
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <motion.div
            className="bg-surface rounded-3xl p-8 shadow-soft text-center"
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
          >
            <motion.div
              className="w-20 h-20 mx-auto mb-6 rounded-full bg-primary/10 flex items-center justify-center"
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', damping: 10 }}
            >
              <Check className="w-10 h-10 text-primary" />
            </motion.div>
            <h2 className="text-xl font-semibold text-textMain mb-2">
              Check-in saved
            </h2>
            <p className="text-textMuted">
              Your feelings are valid. Thank you for taking the time to reflect.
            </p>
            <button
              type="button"
              onClick={loadToday}
              className="mt-6 px-4 py-2 text-sm text-primary bg-primary/10 rounded-full hover:bg-primary/15 transition-colors"
            >
              Reload today's check-in
            </button>
          </motion.div>
          <HistoryPanel
            history={history}
            loading={historyLoading}
            error={historyError}
            lastRequestId={lastRequestId}
          />
          <div className="flex justify-center mt-8">
            <SafetyBadge variant="compact" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-24 lg:pb-8">
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <motion.div
          className="mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="text-2xl font-semibold text-textMain mb-1">
            How are you feeling?
          </h1>
          <p className="text-textMuted">Take a moment to check in with yourself.</p>
        </motion.div>

        {cards.length === 0 && loadStatus === 'ready' && (
          <div className="bg-surface rounded-3xl p-8 shadow-soft text-center">
            <p className="text-textMuted">
              No check-ins assigned for today. Come back tomorrow.
            </p>
          </div>
        )}

        <div className="space-y-6">
          {cards.map((card, idx) => (
            <AssignmentCard
              key={card.assignment.assignmentId}
              card={card}
              index={idx}
              onDraftChange={(updater) => updateDraft(card.assignment.assignmentId, updater)}
              onSubmit={() => submitOne(card.assignment.assignmentId)}
              lastRequestId={lastRequestId}
            />
          ))}
        </div>

        <HistoryPanel
          history={history}
          loading={historyLoading}
          error={historyError}
          lastRequestId={lastRequestId}
        />

        <motion.div
          className="flex justify-center mt-8"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6 }}
        >
          <SafetyBadge variant="compact" />
        </motion.div>
      </div>
    </div>
  );
}

// ----- per-assignment card -----

interface AssignmentCardProps {
  card: AssignCard;
  index: number;
  lastRequestId: string | null;
  onDraftChange: (updater: (d: AnswerDraft) => AnswerDraft) => void;
  onSubmit: () => void;
}

function AssignmentCard({ card, index, lastRequestId, onDraftChange, onSubmit }: AssignmentCardProps) {
  const { assignment, draft, submitting, submitError, done } = card;

  if (done) {
    return (
      <motion.div
        className="bg-surface rounded-3xl p-6 shadow-soft border border-primary/20"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.05 * index }}
      >
        <div className="flex items-center gap-2">
          <Check className="w-5 h-5 text-primary" />
          <h2 className="font-medium text-textMain">{assignment.prompt}</h2>
        </div>
        <p className="mt-2 text-sm text-textMuted">Answered today.</p>
      </motion.div>
    );
  }

  if (!draft) {
    return null;
  }

  const canSubmit = (() => {
    if (submitting) return false;
    if (draft.answerType === 'TEXT') {
      return !!(draft.textValue && draft.textValue.trim().length > 0);
    }
    if (draft.answerType === 'NUMERIC') {
      return typeof draft.numericValue === 'number' && !Number.isNaN(draft.numericValue);
    }
    if (draft.answerType === 'OPTION') {
      return typeof draft.optionValue === 'string' && draft.optionValue.length > 0;
    }
    return false;
  })();

  return (
    <motion.div
      className="bg-surface rounded-3xl p-6 shadow-soft"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.05 * index }}
    >
      <h2 className="font-medium text-textMain mb-4">{assignment.prompt}</h2>

      {assignment.questionType === 'SCALE' && typeof draft.numericValue === 'number' && (() => {
        const range = SCALE_RANGE[assignment.templateCode] ?? SCALE_RANGE.STRESS;
        const Icon = assignment.templateCode === 'STRESS' ? null : assignment.templateCode === 'ENERGY' ? Battery : null;
        return (
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                {Icon && <Icon className="w-5 h-5 text-primary" />}
              </div>
              <span className="text-primary font-semibold">{draft.numericValue}/{range.max}</span>
            </div>
            <input
              type="range"
              min={range.min}
              max={range.max}
              step={range.step}
              value={draft.numericValue}
              onChange={(e) => onDraftChange((d) => ({ ...d, numericValue: Number(e.target.value) }))}
              className="w-full h-2 bg-surfaceMuted rounded-full appearance-none cursor-pointer"
              aria-label={assignment.prompt}
            />
            <div className="flex justify-between text-xs text-textMuted mt-2">
              <span>{range.min}</span>
              <span>{range.max}</span>
            </div>
          </div>
        );
      })()}

      {assignment.questionType === 'NUMBER' && typeof draft.numericValue === 'number' && (() => {
        const range = SCALE_RANGE[assignment.templateCode] ?? SCALE_RANGE.SLEEP;
        return (
          <div>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Moon className="w-5 h-5 text-secondary" />
              </div>
              <span className="text-secondary font-semibold">{draft.numericValue}h</span>
            </div>
            <input
              type="range"
              min={range.min}
              max={range.max}
              step={range.step}
              value={draft.numericValue}
              onChange={(e) => onDraftChange((d) => ({ ...d, numericValue: Number(e.target.value) }))}
              className="w-full h-2 bg-surfaceMuted rounded-full appearance-none cursor-pointer"
              aria-label={assignment.prompt}
            />
            <div className="flex justify-between text-xs text-textMuted mt-2">
              <span>{range.min}h</span>
              <span>{range.max}h</span>
            </div>
          </div>
        );
      })()}

      {assignment.questionType === 'SINGLE_CHOICE' && assignment.options && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {assignment.options.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => onDraftChange((d) => ({ ...d, optionValue: opt.value }))}
              className={`px-3 py-3 rounded-2xl text-sm border transition-colors text-left ${
                draft.optionValue === opt.value
                  ? 'bg-primary text-white border-primary'
                  : 'bg-surfaceMuted text-textMain border-transparent hover:border-primary/30'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}

      {assignment.questionType === 'TEXT' && (
        <div>
          <textarea
            value={draft.textValue ?? ''}
            onChange={(e) => onDraftChange((d) => ({ ...d, textValue: e.target.value }))}
            placeholder="A few words about how you're feeling…"
            maxLength={5000}
            className="w-full h-32 px-4 py-3 bg-surfaceMuted rounded-2xl text-textMain placeholder:text-textMuted/60 focus:outline-none focus:ring-2 focus:ring-primary/20 resize-none"
          />
          <p className="text-xs text-textMuted mt-1">
            {(draft.textValue ?? '').length}/5000
          </p>
        </div>
      )}

      {submitError && (
        <div
          role="alert"
          className="mt-4 text-sm text-red-600 bg-red-50 px-3 py-2 rounded-xl"
        >
          {submitError}
          {lastRequestId && (
            <span className="ml-2 text-xs text-red-500 font-mono">
              {lastRequestId}
            </span>
          )}
        </div>
      )}

      <button
        type="button"
        className={`mt-4 w-full py-3 rounded-2xl font-medium text-base transition-all ${
          canSubmit
            ? 'bg-primary text-white hover:bg-primaryDark'
            : 'bg-gray-200 text-gray-400 cursor-not-allowed'
        }`}
        onClick={onSubmit}
        disabled={!canSubmit}
      >
        {submitting ? (
          <span className="inline-flex items-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" /> Saving…
          </span>
        ) : (
          'Save answer'
        )}
      </button>
    </motion.div>
  );
}

// ----- history panel -----

interface HistoryPanelProps {
  history: CheckinHistoryResponse[];
  loading: boolean;
  error: string | null;
  lastRequestId: string | null;
}

function HistoryPanel({ history, loading, error }: HistoryPanelProps) {
  const [open, setOpen] = useState(false);

  return (
    <motion.div
      className="mt-8 bg-surface rounded-3xl p-6 shadow-soft"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <button
        type="button"
        className="w-full flex items-center justify-between"
        onClick={() => setOpen((v) => !v)}
      >
        <h2 className="font-medium text-textMain">Recent check-ins</h2>
        <span className="text-textMuted text-sm">{open ? '− Hide' : '+ Show'}</span>
      </button>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="overflow-hidden"
          >
            <div className="mt-4 space-y-3">
              {loading && (
                <div className="flex justify-center py-4">
                  <Loader2 className="w-5 h-5 text-primary animate-spin" />
                </div>
              )}
              {error && !loading && (
                <p className="text-sm text-red-600">{error}</p>
              )}
              {!loading && !error && history.length === 0 && (
                <p className="text-sm text-textMuted">
                  No previous check-ins yet.
                </p>
              )}
              {!loading && !error && history.map((row) => (
                <div
                  key={row.date}
                  className="border-t border-gray-100 pt-3 first:border-t-0 first:pt-0"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-textMain">{row.date}</span>
                    <span className="text-xs text-textMuted">
                      {row.answers.length} answer{row.answers.length === 1 ? '' : 's'}
                    </span>
                  </div>
                  <div className="mt-1 text-xs text-textMuted">
                    {row.answers
                      .map((a) => new Date(a.answeredAt).toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit',
                      }))
                      .join(', ')}
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
