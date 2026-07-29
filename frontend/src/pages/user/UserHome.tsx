import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import BreathingOrb from '../../components/ui/BreathingOrb';
import RecommendationCard from '../../components/ui/RecommendationCard';
import SoftLineChart from '../../components/ui/SoftLineChart';
import SafetyBadge from '../../components/ui/SafetyBadge';
import { recommendations, weekMoodData } from '../../data';
import { Heart, MessageCircle, ArrowRight, Sparkles } from 'lucide-react';

export default function UserHome() {
  const greeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  };

  return (
    <div className="min-h-screen bg-background pb-24 lg:pb-32 overflow-x-hidden w-full">
      <div className="w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {/* Header */}
        <motion.div
          className="mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="text-2xl sm:text-3xl font-semibold text-textMain mb-1">
            {greeting()}, friend.
          </h1>
          <p className="text-textMuted">Take a moment to check in with yourself.</p>
        </motion.div>

        {/* Quick actions */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <Link
              to="/app/check-in"
              className="block bg-gradient-to-br from-primary to-primaryDark rounded-2xl p-5 text-white card-hover"
            >
              <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center mb-3">
                <Heart className="w-6 h-6" />
              </div>
              <h3 className="font-semibold text-lg mb-1">Daily Check-in</h3>
              <p className="text-sm text-white/80">How are you feeling today?</p>
            </Link>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 }}
          >
            <Link
              to="/app/chat"
              className="block bg-gradient-to-br from-secondary to-secondary/80 rounded-2xl p-5 text-white card-hover"
            >
              <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center mb-3">
                <MessageCircle className="w-6 h-6" />
              </div>
              <h3 className="font-semibold text-lg mb-1">AI Companion</h3>
              <p className="text-sm text-white/80">Talk to your gentle guide</p>
            </Link>
          </motion.div>
        </div>

        {/* Breathing orb widget */}
        <motion.div
          className="bg-surface rounded-3xl p-4 sm:p-6 shadow-soft border border-gray-100 mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-textMain">Take a breath</h2>
            <Sparkles className="w-5 h-5 text-accent" />
          </div>
          <div className="flex justify-center overflow-hidden">
            <BreathingOrb size="sm" />
          </div>
        </motion.div>

        {/* Today's recommendation */}
        <motion.div
          className="mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.25 }}
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-semibold text-textMain">For you today</h2>
            <Link to="/app/library" className="text-sm text-primary flex items-center gap-1 flex-shrink-0 ml-2">
              View all <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
          <div className="space-y-3">
            {recommendations.slice(0, 2).map((rec) => (
              <RecommendationCard key={rec.id} recommendation={rec} />
            ))}
          </div>
        </motion.div>

        {/* Weekly mood trend */}
        <motion.div
          className="bg-surface rounded-3xl p-4 sm:p-6 shadow-soft border border-gray-100 mb-8"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <div className="flex items-center justify-between mb-4 gap-2">
            <h2 className="font-semibold text-textMain">Your week</h2>
            <Link to="/app/dashboard" className="text-sm text-primary flex items-center gap-1 flex-shrink-0">
              See details <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
          <div className="w-full overflow-hidden">
            <SoftLineChart data={weekMoodData} height={180} />
          </div>
          <div className="flex flex-wrap justify-between mt-4 text-sm gap-2">
            <div className="text-center flex-1 min-w-[80px]">
              <div className="text-lg font-semibold text-primary">7.2</div>
              <div className="text-textMuted">Avg mood</div>
            </div>
            <div className="text-center flex-1 min-w-[80px]">
              <div className="text-lg font-semibold text-primary">6</div>
              <div className="text-textMuted">Check-ins</div>
            </div>
            <div className="text-center flex-1 min-w-[80px]">
              <div className="text-lg font-semibold text-primary">+0.3</div>
              <div className="text-textMuted">vs last week</div>
            </div>
          </div>
        </motion.div>

        {/* Self-help quick access */}
        <motion.div
          className="bg-surface rounded-3xl p-4 sm:p-6 shadow-soft border border-gray-100"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35 }}
        >
          <div className="flex items-center justify-between mb-4 gap-2">
            <h2 className="font-semibold text-textMain">Quick resources</h2>
            <Link to="/app/library" className="text-sm text-primary flex items-center gap-1 flex-shrink-0">
              Browse all <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
          <div className="grid grid-cols-3 gap-2 sm:gap-3">
            {[
              { icon: '🧘', label: 'Breathing' },
              { icon: '📖', label: 'Journaling' },
              { icon: '😴', label: 'Sleep' },
            ].map((item) => (
              <button
                key={item.label}
                className="flex flex-col items-center gap-2 p-3 sm:p-4 bg-surfaceMuted rounded-2xl hover:bg-primary/5 transition-colors"
              >
                <span className="text-2xl">{item.icon}</span>
                <span className="text-xs sm:text-sm text-textMuted text-center">{item.label}</span>
              </button>
            ))}
          </div>
        </motion.div>

        {/* Safety badges */}
        <motion.div
          className="mt-8 flex flex-wrap gap-3 justify-center"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.4 }}
        >
          <SafetyBadge variant="compact" />
        </motion.div>
      </div>
    </div>
  );
}