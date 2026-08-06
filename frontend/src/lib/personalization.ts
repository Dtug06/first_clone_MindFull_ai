import type { MhafProfile, DailyCheckIn } from '../types/user';

/**
 * Build a short, human-readable "why this recommendation" sentence from
 * the user's MHAF profile and recent daily check-ins.
 *
 * Returns null when there isn't enough data yet (no MHAF + no check-ins),
 * so callers can hide the reason line instead of showing a generic one.
 */
export function buildRecommendationReason(
  recType: 'breathing' | 'journaling' | 'article' | 'exercise',
  profile: MhafProfile | null,
  recentCheckIns: DailyCheckIn[]
): string | null {
  const last7 = recentCheckIns.slice(-7);
  const avgIntensity =
    last7.length > 0
      ? last7.reduce((sum, c) => sum + c.intensity, 0) / last7.length
      : null;
  const avgSleep =
    last7.length > 0
      ? last7.reduce((sum, c) => sum + c.sleep_hours, 0) / last7.length
      : null;
  const lastEmotion = last7.length > 0 ? last7[last7.length - 1].emotion_key : null;

  switch (recType) {
    case 'breathing': {
      if (avgIntensity !== null && avgIntensity >= 6) {
        return `your recent check-ins show intensity around ${avgIntensity.toFixed(1)}/10`;
      }
      if (profile?.emotion_intensity !== undefined && profile.emotion_intensity >= 6) {
        return `your initial profile shows emotional intensity at ${profile.emotion_intensity}/10`;
      }
      if (lastEmotion === 'anxious' || lastEmotion === 'stressed') {
        return `you've been feeling ${lastEmotion} lately`;
      }
      return null;
    }

    case 'journaling': {
      if (profile?.dominant_emotion && profile.dominant_emotion !== 'normal') {
        return `your dominant emotion is "${profile.dominant_emotion}"`;
      }
      if (lastEmotion && lastEmotion !== 'normal') {
        return `your recent mood has been "${lastEmotion}"`;
      }
      return null;
    }

    case 'article': {
      if (avgSleep !== null && avgSleep < 7) {
        return `your average sleep is ${avgSleep.toFixed(1)}h, below the 7h target`;
      }
      if (profile?.wellbeing_score !== undefined && profile.wellbeing_score < 8) {
        return `your well-being score is ${profile.wellbeing_score}/15`;
      }
      return null;
    }

    case 'exercise': {
      if (profile?.coping_style === 'avoidance') {
        return `you tend to avoid problems — gentle movement helps break the cycle`;
      }
      return null;
    }

    default:
      return null;
  }
}

export function avgSleepHours(checkIns: DailyCheckIn[]): number | null {
  const last7 = checkIns.slice(-7);
  if (last7.length === 0) return null;
  return last7.reduce((sum, c) => sum + c.sleep_hours, 0) / last7.length;
}

export function recentIntensityAvg(checkIns: DailyCheckIn[]): number | null {
  const last7 = checkIns.slice(-7);
  if (last7.length === 0) return null;
  return last7.reduce((sum, c) => sum + c.intensity, 0) / last7.length;
}

export function goalsCompletionPercent(
  goals: { progress: number; completed: boolean }[]
): number {
  if (goals.length === 0) return 0;
  const total = goals.reduce((sum, g) => sum + g.progress, 0);
  return Math.round(total / goals.length);
}
