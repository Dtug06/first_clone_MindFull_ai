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
  emoji: string;
  color: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
}

export interface SelfHelpArticle {
  id: string;
  title: string;
  category: string;
  content: string;
  duration: string;
  icon: string;
}

export interface Recommendation {
  id: string;
  title: string;
  description: string;
  type: 'exercise' | 'article' | 'breathing' | 'journaling';
  priority: 'high' | 'medium' | 'low';
}

export interface RiskCase {
  id: string;
  userId: string;
  anonymousId: string;
  riskLevel: RiskLevel;
  detectedAt: string;
  reason: string;
  status: 'new' | 'monitoring' | 'resolved' | 'escalated';
  assignedExpert?: string;
}

export type RiskLevel = 1 | 2 | 3 | 4;

export interface Expert {
  id: string;
  name: string;
  email: string;
  specialty: string[];
  status: 'available' | 'busy' | 'offline';
  assignedCases: number;
  avatar?: string;
}

export interface DashboardMetric {
  label: string;
  value: string | number;
  change?: string;
  trend?: 'up' | 'down' | 'stable';
  icon: string;
}

export interface OrganizationStats {
  participationRate: number;
  avgMoodScore: number;
  checkInTrend: number[];
  commonTopics: { topic: string; count: number }[];
}
