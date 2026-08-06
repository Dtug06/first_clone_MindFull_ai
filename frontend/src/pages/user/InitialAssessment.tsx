import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowLeft, ArrowRight, Check } from 'lucide-react';
import { useLanguage } from '../../i18n';
import { useUser } from '../../contexts/UserContext';

type EmotionKey = 'anxious' | 'sad' | 'stressed' | 'angry' | 'lonely' | 'normal';

interface EmotionItem {
  key: EmotionKey;
  label: string;
  emoji: string;
  color: string;
}

type StepId = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9;

type Question =
  | { kind: 'choice-grid'; id: 'q1' }
  | { kind: 'emotion-grid'; id: 'q2' }
  | { kind: 'slider'; id: 'q3' | 'q4' | 'q5' | 'q6' | 'q8' }
  | { kind: 'choice-list'; id: 'q7' | 'q9' | 'q10'; vertical?: boolean };

interface Step {
  id: StepId;
  domainKey: 'domain1' | 'domain2' | 'domain3' | 'domain4' | 'domains56';
  titleKey:
    | 'stepTitle1'
    | 'stepTitle2'
    | 'stepTitle3'
    | 'stepTitle4'
    | 'stepTitle5'
    | 'stepTitle6'
    | 'stepTitle7'
    | 'stepTitle8'
    | 'stepTitle9';
  questions: Question[];
}

const STEPS: Step[] = [
  { id: 1, domainKey: 'domain1', titleKey: 'stepTitle1', questions: [{ kind: 'choice-grid', id: 'q1' }] },
  { id: 2, domainKey: 'domain2', titleKey: 'stepTitle2', questions: [{ kind: 'emotion-grid', id: 'q2' }] },
  { id: 3, domainKey: 'domain2', titleKey: 'stepTitle3', questions: [{ kind: 'slider', id: 'q3' }] },
  { id: 4, domainKey: 'domain3', titleKey: 'stepTitle4', questions: [{ kind: 'slider', id: 'q4' }] },
  { id: 5, domainKey: 'domain3', titleKey: 'stepTitle5', questions: [{ kind: 'slider', id: 'q5' }] },
  { id: 6, domainKey: 'domain3', titleKey: 'stepTitle6', questions: [{ kind: 'slider', id: 'q6' }] },
  { id: 7, domainKey: 'domain4', titleKey: 'stepTitle7', questions: [{ kind: 'choice-list', id: 'q7' }] },
  { id: 8, domainKey: 'domain4', titleKey: 'stepTitle8', questions: [{ kind: 'slider', id: 'q8' }] },
  {
    id: 9,
    domainKey: 'domains56',
    titleKey: 'stepTitle9',
    questions: [
      { kind: 'choice-list', id: 'q9', vertical: true },
      { kind: 'choice-list', id: 'q10', vertical: true },
    ],
  },
];

interface MhafProfile {
  primary_stressor: string;
  dominant_emotion: string;
  emotion_intensity: number;
  wellbeing_score: number;
  social_support_score: number;
  coping_style: string;
  core_value: string;
}

function computeSocialSupport(q7: string | null, q8: number): number {
  const base = (q8 - 1) * (10 / 9);
  const factor = q7 === 'Có' || q7 === 'Yes' ? 1 : q7 === 'Không chắc' || q7 === 'Not sure' ? 0.5 : 0;
  return Math.round((base * 0.7 + factor * 10 * 0.3) * 10) / 10;
}

function computeProfile(answers: Record<string, string | number | null>): MhafProfile {
  const emotionLabel = (answers.q2 as string | null) ?? '';
  const wellbeing =
    ((answers.q4 as number | null) ?? 0) +
    ((answers.q5 as number | null) ?? 0) +
    ((answers.q6 as number | null) ?? 0);
  return {
    primary_stressor: (answers.q1 as string | null) ?? '',
    dominant_emotion: emotionLabel,
    emotion_intensity: (answers.q3 as number | null) ?? 0,
    wellbeing_score: wellbeing,
    social_support_score: computeSocialSupport(
      (answers.q7 as string | null) ?? null,
      (answers.q8 as number | null) ?? 5
    ),
    coping_style: (answers.q9 as string | null) ?? '',
    core_value: (answers.q10 as string | null) ?? '',
  };
}

function ProgressBar({ value }: { value: number }) {
  return (
    <div className="w-full h-1.5 bg-surfaceMuted rounded-full overflow-hidden">
      <motion.div
        className="h-full bg-primary rounded-full"
        initial={false}
        animate={{ width: `${value}%` }}
        transition={{ duration: 0.4, ease: 'easeOut' }}
      />
    </div>
  );
}

function QuestionCard({ children }: { children: React.ReactNode }) {
  return (
    <motion.div
      className="bg-surface rounded-3xl p-6 shadow-soft"
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
    >
      {children}
    </motion.div>
  );
}

function SliderQuestion({
  text,
  min,
  max,
  step = 1,
  value,
  onChange,
  leftLabel,
  rightLabel,
}: {
  text: string;
  min: number;
  max: number;
  step?: number;
  value: number;
  onChange: (v: number) => void;
  leftLabel: string;
  rightLabel: string;
}) {
  return (
    <QuestionCard>
      <h2 className="font-medium text-textMain mb-4 leading-snug">{text}</h2>
      <div className="flex items-center justify-between mb-4">
        <span className="text-xs text-textMuted">{leftLabel}</span>
        <span className="text-primary font-semibold text-lg">
          {value}/{max}
        </span>
        <span className="text-xs text-textMuted">{rightLabel}</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full h-2 bg-surfaceMuted rounded-full appearance-none cursor-pointer slider-primary"
      />
    </QuestionCard>
  );
}

function ChoiceGridQuestion({
  text,
  options,
  value,
  onChange,
}: {
  text: string;
  options: string[];
  value: string | null;
  onChange: (v: string) => void;
}) {
  return (
    <QuestionCard>
      <h2 className="font-medium text-textMain mb-4 leading-snug">{text}</h2>
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        {options.map((opt) => {
          const selected = value === opt;
          return (
            <motion.button
              key={opt}
              type="button"
              onClick={() => onChange(opt)}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.97 }}
              className={`w-full px-4 py-3 rounded-2xl border text-sm font-medium transition-colors ${
                selected
                  ? 'bg-primary text-white border-primary shadow-glow'
                  : 'bg-surfaceMuted text-textMain border-transparent hover:border-primary/30'
              }`}
            >
              {opt}
            </motion.button>
          );
        })}
      </div>
    </QuestionCard>
  );
}

function EmotionGridQuestion({
  text,
  emotions,
  value,
  onChange,
}: {
  text: string;
  emotions: EmotionItem[];
  value: string | null;
  onChange: (label: string) => void;
}) {
  return (
    <QuestionCard>
      <h2 className="font-medium text-textMain mb-4 leading-snug">{text}</h2>
      <div className="grid grid-cols-3 gap-4">
        {emotions.map((e) => {
          const selected = value === e.label;
          return (
            <motion.button
              key={e.key}
              type="button"
              onClick={() => onChange(e.label)}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className={`flex flex-col items-center gap-2 p-3 rounded-2xl transition-colors ${
                selected ? 'bg-primary/10' : 'bg-surfaceMuted/60 hover:bg-surfaceMuted'
              }`}
            >
              <span
                className="w-16 h-16 rounded-full flex items-center justify-center text-3xl"
                style={{
                  background: selected
                    ? `linear-gradient(135deg, ${e.color}40, ${e.color}20)`
                    : 'linear-gradient(135deg, rgba(255,255,255,0.9), rgba(255,255,255,0.7))',
                  border: `2px solid ${selected ? e.color : 'rgba(0,0,0,0.08)'}`,
                  boxShadow: selected
                    ? `0 0 20px ${e.color}40, 0 4px 20px rgba(0,0,0,0.08)`
                    : '0 4px 20px rgba(0,0,0,0.06)',
                }}
              >
                {e.emoji}
              </span>
              <span
                className={`text-sm font-medium ${
                  selected ? 'text-textMain' : 'text-textMuted'
                }`}
              >
                {e.label}
              </span>
            </motion.button>
          );
        })}
      </div>
    </QuestionCard>
  );
}

function ChoiceListQuestion({
  text,
  options,
  value,
  onChange,
  vertical = false,
}: {
  text: string;
  options: string[];
  value: string | null;
  onChange: (v: string) => void;
  vertical?: boolean;
}) {
  return (
    <QuestionCard>
      <h2 className="font-medium text-textMain mb-4 leading-snug">{text}</h2>
      <div className={vertical ? 'flex flex-col gap-2' : 'flex flex-wrap gap-2'}>
        {options.map((opt) => {
          const selected = value === opt;
          return (
            <motion.button
              key={opt}
              type="button"
              onClick={() => onChange(opt)}
              whileHover={{ scale: 1.01 }}
              whileTap={{ scale: 0.98 }}
              className={`px-4 py-3 rounded-2xl border text-sm font-medium transition-colors text-left ${
                vertical ? 'w-full' : ''
              } ${
                selected
                  ? 'bg-primary text-white border-primary shadow-glow'
                  : 'bg-surfaceMuted text-textMain border-transparent hover:border-primary/30'
              }`}
            >
              {opt}
            </motion.button>
          );
        })}
      </div>
    </QuestionCard>
  );
}

const SLIDER_CONFIG: Record<
  'q3' | 'q4' | 'q5' | 'q6' | 'q8',
  {
    min: number;
    max: number;
    step: number;
    textKey: 'q3' | 'q4' | 'q5' | 'q6' | 'q8';
    leftKey: 'q3Left' | 'q4Left' | 'q5Left' | 'q6Left' | 'q8Left';
    rightKey: 'q3Right' | 'q4Right' | 'q5Right' | 'q6Right' | 'q8Right';
  }
> = {
  q3: { min: 1, max: 10, step: 1, textKey: 'q3', leftKey: 'q3Left', rightKey: 'q3Right' },
  q4: { min: 0, max: 5, step: 1, textKey: 'q4', leftKey: 'q4Left', rightKey: 'q4Right' },
  q5: { min: 0, max: 5, step: 1, textKey: 'q5', leftKey: 'q5Left', rightKey: 'q5Right' },
  q6: { min: 0, max: 5, step: 1, textKey: 'q6', leftKey: 'q6Left', rightKey: 'q6Right' },
  q8: { min: 1, max: 10, step: 1, textKey: 'q8', leftKey: 'q8Left', rightKey: 'q8Right' },
};

const CHOICE_LIST_CONFIG: Record<
  'q7' | 'q9' | 'q10',
  {
    textKey: 'q7' | 'q9' | 'q10';
    optionsKey: 'q7Options' | 'q9Options' | 'q10Options';
    vertical: boolean;
  }
> = {
  q7: { textKey: 'q7', optionsKey: 'q7Options', vertical: false },
  q9: { textKey: 'q9', optionsKey: 'q9Options', vertical: true },
  q10: { textKey: 'q10', optionsKey: 'q10Options', vertical: true },
};

const EMOTION_COLORS: Record<EmotionKey, string> = {
  anxious: '#C8A87A',
  sad: '#6F86A6',
  stressed: '#B88A7A',
  angry: '#A67F7F',
  lonely: '#9C8AA6',
  normal: '#7AB5AD',
};

const EMOTION_KEYS: EmotionKey[] = ['anxious', 'sad', 'stressed', 'angry', 'lonely', 'normal'];
const EMOTION_EMOJIS: Record<EmotionKey, string> = {
  anxious: '😰',
  sad: '😢',
  stressed: '😫',
  angry: '😠',
  lonely: '🥺',
  normal: '🙂',
};

export default function InitialAssessment() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  const mhaf = t.user.mhaf;
  const { saveMhafProfile } = useUser();

  const [stepIndex, setStepIndex] = useState(0);
  const [answers, setAnswers] = useState<Record<string, string | number | null>>({
    q1: null,
    q2: null,
    q3: 5,
    q4: 3,
    q5: 3,
    q6: 3,
    q7: null,
    q8: 5,
    q9: null,
    q10: null,
  });
  const [isCompleted, setIsCompleted] = useState(false);

  const totalSteps = STEPS.length;
  const currentStep = STEPS[stepIndex];
  const progress = useMemo(() => ((stepIndex + 1) / totalSteps) * 100, [stepIndex, totalSteps]);

  const emotionItems: EmotionItem[] = useMemo(
    () =>
      EMOTION_KEYS.map((k) => ({
        key: k,
        label: mhaf.q2Options[k],
        emoji: EMOTION_EMOJIS[k],
        color: EMOTION_COLORS[k],
      })),
    [mhaf]
  );

  const isStepValid = useMemo(() => {
    return currentStep.questions.every((q) => {
      if (q.kind === 'slider') return true;
      return answers[q.id] != null;
    });
  }, [currentStep, answers]);

  const isLastStep = stepIndex === totalSteps - 1;

  const handleNext = () => {
    if (!isStepValid) return;
    if (isLastStep) {
      handleFinish();
    } else {
      setStepIndex((i) => Math.min(i + 1, totalSteps - 1));
    }
  };

  const handleBack = () => {
    if (stepIndex === 0) return;
    setStepIndex((i) => Math.max(i - 1, 0));
  };

  const handleFinish = () => {
    const profile = computeProfile(answers);
    saveMhafProfile(profile);
    setIsCompleted(true);
    setTimeout(() => {
      navigate('/app');
    }, 3000);
  };

  const updateAnswer = (qid: string, value: string | number | null) => {
    setAnswers((prev) => ({ ...prev, [qid]: value }));
  };

  if (isCompleted) {
  return (
      <div className="min-h-screen bg-background flex items-center justify-center px-4 pb-24 lg:pb-8">
        <motion.div
          className="bg-surface rounded-3xl p-10 shadow-soft text-center max-w-md w-full"
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
          >
            <motion.div
              className="w-20 h-20 mx-auto mb-6 rounded-full bg-primary/10 flex items-center justify-center"
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring', damping: 10 }}
            >
              <Check className="w-10 h-10 text-primary" />
            </motion.div>
          <h2 className="text-xl font-semibold text-textMain mb-2">{mhaf.completedTitle}</h2>
          <p className="text-textMuted">{mhaf.completedDesc}</p>
          <p className="mt-3 text-xs text-amber-700">
            Prototype only: this MHAF result is stored in this browser and is not part of your verified backend profile yet.
          </p>
          <p className="text-textMuted/70 text-xs mt-4">{mhaf.completedRedirecting}</p>
          </motion.div>
              </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-32 lg:pb-12">
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="mb-4 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs leading-relaxed text-amber-800">
          MHAF integration is pending an approved API, database schema, scoring rules, and consent policy. Current answers remain local to this browser and are not used by backend Safety or G4 aggregation.
        </div>
            <motion.div
          className="mb-6"
          initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
        >
          <p className="text-xs uppercase tracking-wider text-primary font-semibold mb-1">
            {mhaf[currentStep.domainKey]}
          </p>
          <h1 className="text-2xl font-semibold text-textMain mb-2">
            {mhaf[currentStep.titleKey]}
          </h1>
          <div className="flex items-center gap-3 mt-3">
            <ProgressBar value={progress} />
            <span className="text-xs text-textMuted whitespace-nowrap">
              {stepIndex + 1}/{totalSteps}
            </span>
              </div>
            </motion.div>

        <AnimatePresence mode="wait">
            <motion.div
            key={currentStep.id}
            initial={{ opacity: 0, x: 24 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -24 }}
            transition={{ duration: 0.3, ease: 'easeOut' }}
            className="space-y-4"
          >
            {currentStep.questions.map((q) => {
              if (q.kind === 'choice-grid' && q.id === 'q1') {
                const opts = [
                  mhaf.q1Options.academic,
                  mhaf.q1Options.work,
                  mhaf.q1Options.family,
                  mhaf.q1Options.relationship,
                  mhaf.q1Options.finance,
                  mhaf.q1Options.health,
                  mhaf.q1Options.other,
                ];
                return (
                  <ChoiceGridQuestion
                    key={q.id}
                    text={mhaf.q1}
                    options={opts}
                    value={(answers[q.id] as string | null) ?? null}
                    onChange={(v) => updateAnswer(q.id, v)}
                  />
                );
              }
              if (q.kind === 'emotion-grid' && q.id === 'q2') {
                return (
                  <EmotionGridQuestion
                    key={q.id}
                    text={mhaf.q2}
                    emotions={emotionItems}
                    value={(answers[q.id] as string | null) ?? null}
                    onChange={(v) => updateAnswer(q.id, v)}
                  />
                );
              }
              if (q.kind === 'slider') {
                const cfg = SLIDER_CONFIG[q.id];
                return (
                  <SliderQuestion
                    key={q.id}
                    text={mhaf[cfg.textKey]}
                    min={cfg.min}
                    max={cfg.max}
                    step={cfg.step}
                    value={(answers[q.id] as number) ?? cfg.min}
                    leftLabel={mhaf[cfg.leftKey]}
                    rightLabel={mhaf[cfg.rightKey]}
                    onChange={(v) => updateAnswer(q.id, v)}
                  />
                );
              }
              if (q.kind === 'choice-list') {
                const cfg = CHOICE_LIST_CONFIG[q.id];
                const optsMap = mhaf[cfg.optionsKey] as Record<string, string>;
                const order = Object.keys(optsMap);
                const opts = order.map((k) => optsMap[k]);
                return (
                  <ChoiceListQuestion
                    key={q.id}
                    text={mhaf[cfg.textKey]}
                    options={opts}
                    vertical={cfg.vertical}
                    value={(answers[q.id] as string | null) ?? null}
                    onChange={(v) => updateAnswer(q.id, v)}
                  />
                );
              }
              return null;
            })}
            </motion.div>
        </AnimatePresence>

            <motion.div
          className="flex items-center justify-between gap-3 mt-8"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
            >
              <motion.button
            type="button"
            onClick={handleBack}
            disabled={stepIndex === 0}
            whileHover={stepIndex > 0 ? { scale: 1.02 } : {}}
            whileTap={stepIndex > 0 ? { scale: 0.98 } : {}}
            className={`inline-flex items-center gap-2 px-5 py-3 rounded-2xl font-medium transition-colors ${
              stepIndex === 0
                ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                : 'bg-surfaceMuted text-textMain hover:bg-surfaceMuted/80'
            }`}
          >
            <ArrowLeft className="w-4 h-4" aria-hidden="true" />
            {mhaf.back}
          </motion.button>

          <motion.button
            type="button"
            onClick={handleNext}
            disabled={!isStepValid}
            whileHover={isStepValid ? { scale: 1.02 } : {}}
            whileTap={isStepValid ? { scale: 0.98 } : {}}
            className={`inline-flex items-center gap-2 px-6 py-3 rounded-2xl font-medium transition-colors ${
              isStepValid
                    ? 'bg-primary text-white hover:bg-primaryDark shadow-glow'
                    : 'bg-gray-200 text-gray-400 cursor-not-allowed'
                }`}
              >
            {isLastStep ? mhaf.finish : mhaf.next}
            <ArrowRight className="w-4 h-4" aria-hidden="true" />
              </motion.button>
            </motion.div>
      </div>

      <style>{`
        .slider-primary::-webkit-slider-thumb {
          appearance: none;
          width: 20px;
          height: 20px;
          background: #5F9E97;
          border-radius: 50%;
          cursor: pointer;
        }
        .slider-primary::-moz-range-thumb {
          width: 20px;
          height: 20px;
          background: #5F9E97;
          border-radius: 50%;
          cursor: pointer;
          border: none;
        }
      `}</style>
    </div>
  );
}
