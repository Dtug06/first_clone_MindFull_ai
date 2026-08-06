import { useCallback, useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Activity, BarChart2, Info, Moon, RefreshCw, ShieldCheck, TrendingUp } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../api/client';
import type { UserBehaviorProfileResponse } from '../../api/behavior';
import { useLanguage } from '../../i18n';

function formatScore(value: number | null, digits = 2): string {
  return value === null ? '—' : value.toFixed(digits);
}
function formatPercent(value: number): string {
  return `${Math.round(value * 100)}%`;
}

export default function Dashboard() {
  const { t } = useLanguage();
  const { behaviorApi, primeLastRequestId } = useAuth();
  const [profile, setProfile] = useState<UserBehaviorProfileResponse | null>(null);
  const [state, setState] = useState<'loading' | 'ready' | 'empty' | 'error'>('loading');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setState('loading');
    setError(null);
    primeLastRequestId(null);
    try {
      const result = await behaviorApi.currentProfile();
      setProfile(result);
      setState('ready');
    } catch (e) {
      if (e instanceof ApiError) {
        primeLastRequestId(e.requestId);
        if (e.status === 404) {
          setState('empty');
          return;
        }
        setError(e.message);
      } else {
        setError(e instanceof Error ? e.message : 'Unexpected error');
      }
      setState('error');
    }
  }, [behaviorApi, primeLastRequestId]);

  useEffect(() => {
    void load();
  }, [load]);

  const metrics = useMemo(() => profile ? [
    { label: 'Mood average (7 days)', value: formatScore(profile.moodAvg7d), icon: TrendingUp, color: '#5F9E97' },
    { label: 'Sleep average (7 days)', value: profile.sleepAvg7d === null ? '—' : `${profile.sleepAvg7d.toFixed(1)}h`, icon: Moon, color: '#6F86A6' },
    { label: 'Stress average (7 days)', value: formatScore(profile.stressAvg7d), icon: Activity, color: '#D8C7A8' },
    { label: 'Engagement (7 days)', value: profile.engagementScore7d === null ? '—' : `${profile.engagementScore7d}/3`, icon: BarChart2, color: '#5F9E97' },
  ] : [], [profile]);

  return (
    <div className="min-h-screen bg-background pb-24 lg:pb-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <motion.div className="mb-8" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <h1 className="text-2xl font-semibold text-textMain mb-1">{t.user.dashboardTitle}</h1>
          <p className="text-textMuted">{t.data.ui.dashboardSubtitle}</p>
        </motion.div>

        {state === 'loading' && (
          <div className="rounded-3xl bg-surface p-10 shadow-soft text-center text-textMuted">
            Loading your verified G4 profile…
          </div>
        )}

        {state === 'error' && (
          <div role="alert" className="rounded-3xl border border-red-100 bg-red-50 p-6 text-red-700">
            <p>{error ?? 'Could not load your behavior profile.'}</p>
            <button type="button" onClick={() => void load()} className="mt-3 inline-flex items-center gap-2 text-sm font-medium underline">
              <RefreshCw className="w-4 h-4" /> Retry
            </button>
          </div>
        )}

        {state === 'empty' && (
          <div className="rounded-3xl bg-surface p-8 shadow-soft border border-gray-100">
            <div className="flex items-start gap-4">
              <div className="w-11 h-11 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
                <Info className="w-5 h-5 text-primary" />
              </div>
              <div>
                <h2 className="font-semibold text-textMain">Your profile is not available yet</h2>
                <p className="mt-2 text-sm leading-relaxed text-textMuted">
                  Complete Daily Check-ins and use Chat normally. The G4 aggregation job must run before this dashboard has enough verified data.
                </p>
                <p className="mt-2 text-xs text-textMuted">
                  MindBridge does not substitute demo numbers while your real profile is unavailable.
                </p>
              </div>
            </div>
          </div>
        )}

        {state === 'ready' && profile && (
          <>
            <div className="grid grid-cols-2 gap-4 mb-6">
              {metrics.map((metric, index) => (
                <motion.div
                  key={metric.label}
                  className="bg-surface rounded-2xl p-4 shadow-soft border border-gray-100"
                  initial={{ opacity: 0, y: 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                >
                  <div className="flex items-center gap-2 mb-2">
                    <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ backgroundColor: `${metric.color}20` }}>
                      <metric.icon className="w-4 h-4" style={{ color: metric.color }} />
                    </div>
                    <span className="text-sm text-textMuted">{metric.label}</span>
                  </div>
                  <div className="text-2xl font-semibold text-textMain">{metric.value}</div>
                </motion.div>
              ))}
            </div>

            <motion.div className="bg-surface rounded-3xl p-6 shadow-soft border border-gray-100 mb-6" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
              <div className="flex items-center justify-between gap-4 mb-5">
                <div>
                  <h2 className="font-semibold text-textMain">Data quality</h2>
                  <p className="text-sm text-textMuted">Calculated {new Date(profile.calculatedAt).toLocaleString()}</p>
                </div>
                <span className={`px-3 py-1 rounded-full text-xs font-semibold ${
                  profile.dataQualityStatus === 'SUFFICIENT'
                    ? 'bg-green-50 text-green-700'
                    : profile.dataQualityStatus === 'LOW'
                      ? 'bg-amber-50 text-amber-700'
                      : 'bg-gray-100 text-gray-600'
                }`}>
                  {profile.dataQualityStatus}
                </span>
              </div>
              <div className="grid sm:grid-cols-3 gap-4">
                <Quality label="Coverage" value={formatPercent(profile.dataCoverage)} />
                <Quality label="Confidence" value={formatPercent(profile.confidence)} />
                <Quality label="Current risk state" value={profile.riskLevel === null ? '—' : `L${profile.riskLevel}`} />
              </div>
            </motion.div>

            <motion.div className="bg-gradient-to-br from-primary/10 to-secondary/10 rounded-3xl p-6 mb-6" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
              <div className="flex items-start gap-4">
                <div className="w-10 h-10 rounded-xl bg-primary/20 flex items-center justify-center flex-shrink-0">
                  <ShieldCheck className="w-5 h-5 text-primary" />
                </div>
                <div>
                  <h3 className="font-medium text-textMain mb-1">Verified profile snapshot</h3>
                  <p className="text-sm text-textMuted leading-relaxed">
                    Window ending {profile.windowEnd}. Values shown here come from the backend G4 aggregation pipeline, not frontend mock data.
                  </p>
                </div>
              </div>
            </motion.div>

            <div className="grid md:grid-cols-2 gap-6">
              <section className="bg-surface rounded-3xl p-6 shadow-soft border border-gray-100">
                <h2 className="font-semibold text-textMain mb-4">Dominant topics (7 days)</h2>
                {profile.dominantTopics7d.length === 0 ? (
                  <p className="text-sm text-textMuted">No authoritative structured chat analysis is available.</p>
                ) : (
                  <div className="space-y-3">
                    {profile.dominantTopics7d.map((topic) => (
                      <div key={topic.topic} className="flex items-center justify-between text-sm">
                        <span className="text-textMain">{topic.topic}</span>
                        <span className="text-textMuted">{Math.round(topic.share * 100)}%</span>
                      </div>
                    ))}
                  </div>
                )}
              </section>

              <section className="bg-surface rounded-3xl p-6 shadow-soft border border-gray-100">
                <h2 className="font-semibold text-textMain mb-4">Trend summary</h2>
                {profile.trendSummary.entries.length === 0 ? (
                  <p className="text-sm text-textMuted">Not enough data to calculate trends.</p>
                ) : (
                  <div className="space-y-3">
                    {profile.trendSummary.entries.map((entry) => (
                      <div key={entry.featureCode} className="flex items-center justify-between gap-3 text-sm">
                        <span className="text-textMain">{entry.featureCode}</span>
                        <span className="text-textMuted">{entry.direction}</span>
                      </div>
                    ))}
                  </div>
                )}
              </section>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function Quality({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-surfaceMuted/60 p-4">
      <div className="text-xs text-textMuted">{label}</div>
      <div className="mt-1 text-xl font-semibold text-textMain">{value}</div>
    </div>
  );
}
