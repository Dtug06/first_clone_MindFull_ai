import { useCallback, useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Check, Loader2, RefreshCw } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../api/client';
import {
  OPEN_NOTE_TEMPLATE_CODE,
  type DailyAnswerRequest,
  type DailyQuestionAssignmentResponse,
} from '../../api/dailyquestion';
import {
  forgetIdempotencyKey,
  newIdempotencyKey,
  recallIdempotencyKey,
  rememberIdempotencyKey,
} from '../../api/idempotency';
import { useLanguage } from '../../i18n';

type DraftMap = Record<string, DailyAnswerRequest>;

const RANGE_BY_CODE: Record<string, { min: number; max: number; step: number; initial: number }> = {
  STRESS: { min: 1, max: 5, step: 1, initial: 3 },
  ENERGY: { min: 1, max: 5, step: 1, initial: 3 },
  SLEEP: { min: 0, max: 24, step: 0.5, initial: 7 },
};

function initialAnswer(question: DailyQuestionAssignmentResponse): DailyAnswerRequest | null {
  if (question.answered) return null;
  switch (question.questionType) {
    case 'SCALE':
    case 'NUMBER': {
      const range = RANGE_BY_CODE[question.templateCode] ?? {
        min: 0,
        max: 10,
        step: 1,
        initial: 5,
      };
      return { answerType: 'NUMERIC', numericValue: range.initial };
    }
    case 'SINGLE_CHOICE':
      return {
        answerType: 'OPTION',
        optionValue: question.options?.[0]?.value,
      };
    case 'TEXT':
      return { answerType: 'TEXT', textValue: '' };
  }
}

function errorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return error instanceof Error ? error.message : 'Unexpected error';
  }
  if (error.code === 'NETWORK_ERROR') return "Can't reach MindBridge. Check your connection.";
  if (error.status === 401) return 'Your session expired. Please sign in again.';
  if (error.status === 409) return 'One of these questions was already answered today.';
  return error.message;
}

export default function DailyCheckIn() {
  const { t } = useLanguage();
  const { dailyQuestionApi, primeLastRequestId } = useAuth();
  const [questions, setQuestions] = useState<DailyQuestionAssignmentResponse[]>([]);
  const [drafts, setDrafts] = useState<DraftMap>({});
  const [state, setState] = useState<'loading' | 'ready' | 'saving' | 'saved' | 'error'>('loading');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setState('loading');
    setError(null);
    primeLastRequestId(null);
    try {
      const rows = await dailyQuestionApi.today();
      const nextDrafts: DraftMap = {};
      rows.forEach((row) => {
        const draft = initialAnswer(row);
        if (draft) nextDrafts[row.assignmentId] = draft;
      });
      setQuestions(rows);
      setDrafts(nextDrafts);
      setState(rows.length > 0 && rows.every((row) => row.answered) ? 'saved' : 'ready');
    } catch (e) {
      if (e instanceof ApiError) primeLastRequestId(e.requestId);
      setError(errorMessage(e));
      setState('error');
    }
  }, [dailyQuestionApi, primeLastRequestId]);

  useEffect(() => {
    void load();
  }, [load]);

  const unanswered = useMemo(
    () => questions.filter((question) => !question.answered),
    [questions],
  );

  const canSave = unanswered.length > 0 && unanswered.every((question) => {
    const value = drafts[question.assignmentId];
    if (!value) return false;
    if (value.answerType === 'TEXT') {
      return question.templateCode === OPEN_NOTE_TEMPLATE_CODE || Boolean(value.textValue?.trim());
    }
    if (value.answerType === 'OPTION') return Boolean(value.optionValue);
    return value.numericValue !== undefined;
  });

  const updateDraft = (assignmentId: string, update: Partial<DailyAnswerRequest>) => {
    setDrafts((current) => ({
      ...current,
      [assignmentId]: { ...current[assignmentId], ...update },
    }));
  };

  const save = async () => {
    if (!canSave) return;
    setState('saving');
    setError(null);
    try {
      const submittedIds = new Set<string>();
      for (const question of unanswered) {
        const draft = drafts[question.assignmentId];
        if (
          question.templateCode === OPEN_NOTE_TEMPLATE_CODE
          && draft.answerType === 'TEXT'
          && !draft.textValue?.trim()
        ) {
          continue;
        }
        const anchor = `daily-checkin:answer:${question.assignmentId}`;
        const key = recallIdempotencyKey(anchor) ?? newIdempotencyKey();
        rememberIdempotencyKey(anchor, key);
        await dailyQuestionApi.submitAnswer(question.assignmentId, draft, key);
        forgetIdempotencyKey(anchor);
        submittedIds.add(question.assignmentId);
      }
      setQuestions((current) => current.map((question) => (
        submittedIds.has(question.assignmentId) ? { ...question, answered: true } : question
      )));
      setState('saved');
    } catch (e) {
      if (e instanceof ApiError) primeLastRequestId(e.requestId);
      setError(errorMessage(e));
      setState('error');
    }
  };

  if (state === 'loading') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Loader2 className="w-7 h-7 animate-spin text-primary" />
      </div>
    );
  }

  if (state === 'saved') {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center px-4 pb-24 lg:pb-8">
        <motion.div
          className="bg-surface rounded-3xl p-10 shadow-soft text-center max-w-md w-full"
          initial={{ opacity: 0, scale: 0.94 }}
          animate={{ opacity: 1, scale: 1 }}
        >
          <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-primary/10 flex items-center justify-center">
            <Check className="w-10 h-10 text-primary" />
          </div>
          <h2 className="text-xl font-semibold text-textMain mb-2">{t.user.dailySavedTitle}</h2>
          <p className="text-textMuted">{t.user.dailySavedDesc}</p>
          <p className="mt-4 text-xs text-textMuted">
            Answers are stored by MindBridge for your authenticated account.
          </p>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-32 lg:pb-12">
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <motion.div className="mb-6" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
          <h1 className="text-2xl font-semibold text-textMain mb-1">{t.user.dailyTitle}</h1>
          <p className="text-textMuted">{t.user.dailySubtitle}</p>
        </motion.div>

        {error && (
          <div role="alert" className="mb-4 rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">
            <p>{error}</p>
            <button type="button" onClick={() => void load()} className="mt-2 inline-flex items-center gap-1 font-medium underline">
              <RefreshCw className="w-3.5 h-3.5" /> Retry
            </button>
          </div>
        )}

        {questions.length === 0 && (
          <div className="rounded-3xl bg-surface p-8 text-center shadow-soft">
            <p className="font-medium text-textMain">No approved daily questions are available.</p>
            <p className="mt-2 text-sm text-textMuted">
              The backend must contain approved Daily Question templates before a check-in can be created.
            </p>
          </div>
        )}

        <div className="space-y-4">
          {questions.map((question) => (
            <QuestionCard
              key={question.assignmentId}
              question={question}
              value={drafts[question.assignmentId]}
              onChange={(update) => updateDraft(question.assignmentId, update)}
            />
          ))}

          {questions.length > 0 && (
            <motion.button
              type="button"
              onClick={() => void save()}
              disabled={!canSave || state === 'saving'}
              whileHover={canSave ? { scale: 1.01 } : {}}
              whileTap={canSave ? { scale: 0.99 } : {}}
              className="w-full py-3.5 rounded-2xl font-medium text-white bg-primary hover:bg-primaryDark disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {state === 'saving' ? (
                <span className="inline-flex items-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" /> Saving...
                </span>
              ) : t.user.dailySave}
            </motion.button>
          )}
        </div>
      </div>
    </div>
  );
}

function QuestionCard({
  question,
  value,
  onChange,
}: {
  question: DailyQuestionAssignmentResponse;
  value?: DailyAnswerRequest;
  onChange: (update: Partial<DailyAnswerRequest>) => void;
}) {
  if (question.answered) {
    return (
      <div className="bg-surface rounded-3xl p-6 shadow-soft border border-primary/10">
        <div className="flex items-center gap-3">
          <Check className="w-5 h-5 text-primary" />
          <div>
            <p className="font-medium text-textMain">{question.prompt}</p>
            <p className="text-sm text-textMuted">Answered for {question.assignedForDate}</p>
          </div>
        </div>
      </div>
    );
  }

  const range = RANGE_BY_CODE[question.templateCode] ?? {
    min: 0,
    max: 10,
    step: 1,
    initial: 5,
  };

  return (
    <motion.div className="bg-surface rounded-3xl p-6 shadow-soft" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
      <h2 className="font-medium text-textMain mb-4">{question.prompt}</h2>

      {(question.questionType === 'SCALE' || question.questionType === 'NUMBER') && (
        <>
          <div className="flex items-center justify-between mb-4 text-sm">
            <span className="text-textMuted">{range.min}</span>
            <span className="text-primary font-semibold text-lg">{value?.numericValue ?? range.initial}</span>
            <span className="text-textMuted">{range.max}</span>
          </div>
          <input
            type="range"
            min={range.min}
            max={range.max}
            step={range.step}
            value={value?.numericValue ?? range.initial}
            onChange={(event) => onChange({ answerType: 'NUMERIC', numericValue: Number(event.target.value) })}
            className="w-full h-2 bg-surfaceMuted rounded-full appearance-none cursor-pointer slider-primary"
          />
        </>
      )}

      {question.questionType === 'SINGLE_CHOICE' && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {(question.options ?? []).map((option) => {
            const selected = value?.optionValue === option.value;
            return (
              <button
                key={option.value}
                type="button"
                onClick={() => onChange({ answerType: 'OPTION', optionValue: option.value })}
                className={`rounded-2xl border px-4 py-3 text-sm text-left transition-colors ${
                  selected
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-gray-100 bg-surfaceMuted/50 text-textMain hover:border-primary/30'
                }`}
              >
                {option.label}
              </button>
            );
          })}
        </div>
      )}

      {question.questionType === 'TEXT' && (
        <textarea
          value={value?.textValue ?? ''}
          maxLength={5000}
          rows={3}
          onChange={(event) => onChange({ answerType: 'TEXT', textValue: event.target.value })}
          className="w-full px-4 py-3 rounded-2xl bg-surfaceMuted/60 border border-transparent focus:border-primary/30 focus:outline-none text-sm text-textMain resize-none"
        />
      )}
    </motion.div>
  );
}
