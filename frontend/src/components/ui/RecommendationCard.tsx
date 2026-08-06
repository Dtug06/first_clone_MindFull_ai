import { motion } from 'framer-motion';
import { Heart, BookOpen, Wind, PenTool, Info } from 'lucide-react';
import { Recommendation } from '../../types';
import { useLanguage } from '../../i18n';

interface RecommendationCardProps {
  recommendation: Recommendation;
  /** Optional personalized reason (Explainable AI, spec 4.11). */
  reason?: string | null;
  onClick?: () => void;
}

const typeIcons = {
  breathing: Wind,
  exercise: Heart,
  article: BookOpen,
  journaling: PenTool,
};

const typeColors = {
  breathing: '#5F9E97',
  exercise: '#C8766B',
  article: '#6F86A6',
  journaling: '#D8C7A8',
};

export default function RecommendationCard({ recommendation, reason, onClick }: RecommendationCardProps) {
  const { t } = useLanguage();
  const Icon = typeIcons[recommendation.type];
  const color = typeColors[recommendation.type];
  const recT = t.data.recommendations[recommendation.i18nKey];
  const title = recT.title;
  const description = recT.desc;

  return (
    <motion.div
      className="bg-surface rounded-2xl p-5 border border-gray-100 shadow-soft cursor-pointer"
      onClick={onClick}
      whileHover={{ y: -4, boxShadow: '0 8px 40px rgba(38, 50, 56, 0.1)' }}
      whileTap={{ scale: 0.98 }}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className="flex items-start gap-4">
        <div
          className="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0"
          style={{ backgroundColor: `${color}15` }}
        >
          <Icon className="w-6 h-6" style={{ color }} />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <h4 className="font-medium text-textMain truncate">{title}</h4>
            {recommendation.priority === 'high' && (
              <span className="px-2 py-0.5 bg-softWarning/10 text-softWarning text-xs font-medium rounded-full">
                {t.data.ui.priorityHigh}
              </span>
            )}
          </div>
          <p className="text-sm text-textMuted leading-relaxed">{description}</p>
          {reason && (
            <p className="mt-2 flex items-start gap-1.5 text-xs text-primary leading-relaxed">
              <Info className="w-3.5 h-3.5 flex-shrink-0 mt-0.5" />
              <span>
                <span className="font-medium">{t.user.recommendationReasonPrefix}</span>{' '}
                {reason}
              </span>
            </p>
          )}
        </div>
      </div>
    </motion.div>
  );
}
