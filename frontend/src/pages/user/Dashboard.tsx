import { motion } from 'framer-motion';
import SoftLineChart from '../../components/ui/SoftLineChart';
import RecommendationCard from '../../components/ui/RecommendationCard';
import { recommendations, weekMoodData } from '../../data';
import { TrendingUp, Moon, Activity, Target, Info } from 'lucide-react';

export default function Dashboard() {
  return (
    <div className="min-h-screen bg-background pb-24 lg:pb-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Header */}
        <motion.div
          className="mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="text-2xl font-semibold text-textMain mb-1">
            Your wellness dashboard
          </h1>
          <p className="text-textMuted">Track your progress and gain insights over time.</p>
        </motion.div>

        {/* Stats overview */}
        <div className="grid grid-cols-2 gap-4 mb-6">
          {[
            { label: 'Avg Mood', value: '7.2', icon: TrendingUp, color: '#5F9E97' },
            { label: 'Sleep Quality', value: '6.8', icon: Moon, color: '#6F86A6' },
            { label: 'Stress Level', value: '4.2', icon: Activity, color: '#D8C7A8' },
            { label: 'Goals Progress', value: '72%', icon: Target, color: '#5F9E97' },
          ].map((stat, i) => (
            <motion.div
              key={i}
              className="bg-surface rounded-2xl p-4 shadow-soft border border-gray-100"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 + i * 0.05 }}
            >
              <div className="flex items-center gap-2 mb-2">
                <div 
                  className="w-8 h-8 rounded-lg flex items-center justify-center"
                  style={{ backgroundColor: `${stat.color}15` }}
                >
                  <stat.icon className="w-4 h-4" style={{ color: stat.color }} />
                </div>
                <span className="text-sm text-textMuted">{stat.label}</span>
              </div>
              <div className="text-2xl font-semibold text-textMain">{stat.value}</div>
            </motion.div>
          ))}
        </div>

        {/* Mood trend chart */}
        <motion.div
          className="bg-surface rounded-3xl p-6 shadow-soft border border-gray-100 mb-6"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <h2 className="font-semibold text-textMain mb-4">Mood trend</h2>
          <SoftLineChart data={weekMoodData} height={200} />
          <div className="mt-4 flex items-center justify-between text-sm">
            <span className="text-textMuted">Last 7 days</span>
            <span className="text-primary font-medium">+0.3 vs previous week</span>
          </div>
        </motion.div>

        {/* Check-in frequency */}
        <motion.div
          className="bg-surface rounded-3xl p-6 shadow-soft border border-gray-100 mb-6"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
        >
          <h2 className="font-semibold text-textMain mb-4">Check-in frequency</h2>
          <div className="flex items-end gap-2 h-20">
            {[1, 1, 0, 1, 1, 1, 1].map((checked, i) => (
              <div key={i} className="flex-1 flex flex-col items-center gap-2">
                <div 
                  className={`w-full rounded-t transition-colors ${checked ? 'bg-primary' : 'bg-gray-200'}`}
                  style={{ height: checked ? `${60 + Math.random() * 30}%` : '10%' }}
                />
                <span className="text-xs text-textMuted">
                  {['M', 'T', 'W', 'T', 'F', 'S', 'S'][i]}
                </span>
              </div>
            ))}
          </div>
          <div className="mt-4 text-center text-sm text-textMuted">
            6 check-ins this week
          </div>
        </motion.div>

        {/* Personalized insight */}
        <motion.div
          className="bg-gradient-to-br from-primary/10 to-secondary/10 rounded-3xl p-6 mb-6"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <div className="flex items-start gap-4">
            <div className="w-10 h-10 rounded-xl bg-primary/20 flex items-center justify-center flex-shrink-0">
              <Info className="w-5 h-5 text-primary" />
            </div>
            <div>
              <h3 className="font-medium text-textMain mb-1">Personalized insight</h3>
              <p className="text-sm text-textMuted leading-relaxed">
                Based on your recent check-ins, your mood tends to improve on weekends. 
                This week, consider planning more relaxing activities to maintain this positive trend.
              </p>
            </div>
          </div>
        </motion.div>

        {/* Recommendations */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35 }}
        >
          <h2 className="font-semibold text-textMain mb-4">Recommendations for you</h2>
          <div className="space-y-3">
            {recommendations.slice(0, 2).map((rec) => (
              <RecommendationCard key={rec.id} recommendation={rec} />
            ))}
          </div>
        </motion.div>
      </div>
    </div>
  );
}
