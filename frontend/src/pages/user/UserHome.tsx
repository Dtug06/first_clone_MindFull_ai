import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { ArrowRight, ClipboardCheck, Heart, MessageCircle, ShieldCheck } from 'lucide-react';
import BreathingOrb from '../../components/ui/BreathingOrb';
import SafetyBadge from '../../components/ui/SafetyBadge';
import { useAuth } from '../../auth/AuthContext';
import type { UserBehaviorProfileResponse } from '../../api/behavior';
import { useLanguage } from '../../i18n';
import { useUser } from '../../contexts/UserContext';

export default function UserHome() {
  const { user, behaviorApi } = useAuth();
  const { t } = useLanguage();
  const { hasCompletedOnboarding } = useUser();
  const [profile, setProfile] = useState<UserBehaviorProfileResponse | null>(null);

  useEffect(() => {
    let cancelled = false;
    behaviorApi.currentProfile()
      .then((result) => {
        if (!cancelled) setProfile(result);
      })
      .catch(() => {
        if (!cancelled) setProfile(null);
      });
    return () => {
      cancelled = true;
    };
  }, [behaviorApi]);

  const greeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return t.user.greetingMorning;
    if (hour < 18) return t.user.greetingAfternoon;
    return t.user.greetingEvening;
  };

  return (
    <div className="min-h-screen bg-background pb-24 lg:pb-12 overflow-x-hidden">
      <div className="w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <motion.div className="mb-8" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <h1 className="text-2xl sm:text-3xl font-semibold text-textMain mb-1">
            {greeting()}, {user?.displayName ?? 'friend'}.
          </h1>
          <p className="text-textMuted">{t.user.checkInPrompt}</p>
        </motion.div>

        {!hasCompletedOnboarding && (
          <motion.div
            className="mb-6 rounded-3xl border border-primary/20 bg-primary/5 p-5"
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <div className="flex items-start gap-4">
              <div className="w-11 h-11 rounded-xl bg-primary/15 flex items-center justify-center flex-shrink-0">
                <ClipboardCheck className="w-5 h-5 text-primary" />
              </div>
              <div className="flex-1">
                <h2 className="font-semibold text-textMain">Initial consultation prototype</h2>
                <p className="mt-1 text-sm text-textMuted">
                  Complete the new MHAF interface. Until a backend contract is approved, this profile remains local to this browser.
                </p>
                <Link to="/app/onboarding" className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-primary">
                  Start consultation <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            </div>
          </motion.div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
          <Link to="/app/daily" className="block bg-gradient-to-br from-primary to-primaryDark rounded-2xl p-5 text-white card-hover">
            <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center mb-3">
              <Heart className="w-6 h-6" />
            </div>
            <h3 className="font-semibold text-lg mb-1">{t.user.quickCheckIn}</h3>
            <p className="text-sm text-white/80">{t.user.quickCheckInDesc}</p>
          </Link>

          <Link to="/app/chat" className="block bg-gradient-to-br from-secondary to-secondary/80 rounded-2xl p-5 text-white card-hover">
            <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center mb-3">
              <MessageCircle className="w-6 h-6" />
            </div>
            <h3 className="font-semibold text-lg mb-1">{t.user.quickCompanion}</h3>
            <p className="text-sm text-white/80">{t.user.quickCompanionDesc}</p>
          </Link>
        </div>

        <motion.div className="bg-surface rounded-3xl p-5 sm:p-6 shadow-soft border border-gray-100 mb-8" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-primary" />
                <h2 className="font-semibold text-textMain">G4 data profile</h2>
              </div>
              {profile ? (
                <p className="mt-2 text-sm text-textMuted">
                  {profile.dataQualityStatus} · {Math.round(profile.dataCoverage * 100)}% coverage · calculated {new Date(profile.calculatedAt).toLocaleDateString()}
                </p>
              ) : (
                <p className="mt-2 text-sm text-textMuted">
                  No aggregated profile yet. MindBridge will not show demo metrics as if they were your data.
                </p>
              )}
            </div>
            <Link to="/app/dashboard" className="inline-flex items-center gap-1 text-sm font-medium text-primary whitespace-nowrap">
              {t.user.dashboard} <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </motion.div>

        <motion.div className="bg-surface rounded-3xl p-5 sm:p-6 shadow-soft border border-gray-100 mb-8" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
          <h2 className="font-semibold text-textMain mb-4">{t.user.breathing}</h2>
          <div className="flex justify-center overflow-hidden">
            <BreathingOrb size="sm" />
          </div>
        </motion.div>

        <div className="flex justify-center">
          <SafetyBadge variant="compact" />
        </div>
      </div>
    </div>
  );
}
