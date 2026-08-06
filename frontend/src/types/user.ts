export type EmotionKey = 'anxious' | 'sad' | 'stressed' | 'angry' | 'lonely' | 'normal';

export interface MhafProfile {
  primary_stressor: string;
  dominant_emotion: string;
  emotion_intensity: number;
  wellbeing_score: number;
  social_support_score: number;
  coping_style: string;
  core_value: string;
  completed_at: string;
}

export interface DailyCheckIn {
  date: string;
  emotion: string;
  emotion_key: EmotionKey;
  intensity: number;
  sleep_hours: number;
  note?: string;
}

export interface UserGoal {
  id: string;
  title: string;
  deadline?: string;
  progress: number;
  completed: boolean;
  created_at: string;
}

export type RiskLevel = 1 | 2 | 3 | 4;

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  risk_level?: RiskLevel;
}
