import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Trash2, AlertTriangle, RefreshCw, Plus, Target, LogOut, ShieldCheck } from 'lucide-react';
import { useLanguage } from '../../i18n';
import { useUser } from '../../contexts/UserContext';
import { useAuth } from '../../auth/AuthContext';
import type { ConsentType, CurrentConsentResponse } from '../../api/consents';
import { clearChatSessionId } from '../../lib/accountStorage';

export default function UserSettings() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  const { user, logout, consentsApi } = useAuth();
  const {
    mhafProfile,
    resetMhafProfile,
    clearAllData,
    goals,
    addGoal,
    updateGoalProgress,
    removeGoal,
  } = useUser();

  const [goalTitle, setGoalTitle] = useState('');
  const [goalDeadline, setGoalDeadline] = useState('');
  const [confirmClear, setConfirmClear] = useState(false);
  const [confirmRedo, setConfirmRedo] = useState(false);
  const [consents, setConsents] = useState<CurrentConsentResponse[]>([]);
  const [consentLoading, setConsentLoading] = useState(true);
  const [consentUpdating, setConsentUpdating] = useState<ConsentType | null>(null);
  const [consentError, setConsentError] = useState<string | null>(null);

  const loadConsents = useCallback(async () => {
    setConsentLoading(true);
    setConsentError(null);
    try {
      setConsents(await consentsApi.current());
    } catch {
      setConsentError('Could not load your server-side consent settings.');
    } finally {
      setConsentLoading(false);
    }
  }, [consentsApi]);

  useEffect(() => {
    void loadConsents();
  }, [loadConsents]);

  const isGranted = (type: ConsentType): boolean =>
    consents.find((item) => item.consentType === type)?.granted ?? false;

  const updateConsent = async (type: ConsentType, granted: boolean) => {
    setConsentUpdating(type);
    setConsentError(null);
    try {
      await consentsApi.record({
        consentType: type,
        action: granted ? 'GRANTED' : 'REVOKED',
        policyVersion: '1.0',
      });
      await loadConsents();
    } catch {
      setConsentError('The consent change could not be saved. Please try again.');
    } finally {
      setConsentUpdating(null);
    }
  };

  const handleAddGoal = () => {
    const title = goalTitle.trim();
    if (!title) return;
    addGoal({
      title,
      deadline: goalDeadline || undefined,
    });
    setGoalTitle('');
    setGoalDeadline('');
  };

  const handleRedoOnboarding = () => {
    resetMhafProfile();
    setConfirmRedo(false);
    navigate('/app/onboarding');
  };

  const handleClearAll = () => {
    clearAllData();
    setConfirmClear(false);
    navigate('/app/onboarding');
  };

  const handleSignOut = () => {
    clearChatSessionId(user?.id ?? null);
    logout();
    navigate('/auth', { replace: true });
  };

  const formatDate = (iso: string): string => {
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  };

  return (
    <div className="min-h-screen bg-background pb-32 lg:pb-12">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <motion.div
          className="mb-8"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="text-2xl font-semibold text-textMain mb-1">{t.user.settingsTitle}</h1>
          <p className="text-textMuted">{t.user.settingsSubtitle}</p>
        </motion.div>

        {/* Profile section */}
        <motion.section
          className="bg-surface rounded-3xl p-6 shadow-soft mb-6"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <h2 className="font-semibold text-textMain mb-4">{t.user.settingsProfileSection}</h2>
          {mhafProfile ? (
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-3 text-sm">
              <div>
                <dt className="text-textMuted">{t.user.settingsStressor}</dt>
                <dd className="text-textMain font-medium">{mhafProfile.primary_stressor || '—'}</dd>
              </div>
              <div>
                <dt className="text-textMuted">{t.user.settingsDominantEmotion}</dt>
                <dd className="text-textMain font-medium">{mhafProfile.dominant_emotion || '—'}</dd>
              </div>
              <div>
                <dt className="text-textMuted">{t.user.settingsIntensity}</dt>
                <dd className="text-textMain font-medium">{mhafProfile.emotion_intensity}/10</dd>
              </div>
              <div>
                <dt className="text-textMuted">{t.user.settingsWellbeing}</dt>
                <dd className="text-textMain font-medium">{mhafProfile.wellbeing_score}/15</dd>
              </div>
              <div>
                <dt className="text-textMuted">{t.user.settingsSocialSupport}</dt>
                <dd className="text-textMain font-medium">{mhafProfile.social_support_score}/10</dd>
              </div>
              <div>
                <dt className="text-textMuted">{t.user.settingsCopingStyle}</dt>
                <dd className="text-textMain font-medium">{mhafProfile.coping_style || '—'}</dd>
              </div>
              <div>
                <dt className="text-textMuted">{t.user.settingsCoreValue}</dt>
                <dd className="text-textMain font-medium">{mhafProfile.core_value || '—'}</dd>
              </div>
              <div>
                <dt className="text-textMuted">{t.user.settingsCompletedAt}</dt>
                <dd className="text-textMain font-medium">{formatDate(mhafProfile.completed_at)}</dd>
              </div>
            </dl>
          ) : (
            <p className="text-sm text-textMuted">—</p>
          )}

          <div className="mt-6 pt-6 border-t border-gray-100">
            {!confirmRedo ? (
              <button
                type="button"
                onClick={() => setConfirmRedo(true)}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-full border border-primary/30 text-primary text-sm font-medium hover:bg-primary/5"
              >
                <RefreshCw className="w-4 h-4" />
                {t.user.settingsRedoOnboarding}
              </button>
            ) : (
              <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4">
                <div className="flex items-start gap-3">
                  <AlertTriangle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <p className="text-sm font-medium text-amber-900 mb-1">
                      {t.user.settingsRedoOnboardingConfirm}
                    </p>
                    <p className="text-xs text-amber-700 mb-3">{t.user.settingsRedoOnboardingHint}</p>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={handleRedoOnboarding}
                        className="px-3 py-1.5 bg-amber-600 text-white rounded-full text-xs font-medium"
                      >
                        OK
                      </button>
                      <button
                        type="button"
                        onClick={() => setConfirmRedo(false)}
                        className="px-3 py-1.5 border border-gray-300 text-textMuted rounded-full text-xs font-medium"
                      >
                        {t.common.cancel}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </motion.section>

        {/* Goals section */}
        <motion.section
          className="bg-surface rounded-3xl p-6 shadow-soft mb-6"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
        >
          <div className="flex items-center gap-2 mb-4">
            <Target className="w-5 h-5 text-primary" />
            <h2 className="font-semibold text-textMain">{t.user.settingsGoalsSection}</h2>
          </div>

          <div className="flex flex-col sm:flex-row gap-2 mb-4">
            <input
              type="text"
              value={goalTitle}
              onChange={(e) => setGoalTitle(e.target.value)}
              placeholder={t.user.settingsGoalPlaceholder}
              className="flex-1 px-4 py-2.5 rounded-full bg-surfaceMuted/60 border border-transparent focus:border-primary/30 focus:outline-none text-sm"
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleAddGoal();
              }}
            />
            <input
              type="date"
              value={goalDeadline}
              onChange={(e) => setGoalDeadline(e.target.value)}
              className="px-4 py-2.5 rounded-full bg-surfaceMuted/60 border border-transparent focus:border-primary/30 focus:outline-none text-sm"
              title={t.user.settingsGoalDeadline}
            />
            <button
              type="button"
              onClick={handleAddGoal}
              disabled={!goalTitle.trim()}
              className="inline-flex items-center justify-center gap-1 px-4 py-2.5 rounded-full bg-primary text-white text-sm font-medium disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              <Plus className="w-4 h-4" />
              {t.user.settingsGoalAddButton}
            </button>
          </div>

          {goals.length === 0 ? (
            <p className="text-sm text-textMuted italic py-4 text-center">
              {t.user.settingsGoalEmpty}
            </p>
          ) : (
            <ul className="space-y-3">
              {goals.map((goal) => (
                <li
                  key={goal.id}
                  className={`p-4 rounded-2xl border ${
                    goal.completed
                      ? 'bg-primary/5 border-primary/20'
                      : 'bg-surfaceMuted/40 border-transparent'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3 mb-2">
                    <div className="flex-1 min-w-0">
                      <p
                        className={`font-medium text-sm ${
                          goal.completed
                            ? 'text-textMuted line-through'
                            : 'text-textMain'
                        }`}
                      >
                        {goal.title}
                      </p>
                      {goal.deadline && (
                        <p className="text-xs text-textMuted mt-0.5">{goal.deadline}</p>
                      )}
                    </div>
                    <button
                      type="button"
                      onClick={() => removeGoal(goal.id)}
                      className="p-1.5 text-textMuted hover:text-red-500 hover:bg-red-50 rounded-full"
                      title={t.user.settingsGoalRemove}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                  <div className="flex items-center gap-3">
                    <input
                      type="range"
                      min={0}
                      max={100}
                      step={5}
                      value={goal.progress}
                      onChange={(e) => updateGoalProgress(goal.id, Number(e.target.value))}
                      className="flex-1 h-1.5 bg-surfaceMuted rounded-full appearance-none cursor-pointer slider-primary"
                    />
                    <span className="text-xs font-medium text-primary min-w-[3rem] text-right">
                      {goal.progress}%
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </motion.section>

        {/* AI data consent */}
        <motion.section
          className="bg-surface rounded-3xl p-6 shadow-soft mb-6"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <div className="flex items-center gap-2 mb-2">
            <ShieldCheck className="w-5 h-5 text-primary" />
            <h2 className="font-semibold text-textMain">AI data permissions</h2>
          </div>
          <p className="text-sm text-textMuted mb-5">
            These choices are stored as append-only consent events. Revoking personalization stops
            new AI replies from receiving your name, typed Daily Check-in values, and G4 trend summary.
          </p>

          {consentLoading ? (
            <p className="text-sm text-textMuted">Loading permissions…</p>
          ) : (
            <div className="space-y-4">
              {([
                {
                  type: 'CHAT_ANALYSIS' as const,
                  title: 'Chat analysis and AI replies',
                  description: 'Allows safety-screened chat messages to be analyzed and sent to the configured AI provider.',
                },
                {
                  type: 'PERSONALIZATION' as const,
                  title: 'Personalized chat context',
                  description: 'Allows chat to use your display name, today’s typed Daily Check-in values, and available G4 trends.',
                },
              ]).map((item) => {
                const granted = isGranted(item.type);
                return (
                  <div key={item.type} className="flex flex-col sm:flex-row sm:items-center gap-3 rounded-2xl bg-surfaceMuted/40 p-4">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-textMain">{item.title}</p>
                      <p className="text-xs leading-relaxed text-textMuted mt-1">{item.description}</p>
                    </div>
                    <button
                      type="button"
                      disabled={consentUpdating !== null}
                      onClick={() => void updateConsent(item.type, !granted)}
                      className={`px-4 py-2 rounded-full text-sm font-medium disabled:opacity-50 ${
                        granted
                          ? 'border border-red-200 text-red-600 hover:bg-red-50'
                          : 'bg-primary text-white hover:bg-primary/90'
                      }`}
                    >
                      {consentUpdating === item.type
                        ? 'Saving…'
                        : granted
                          ? 'Revoke'
                          : 'Allow'}
                    </button>
                  </div>
                );
              })}
            </div>
          )}

          {consentError && (
            <div className="mt-4 flex items-center justify-between gap-3 rounded-2xl bg-red-50 p-3 text-sm text-red-700">
              <span>{consentError}</span>
              <button type="button" onClick={() => void loadConsents()} className="font-medium underline">
                Retry
              </button>
            </div>
          )}
        </motion.section>

        {/* Data & Privacy */}
        <motion.section
          className="bg-surface rounded-3xl p-6 shadow-soft"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
        >
          <h2 className="font-semibold text-textMain mb-4">{t.user.settingsDataSection}</h2>

          <p className="mb-4 text-xs leading-relaxed text-textMuted">
            The controls below currently clear only the MHAF prototype and goals stored in this browser.
            They do not delete server-side chat, consent, Daily Check-in, or G4 profile data.
          </p>

          {!confirmClear ? (
            <button
              type="button"
              onClick={() => setConfirmClear(true)}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-full border border-red-300 text-red-600 text-sm font-medium hover:bg-red-50"
            >
              <Trash2 className="w-4 h-4" />
              {t.user.settingsClearData}
            </button>
          ) : (
            <div className="bg-red-50 border border-red-200 rounded-2xl p-4">
              <div className="flex items-start gap-3">
                <AlertTriangle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <p className="text-sm font-medium text-red-900 mb-1">
                    {t.user.settingsClearDataConfirm}
                  </p>
                  <p className="text-xs text-red-700 mb-3">{t.user.settingsClearDataHint}</p>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={handleClearAll}
                      className="px-3 py-1.5 bg-red-600 text-white rounded-full text-xs font-medium"
                    >
                      {t.common.delete}
                    </button>
                    <button
                      type="button"
                      onClick={() => setConfirmClear(false)}
                      className="px-3 py-1.5 border border-gray-300 text-textMuted rounded-full text-xs font-medium"
                    >
                      {t.common.cancel}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          <div className="mt-6 pt-6 border-t border-gray-100">
            <button
              type="button"
              onClick={handleSignOut}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-full border border-gray-200 text-textMuted text-sm font-medium hover:bg-gray-50 hover:text-red-600"
            >
              <LogOut className="w-4 h-4" />
              {t.common.signOut}
            </button>
          </div>
        </motion.section>
      </div>
    </div>
  );
}
