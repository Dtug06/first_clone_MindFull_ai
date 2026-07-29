import { motion } from 'framer-motion';
import DashboardMetricCard from '../../components/ui/DashboardMetricCard';
import SoftLineChart from '../../components/ui/SoftLineChart';
import { dashboardMetrics, weekMoodData } from '../../data';
import { TrendingUp, TrendingDown, Users, Activity } from 'lucide-react';

export default function AdminOverview() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-semibold text-textMain">Dashboard Overview</h1>
        <p className="text-textMuted">Monitor user wellness and platform health.</p>
      </motion.div>

      {/* Metrics grid */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {dashboardMetrics.map((metric, i) => (
          <DashboardMetricCard
            key={i}
            label={metric.label}
            value={metric.value}
            change={metric.change}
            trend={metric.trend}
            icon={metric.icon}
            delay={i}
          />
        ))}
      </div>

      {/* Charts row */}
      <div className="grid lg:grid-cols-2 gap-6">
        {/* Mood trend */}
        <motion.div
          className="bg-surface rounded-2xl p-6 shadow-soft"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <h2 className="font-semibold text-textMain mb-4">Platform Mood Trend</h2>
          <SoftLineChart data={weekMoodData} height={200} />
        </motion.div>

        {/* Risk breakdown */}
        <motion.div
          className="bg-surface rounded-2xl p-6 shadow-soft"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.5 }}
        >
          <h2 className="font-semibold text-textMain mb-4">Risk Level Breakdown</h2>
          <div className="space-y-4">
            {[
              { level: 'Normal', count: 12400, percentage: 96.5, color: '#5F9E97' },
              { level: 'Monitoring', count: 380, percentage: 3, color: '#D8C7A8' },
              { level: 'High Risk', count: 52, percentage: 0.4, color: '#C8766B' },
              { level: 'Emergency', count: 15, percentage: 0.1, color: '#B85A50' },
            ].map((risk, i) => (
              <div key={i} className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-textMain">{risk.level}</span>
                  <span className="text-textMuted">{risk.count} ({risk.percentage}%)</span>
                </div>
                <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className="h-full rounded-full transition-all duration-500"
                    style={{ width: `${risk.percentage * 10}%`, backgroundColor: risk.color }}
                  />
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Additional stats */}
      <div className="grid lg:grid-cols-3 gap-6">
        {/* Expert referrals */}
        <motion.div
          className="bg-surface rounded-2xl p-6 shadow-soft"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.6 }}
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-textMain">Expert Referrals</h2>
            <Users className="w-5 h-5 text-primary" />
          </div>
          <div className="text-3xl font-semibold text-textMain mb-2">127</div>
          <div className="flex items-center gap-1 text-sm text-primary">
            <TrendingUp className="w-4 h-4" />
            <span>+12% from last week</span>
          </div>
        </motion.div>

        {/* Content usage */}
        <motion.div
          className="bg-surface rounded-2xl p-6 shadow-soft"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.65 }}
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-textMain">Content Usage</h2>
            <Activity className="w-5 h-5 text-secondary" />
          </div>
          <div className="text-3xl font-semibold text-textMain mb-2">8,432</div>
          <div className="flex items-center gap-1 text-sm text-primary">
            <TrendingUp className="w-4 h-4" />
            <span>+23% from last week</span>
          </div>
        </motion.div>

        {/* Active users */}
        <motion.div
          className="bg-surface rounded-2xl p-6 shadow-soft"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-textMain">Active Today</h2>
            <Users className="w-5 h-5 text-accent" />
          </div>
          <div className="text-3xl font-semibold text-textMain mb-2">3,241</div>
          <div className="flex items-center gap-1 text-sm text-softWarning">
            <TrendingDown className="w-4 h-4" />
            <span>-5% from yesterday</span>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
