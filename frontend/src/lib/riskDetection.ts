import type { RiskLevel } from '../types/user';

/**
 * Frontend-only, rule-based risk screening for user messages.
 *
 * This is intentionally simple — the *real* safety layer (Clinical Safety
 * Layer in spec section 4.12) belongs on the backend with proper NLP and
 * crisis-trained models. The frontend copy exists so the UI can react
 * visibly when obvious crisis keywords appear, but it must NOT be the only
 * safeguard.
 *
 * Returns:
 *   1 — normal
 *   2 — monitor (sustained distress)
 *   3 — high risk (consider professional help)
 *   4 — emergency (show hotline, stop normal advice)
 */
export function detectRiskLevel(text: string): RiskLevel {
  const normalized = text.toLowerCase().trim();

  // Level 4 — explicit crisis language. Keep bilingual: spec is Vietnamese,
  // but users may also type in English.
  const emergencyPatterns = [
    /\b(tự tử|tu tu|tử tự|tu tu|tu\*|muốn chết|muon chet|không muốn sống|khong muon song|kết thúc|kết thúc cuộc đời|giết mình|giết tôi|nhảy lầu|tự làm đau|làm tổn thương bản thân)\b/i,
    /\b(suicide|kill myself|end my life|want to die|jump off|hang myself|cut myself|hurt myself)\b/i,
  ];

  if (emergencyPatterns.some((p) => p.test(normalized))) {
    return 4;
  }

  // Level 3 — strong hopelessness / self-harm ideation without explicit intent.
  const highRiskPatterns = [
    /\b(vô vọng|không còn ý nghĩa|bỏ cuộc|chán nản quá|tuyệt vọng|trầm cảm nặng|cắt tay|cào tay)\b/i,
    /\b(hopeless|no point|give up|worthless|severe depression|self[- ]?harm)\b/i,
  ];

  if (highRiskPatterns.some((p) => p.test(normalized))) {
    return 3;
  }

  // Level 2 — sustained mild-to-moderate distress.
  const monitorPatterns = [
    /\b(rất mệt|kiệt sức|burnout|lo lắng quá|căng thẳng kéo dài|mất ngủ nhiều|khóc|buồn quá)\b/i,
    /\b(exhausted|burnt out|so anxious|chronic stress|can't sleep|crying|so sad)\b/i,
  ];

  if (monitorPatterns.some((p) => p.test(normalized))) {
    return 2;
  }

  return 1;
}

/**
 * Quick hint used by the chat UI to attach a crisis-resources banner when a
 * user's message is detected at level 3 or 4. Returns null for safe levels.
 */
export function shouldShowCrisisResources(level: RiskLevel): boolean {
  return level >= 3;
}
