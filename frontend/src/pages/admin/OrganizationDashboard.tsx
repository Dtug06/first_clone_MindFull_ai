import { motion } from 'framer-motion';
import { organizationStats } from '../../data';
import SoftLineChart from '../../components/ui/SoftLineChart';
import { Building2, Users, TrendingUp, Shield } from 'lucide-react';

export default function OrganizationDashboard() {
  const miniChartData = organizationStats.checkInTrend.map((value, i) => ({
    day: ['W1', 'W2', 'W3', 'W4', 'W5', 'W6', 'W7', 'W8'][i],
    score: value,
  }));

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-semibold text-textMain">Organization Dashboard</h1>
        <p className="text-textMuted">Anonymous aggregated statistics for universities, companies, and organizations.</p>
      </motion.div>

      {/* Privacy notice */}
      <motion.div
        className="bg-gradient-to-br from-secondary/10 to-secondary/5 rounded-2xl p-6 border border-secondary/20"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <div className="flex items-center gap-3">
          <Shield className="w-6 h-6 text-secondary" />
          <div>
            <h3 className="font-semibold text-textMain">Privacy-First Design</h3>
            <p className="text-sm text-textMuted">
              All data shown is anonymized and aggregated. Individual user identities are never exposed.
            </p>
          </div>
        </div>
      </motion.div>

      {/* Overview stats */}
      <div className="grid sm:grid-cols-3 gap-4">
        {[
          { label: 'Participation Rate', value: `${organizationStats.participationRate}%`, icon: Users, color: '#5F9E97' },
          { label: 'Average Mood Score', value: organizationStats.avgMoodScore, icon: TrendingUp, color: '#6F86A6' },
          { label: 'Organizations', value: '24', icon: Building2, color: '#D8C7A8' },
        ].map((stat, i) => (
          <motion.div
            key={i}
            className="bg-surface rounded-2xl p-5 shadow-soft"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 + i * 0.05 }}
          >
            <div className="flex items-center gap-3 mb-3">
              <div 
                className="w-10 h-10 rounded-xl flex items-center justify-center"
                style={{ backgroundColor: `${stat.color}15` }}
              >
                <stat.icon className="w-5 h-5" style={{ color: stat.color }} />
              </div>
              <span className="text-sm text-textMuted">{stat.label}</span>
            </div>
            <div className="text-2xl font-semibold text-textMain">{stat.value}</div>
          </motion.div>
        ))}
      </div>

      {/* Check-in trend */}
      <motion.div
        className="bg-surface rounded-2xl p-6 shadow-soft"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
      >
        <h2 className="font-semibold text-textMain mb-4">Weekly Check-in Trend</h2>
        <SoftLineChart data={miniChartData} height={200} />
      </motion.div>

      {/* Common topics */}
      <motion.div
        className="bg-surface rounded-2xl p-6 shadow-soft"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.35 }}
      >
        <h2 className="font-semibold text-textMain mb-4">Common Stress Topics</h2>
        <div className="space-y-4">
          {organizationStats.commonTopics.map((topic, i) => (
            <div key={i} className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-textMain">{topic.topic}</span>
                <span className="text-textMuted">{topic.count} mentions</span>
              </div>
              <div className="h-2 bg-surfaceMuted rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full bg-primary transition-all duration-500"
                  style={{ width: `${(topic.count / 200) * 100}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </motion.div>

      {/* Resource recommendations */}
      <motion.div
        className="bg-surface rounded-2xl p-6 shadow-soft"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
      >
        <h2 className="font-semibold text-textMain mb-4">Recommended Resources</h2>
        <div className="grid sm:grid-cols-2 gap-4">
          {[
            { title: 'Academic Pressure Workshop', usage: 234 },
            { title: 'Work-Life Balance Guide', usage: 189 },
            { title: 'Mindfulness for Beginners', usage: 156 },
            { title: 'Sleep Quality Improvement', usage: 142 },
          ].map((resource, i) => (
            <div key={i} className="p-4 bg-surfaceMuted rounded-xl">
              <div className="font-medium text-textMain mb-1">{resource.title}</div>
              <div className="text-sm text-textMuted">{resource.usage} views</div>
            </div>
          ))}
        </div>
      </motion.div>
    </div>
  );
}
