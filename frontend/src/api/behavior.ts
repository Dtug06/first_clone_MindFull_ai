import { ApiClient } from './client';

export type DataQualityStatus = 'SUFFICIENT' | 'LOW' | 'INSUFFICIENT';

export interface TopicFrequency {
  topic: string;
  frequency: number;
  share: number;
}
export interface TrendEntry {
  featureCode: string;
  direction: 'UP' | 'DOWN' | 'STABLE' | 'UNKNOWN';
  deltaPct: number | null;
  reason: string;
  recentAvg: number | null;
  priorAvg: number | null;
  recentCoverage: number | null;
  priorCoverage: number | null;
}

export interface TrendSummary {
  userId: string;
  targetDate: string;
  zoneId: string | null;
  entries: TrendEntry[];
  streak: {
    checkInStreak: number;
    highStressStreak: number;
    lastCheckInDate: string | null;
    lastHighStressDate: string | null;
    streakWindowDays: number;
  } | null;
  dataQuality: string;
  calculationVersion: string;
}

export interface UserBehaviorProfileResponse {
  profileVersion: string;
  windowEnd: string;
  stressAvg7d: number | null;
  stressAvg30d: number | null;
  moodAvg7d: number | null;
  moodAvg30d: number | null;
  energyAvg7d: number | null;
  energyAvg30d: number | null;
  sleepAvg7d: number | null;
  sleepAvg30d: number | null;
  anxietyAvg7d: number | null;
  anxietyAvg30d: number | null;
  engagementScore7d: number | null;
  engagementScore30d: number | null;
  riskLevel: number | null;
  dominantTopics7d: TopicFrequency[];
  dominantTopics30d: TopicFrequency[];
  trendSummary: TrendSummary;
  dataCoverage: number;
  confidence: number;
  dataQualityStatus: DataQualityStatus;
  calculatedAt: string;
}

export class BehaviorApi {
  constructor(private readonly client: ApiClient) {}

  currentProfile(): Promise<UserBehaviorProfileResponse> {
    return this.client.request<UserBehaviorProfileResponse>('/behavior/profile', {
      method: 'GET',
    });
  }
}
