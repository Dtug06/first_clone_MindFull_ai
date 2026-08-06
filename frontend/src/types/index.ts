export interface User {
  id: string;
  name: string;
  email: string;
  avatar?: string;
  role: 'user' | 'expert' | 'admin';
  createdAt: string;
}

export interface MoodCheckIn {
  id: string;
  userId: string;
  mood: MoodType;
  stressLevel: number;
  sleepQuality: number;
  energyLevel: number;
  note?: string;
  createdAt: string;
}

export type MoodType = 
  | 'very_happy' 
  | 'happy' 
  | 'neutral' 
  | 'sad' 
  | 'anxious' 
  | 'stressed' 
  | 'overwhelmed';

export interface MoodOption {
  type: MoodType;
  label: string;
  i18nKey: 'veryHappy' | 'happy' | 'neutral' | 'sad' | 'anxious' | 'stressed' | 'overwhelmed';
  emoji: string;
  color: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

export type ArticleCategoryKey =
  | 'breathing'
  | 'stressManagement'
  | 'sleepHygiene'
  | 'journaling'
  | 'cbt'
  | 'dbt'
  | 'communication';

export interface SelfHelpArticle {
  id: string;
  title: string;
  i18nKey: 'a1' | 'a2' | 'a3' | 'a4' | 'a5' | 'a6' | 'a7' | 'a8' | 'a9' | 'a10';
  category: string;
  categoryKey: ArticleCategoryKey;
  content: string;
  duration: string;
  icon: string;
}

export interface Recommendation {
  id: string;
  title: string;
  description: string;
  i18nKey: 'r1' | 'r2' | 'r3';
  type: 'exercise' | 'article' | 'breathing' | 'journaling';
  priority: 'high' | 'medium' | 'low';
  contextualReason?: boolean;
}

export interface RiskCase {
  id: string;
  userId: string;
  anonymousId: string;
  riskLevel: RiskLevel;
  detectedAt: string;
  reason: string;
  reasonKey: 'rc1' | 'rc2' | 'rc3';
  status: 'new' | 'monitoring' | 'resolved' | 'escalated';
  assignedExpert?: string;
  assignedExpertKey?: 'e1' | 'e2' | 'e3' | 'e4';
}

export type RiskLevel = 1 | 2 | 3 | 4;

export type ExpertStatusKey = 'statusAvailable' | 'statusBusy' | 'statusOffline';
export type SpecialtyKey =
  | 'specialtyAnxiety'
  | 'specialtyDepression'
  | 'specialtyCBT'
  | 'specialtyStress'
  | 'specialtyDBT'
  | 'specialtyMindfulness'
  | 'specialtyYouth'
  | 'specialtyCareer'
  | 'specialtySleep'
  | 'specialtyBurnout';

export interface Expert {
  id: string;
  name: string;
  nameKey: 'e1' | 'e2' | 'e3' | 'e4';
  email: string;
  specialty: string[];
  specialtyKeys: SpecialtyKey[];
  status: 'available' | 'busy' | 'offline';
  statusKey: ExpertStatusKey;
  assignedCases: number;
  avatar?: string;
}

export interface DashboardMetric {
  label: string;
  metricKey: 'totalUsers' | 'checkInsToday' | 'avgMoodScore' | 'riskAlerts';
  value: string | number;
  change?: string;
  trend?: 'up' | 'down' | 'stable';
  icon: string;
}

export interface OrganizationStats {
  participationRate: number;
  avgMoodScore: number;
  checkInTrend: number[];
  commonTopics: { topic: string; i18nKey: 'academic' | 'workLife' | 'social' | 'career'; count: number }[];
}

// --- G3-T13: Expert Review ---
export type SafetyEventStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'DISMISSED';

export type ExpertReviewDecision =
  | 'CONFIRM_RISK'
  | 'DOWNGRADE_RISK'
  | 'ESCALATE'
  | 'NO_ACTION'
  | 'CONTINUE_MONITORING'
  | 'REQUEST_FOLLOWUP'
  | 'DISMISS';

export interface SafetyEventSource {
  id: string;
  sourceType: 'CHAT_ANALYSIS' | 'DAILY_ANSWER' | 'EXERCISE_SUBMISSION' | 'PROGRAM_ASSESSMENT';
  sourceId: string | null;
  createdAt: string;
}

export interface SafetyAction {
  id: string;
  actionType: 'SHOW_TEMPLATE' | 'BLOCK_MATCHING' | 'FLAG_REVIEW' | 'PAUSE_PROGRAM';
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED';
  errorMessage: string | null;
  executedAt: string | null;
  createdAt: string;
}

export interface ExpertReviewResponse {
  id: string;
  safetyEventId: string;
  reviewerId: string;
  reviewerDisplayName: string;
  decision: ExpertReviewDecision;
  note: string | null;
  createdAt: string;
}

export interface SafetyEventDetail {
  id: string;
  userId: string;
  riskLevel: RiskLevel;
  status: SafetyEventStatus;
  summary: string | null;
  createdAt: string;
  resolvedAt: string | null;
  sources: SafetyEventSource[];
  actions: SafetyAction[];
  reviews: ExpertReviewResponse[];
}

export interface SafetyEventSummary {
  id: string;
  userId: string;
  riskLevel: RiskLevel;
  status: SafetyEventStatus;
  summary: string | null;
  createdAt: string;
  resolvedAt: string | null;
  reviewCount: number;
}
