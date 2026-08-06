import {
  MoodOption,
  SelfHelpArticle,
  Recommendation,
  RiskCase,
  Expert,
  DashboardMetric,
  OrganizationStats,
} from '../types';

// =============================================================================
// Static (non-translatable) parts of the seed data. Translation is applied at
// render time by mapping each entry's `i18nKey` against `t.data.*`.
// =============================================================================

export const moodOptions: MoodOption[] = [
  { type: 'very_happy',  label: 'Very Happy',    i18nKey: 'veryHappy',   emoji: '✨',  color: '#5F9E97' },
  { type: 'happy',       label: 'Happy',         i18nKey: 'happy',       emoji: '😊',  color: '#7AB5AD' },
  { type: 'neutral',     label: 'Neutral',       i18nKey: 'neutral',     emoji: '😐',  color: '#D8C7A8' },
  { type: 'sad',         label: 'Sad',           i18nKey: 'sad',         emoji: '😢',  color: '#6F86A6' },
  { type: 'anxious',     label: 'Anxious',       i18nKey: 'anxious',     emoji: '😰',  color: '#C8A87A' },
  { type: 'stressed',    label: 'Stressed',      i18nKey: 'stressed',    emoji: '😫',  color: '#B88A7A' },
  { type: 'overwhelmed', label: 'Overwhelmed',   i18nKey: 'overwhelmed', emoji: '😔',  color: '#A67F7F' },
];

// Article `category` matches keys in `t.data.articleCategories`.
// Article title/content resolve via `t.data.articles.{i18nKey}`.
export const selfHelpArticles: SelfHelpArticle[] = [
  {
    id: '1',
    title: '4-7-8 Breathing Technique',
    i18nKey: 'a1',
    category: 'Breathing',
    categoryKey: 'breathing',
    content: 'A calming breathing pattern that helps reduce anxiety and promote sleep.',
    duration: '5 min',
    icon: 'Wind',
  },
  {
    id: '2',
    title: 'Progressive Muscle Relaxation',
    i18nKey: 'a2',
    category: 'Stress Management',
    categoryKey: 'stressManagement',
    content: 'Release physical tension through systematic muscle relaxation exercises.',
    duration: '10 min',
    icon: 'Heart',
  },
  {
    id: '3',
    title: 'Sleep Hygiene Checklist',
    i18nKey: 'a3',
    category: 'Sleep Hygiene',
    categoryKey: 'sleepHygiene',
    content: 'Establish healthy sleep habits for better rest and recovery.',
    duration: '5 min',
    icon: 'Moon',
  },
  {
    id: '4',
    title: 'Gratitude Journaling',
    i18nKey: 'a4',
    category: 'Journaling',
    categoryKey: 'journaling',
    content: 'Practice daily gratitude to shift focus toward positive experiences.',
    duration: '8 min',
    icon: 'BookOpen',
  },
  {
    id: '5',
    title: 'Cognitive Reframing',
    i18nKey: 'a5',
    category: 'CBT-based exercises',
    categoryKey: 'cbt',
    content: 'Learn to identify and challenge negative thought patterns.',
    duration: '12 min',
    icon: 'Brain',
  },
  {
    id: '6',
    title: 'Emotional Regulation Skills',
    i18nKey: 'a6',
    category: 'DBT-based exercises',
    categoryKey: 'dbt',
    content: 'Master techniques for managing intense emotions effectively.',
    duration: '15 min',
    icon: 'Scale',
  },
  {
    id: '7',
    title: 'Active Listening Practice',
    i18nKey: 'a7',
    category: 'Communication Skills',
    categoryKey: 'communication',
    content: 'Improve your communication through mindful listening techniques.',
    duration: '10 min',
    icon: 'MessageCircle',
  },
  {
    id: '8',
    title: 'Box Breathing',
    i18nKey: 'a8',
    category: 'Breathing',
    categoryKey: 'breathing',
    content: 'A simple technique used by Navy SEALs to stay calm under pressure.',
    duration: '4 min',
    icon: 'Wind',
  },
  {
    id: '9',
    title: 'Mindfulness Body Scan',
    i18nKey: 'a9',
    category: 'Stress Management',
    categoryKey: 'stressManagement',
    content: 'Connect with your body through this guided awareness practice.',
    duration: '15 min',
    icon: 'User',
  },
  {
    id: '10',
    title: 'Thought Record Journal',
    i18nKey: 'a10',
    category: 'CBT-based exercises',
    categoryKey: 'cbt',
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
    i18nKey: 'r1',
    type: 'breathing',
    priority: 'high',
    contextualReason: true,
  },
  {
    id: '2',
    title: 'Gratitude Journaling',
    description: 'Taking a few minutes to write down positive moments can shift your perspective.',
    i18nKey: 'r2',
    type: 'journaling',
    priority: 'medium',
    contextualReason: true,
  },
  {
    id: '3',
    title: 'Sleep Quality Improvement',
    description: 'Your sleep data suggests some areas for improvement. Check out our sleep hygiene guide.',
    i18nKey: 'r3',
    type: 'article',
    priority: 'medium',
    contextualReason: true,
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
    reasonKey: 'rc1',
    status: 'monitoring',
    assignedExpert: 'Dr. Minh',
    assignedExpertKey: 'e1',
  },
  {
    id: '2',
    userId: 'user_456',
    anonymousId: 'MB-XXXX-3156',
    riskLevel: 2,
    detectedAt: '2024-01-14T14:22:00Z',
    reason: 'Increased anxiety indicators in recent check-ins',
    reasonKey: 'rc2',
    status: 'new',
  },
  {
    id: '3',
    userId: 'user_789',
    anonymousId: 'MB-XXXX-9042',
    riskLevel: 1,
    detectedAt: '2024-01-13T09:15:00Z',
    reason: 'Minor stress pattern detected, within normal range',
    reasonKey: 'rc3',
    status: 'resolved',
    assignedExpert: 'Dr. Linh',
    assignedExpertKey: 'e2',
  },
];

export const experts: Expert[] = [
  {
    id: '1',
    name: 'Dr. Nguyen Minh',
    nameKey: 'e1',
    email: 'minh@example.com',
    specialty: ['Anxiety', 'Depression', 'CBT'],
    specialtyKeys: ['specialtyAnxiety', 'specialtyDepression', 'specialtyCBT'],
    status: 'available',
    statusKey: 'statusAvailable',
    assignedCases: 8,
  },
  {
    id: '2',
    name: 'Dr. Tran Linh',
    nameKey: 'e2',
    email: 'linh@example.com',
    specialty: ['Stress Management', 'DBT', 'Mindfulness'],
    specialtyKeys: ['specialtyStress', 'specialtyDBT', 'specialtyMindfulness'],
    status: 'busy',
    statusKey: 'statusBusy',
    assignedCases: 15,
  },
  {
    id: '3',
    name: 'Dr. Le Hoang',
    nameKey: 'e3',
    email: 'hoang@example.com',
    specialty: ['Youth Mental Health', 'Career Counseling'],
    specialtyKeys: ['specialtyYouth', 'specialtyCareer'],
    status: 'available',
    statusKey: 'statusAvailable',
    assignedCases: 5,
  },
  {
    id: '4',
    name: 'Dr. Pham Thao',
    nameKey: 'e4',
    email: 'thao@example.com',
    specialty: ['Sleep Disorders', 'Burnout', 'CBT'],
    specialtyKeys: ['specialtySleep', 'specialtyBurnout', 'specialtyCBT'],
    status: 'offline',
    statusKey: 'statusOffline',
    assignedCases: 12,
  },
];

// `metricKey` maps to `t.data.dashboardMetrics.*`.
export const dashboardMetrics: DashboardMetric[] = [
  { label: 'Total Users',     metricKey: 'totalUsers',    value: '12,847', change: '+12%', trend: 'up',   icon: 'Users' },
  { label: 'Check-ins Today', metricKey: 'checkInsToday', value: '2,341',  change: '+8%',  trend: 'up',   icon: 'CheckCircle' },
  { label: 'Avg Mood Score',  metricKey: 'avgMoodScore',  value: '6.8',    change: '+0.3', trend: 'up',   icon: 'TrendingUp' },
  { label: 'Risk Alerts',     metricKey: 'riskAlerts',    value: '23',     change: '-15%', trend: 'down', icon: 'AlertTriangle' },
];

// Topic `i18nKey` resolves through `t.data.orgStats.*`.
export const organizationStats: OrganizationStats = {
  participationRate: 67,
  avgMoodScore: 6.4,
  checkInTrend: [65, 68, 72, 70, 75, 78, 74, 80],
  commonTopics: [
    { topic: 'Academic Pressure',     i18nKey: 'academic', count: 156 },
    { topic: 'Work-Life Balance',     i18nKey: 'workLife', count: 134 },
    { topic: 'Social Relationships',  i18nKey: 'social',   count: 98  },
    { topic: 'Career Uncertainty',    i18nKey: 'career',   count: 87  },
  ],
};

export const suggestedPrompts = [
  'p1',
  'p2',
  'p3',
] as const;

// `day` resolves via `t.data.weekDays[dayKey]`.
export const weekMoodData = [
  { day: 'Mon', dayKey: 'mon', score: 6.5 },
  { day: 'Tue', dayKey: 'tue', score: 7.2 },
  { day: 'Wed', dayKey: 'wed', score: 6.8 },
  { day: 'Thu', dayKey: 'thu', score: 7.5 },
  { day: 'Fri', dayKey: 'fri', score: 6.2 },
  { day: 'Sat', dayKey: 'sat', score: 7.8 },
  { day: 'Sun', dayKey: 'sun', score: 7.4 },
];
