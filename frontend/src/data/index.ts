import { MoodOption, SelfHelpArticle, Recommendation, RiskCase, Expert, DashboardMetric, OrganizationStats } from '../types';

export const moodOptions: MoodOption[] = [
  { type: 'very_happy', label: 'Very Happy', emoji: '✨', color: '#5F9E97' },
  { type: 'happy', label: 'Happy', emoji: '😊', color: '#7AB5AD' },
  { type: 'neutral', label: 'Neutral', emoji: '😐', color: '#D8C7A8' },
  { type: 'sad', label: 'Sad', emoji: '😢', color: '#6F86A6' },
  { type: 'anxious', label: 'Anxious', emoji: '😰', color: '#C8A87A' },
  { type: 'stressed', label: 'Stressed', emoji: '😫', color: '#B88A7A' },
  { type: 'overwhelmed', label: 'Overwhelmed', emoji: '😔', color: '#A67F7F' },
];

export const selfHelpArticles: SelfHelpArticle[] = [
  {
    id: '1',
    title: '4-7-8 Breathing Technique',
    category: 'Breathing',
    content: 'A calming breathing pattern that helps reduce anxiety and promote sleep.',
    duration: '5 min',
    icon: 'Wind',
  },
  {
    id: '2',
    title: 'Progressive Muscle Relaxation',
    category: 'Stress Management',
    content: 'Release physical tension through systematic muscle relaxation exercises.',
    duration: '10 min',
    icon: 'Heart',
  },
  {
    id: '3',
    title: 'Sleep Hygiene Checklist',
    category: 'Sleep Hygiene',
    content: 'Establish healthy sleep habits for better rest and recovery.',
    duration: '5 min',
    icon: 'Moon',
  },
  {
    id: '4',
    title: 'Gratitude Journaling',
    category: 'Journaling',
    content: 'Practice daily gratitude to shift focus toward positive experiences.',
    duration: '8 min',
    icon: 'BookOpen',
  },
  {
    id: '5',
    title: 'Cognitive Reframing',
    category: 'CBT-based exercises',
    content: 'Learn to identify and challenge negative thought patterns.',
    duration: '12 min',
    icon: 'Brain',
  },
  {
    id: '6',
    title: 'Emotional Regulation Skills',
    category: 'DBT-based exercises',
    content: 'Master techniques for managing intense emotions effectively.',
    duration: '15 min',
    icon: 'Scale',
  },
  {
    id: '7',
    title: 'Active Listening Practice',
    category: 'Communication Skills',
    content: 'Improve your communication through mindful listening techniques.',
    duration: '10 min',
    icon: 'MessageCircle',
  },
  {
    id: '8',
    title: 'Box Breathing',
    category: 'Breathing',
    content: 'A simple technique used by Navy SEALs to stay calm under pressure.',
    duration: '4 min',
    icon: 'Wind',
  },
  {
    id: '9',
    title: 'Mindfulness Body Scan',
    category: 'Stress Management',
    content: 'Connect with your body through this guided awareness practice.',
    duration: '15 min',
    icon: 'User',
  },
  {
    id: '10',
    title: 'Thought Record Journal',
    category: 'CBT-based exercises',
    content: 'Track and analyze your thoughts to gain insight into patterns.',
    duration: '10 min',
    icon: 'Edit3',
  },
];

export const recommendations: Recommendation[] = [
  {
    id: '1',
    title: 'Try the 4-7-8 Breathing Exercise',
    description: 'Based on your recent stress levels, this breathing technique can help calm your nervous system.',
    type: 'breathing',
    priority: 'high',
  },
  {
    id: '2',
    title: 'Gratitude Journaling',
    description: 'Taking a few minutes to write down positive moments can shift your perspective.',
    type: 'journaling',
    priority: 'medium',
  },
  {
    id: '3',
    title: 'Sleep Quality Improvement',
    description: 'Your sleep data suggests some areas for improvement. Check out our sleep hygiene guide.',
    type: 'article',
    priority: 'medium',
  },
];

export const riskCases: RiskCase[] = [
  {
    id: '1',
    userId: 'user_123',
    anonymousId: 'MB-XXXX-7821',
    riskLevel: 3,
    detectedAt: '2024-01-15T08:30:00Z',
    reason: 'Sustained low mood scores over 7 days with declining engagement',
    status: 'monitoring',
    assignedExpert: 'Dr. Minh',
  },
  {
    id: '2',
    userId: 'user_456',
    anonymousId: 'MB-XXXX-3156',
    riskLevel: 2,
    detectedAt: '2024-01-14T14:22:00Z',
    reason: 'Increased anxiety indicators in recent check-ins',
    status: 'new',
  },
  {
    id: '3',
    userId: 'user_789',
    anonymousId: 'MB-XXXX-9042',
    riskLevel: 1,
    detectedAt: '2024-01-13T09:15:00Z',
    reason: 'Minor stress pattern detected, within normal range',
    status: 'resolved',
    assignedExpert: 'Dr. Linh',
  },
];

export const experts: Expert[] = [
  {
    id: '1',
    name: 'Dr. Nguyen Minh',
    email: 'minh@example.com',
    specialty: ['Anxiety', 'Depression', 'CBT'],
    status: 'available',
    assignedCases: 8,
  },
  {
    id: '2',
    name: 'Dr. Tran Linh',
    email: 'linh@example.com',
    specialty: ['Stress Management', 'DBT', 'Mindfulness'],
    status: 'busy',
    assignedCases: 15,
  },
  {
    id: '3',
    name: 'Dr. Le Hoang',
    email: 'hoang@example.com',
    specialty: ['Youth Mental Health', 'Career Counseling'],
    status: 'available',
    assignedCases: 5,
  },
  {
    id: '4',
    name: 'Dr. Pham Thao',
    email: 'thao@example.com',
    specialty: ['Sleep Disorders', 'Burnout', 'CBT'],
    status: 'offline',
    assignedCases: 12,
  },
];

export const dashboardMetrics: DashboardMetric[] = [
  { label: 'Total Users', value: '12,847', change: '+12%', trend: 'up', icon: 'Users' },
  { label: 'Check-ins Today', value: '2,341', change: '+8%', trend: 'up', icon: 'CheckCircle' },
  { label: 'Avg Mood Score', value: '6.8', change: '+0.3', trend: 'up', icon: 'TrendingUp' },
  { label: 'Risk Alerts', value: '23', change: '-15%', trend: 'down', icon: 'AlertTriangle' },
];

export const organizationStats: OrganizationStats = {
  participationRate: 67,
  avgMoodScore: 6.4,
  checkInTrend: [65, 68, 72, 70, 75, 78, 74, 80],
  commonTopics: [
    { topic: 'Academic Pressure', count: 156 },
    { topic: 'Work-Life Balance', count: 134 },
    { topic: 'Social Relationships', count: 98 },
    { topic: 'Career Uncertainty', count: 87 },
  ],
};

export const suggestedPrompts = [
  'I feel overwhelmed today',
  'Help me reflect on my mood',
  'Guide me through a breathing exercise',
];

export const weekMoodData = [
  { day: 'Mon', score: 6.5 },
  { day: 'Tue', score: 7.2 },
  { day: 'Wed', score: 6.8 },
  { day: 'Thu', score: 7.5 },
  { day: 'Fri', score: 6.2 },
  { day: 'Sat', score: 7.8 },
  { day: 'Sun', score: 7.4 },
];
