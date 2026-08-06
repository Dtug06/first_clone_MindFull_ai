import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import RiskLevelBadge from '../../components/ui/RiskLevelBadge';
import { Clock, MessageSquare, FileText, ArrowRight, AlertTriangle, Loader2 } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';
import type { SafetyEventSummary, SafetyEventStatus } from '../../types';
import { useNavigate } from 'react-router-dom';

const STATUS_LABELS: Record<SafetyEventStatus, string> = {
  OPEN: 'Open',
  UNDER_REVIEW: 'Under Review',
  RESOLVED: 'Resolved',
  DISMISSED: 'Dismissed',
};

export default function ExpertCases() {
  const { expertReviewApi } = useAuth();
  const navigate = useNavigate();

  const [events, setEvents] = useState<SafetyEventSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    expertReviewApi.listEvents({ size: 100 })
      .then(res => setEvents(res.content))
      .catch(() => setError('Failed to load cases. Please try again.'))
      .finally(() => setLoading(false));
  }, [expertReviewApi]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-red-500">{error}</p>
      </div>
    );
  }

  const activeEvents = events.filter(e => e.status !== 'RESOLVED' && e.status !== 'DISMISSED');

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-semibold text-textMain">Assigned Cases</h1>
        <p className="text-textMuted">Safety events requiring expert review.</p>
      </motion.div>

      {/* Stats bar */}
      <div className="flex flex-wrap gap-4 text-sm">
        <div className="flex items-center gap-2 px-3 py-1.5 bg-red-50 rounded-lg">
          <AlertTriangle className="w-4 h-4 text-red-500" />
          <span className="font-medium text-red-700">
            {events.filter(e => e.riskLevel === 4).length} critical
          </span>
        </div>
        <div className="flex items-center gap-2 px-3 py-1.5 bg-orange-50 rounded-lg">
          <span className="font-medium text-orange-700">
            {events.filter(e => e.status === 'OPEN').length} open
          </span>
        </div>
        <div className="flex items-center gap-2 px-3 py-1.5 bg-blue-50 rounded-lg">
          <span className="font-medium text-blue-700">
            {events.filter(e => e.status === 'UNDER_REVIEW').length} under review
          </span>
        </div>
      </div>

      {/* Cases list */}
      <div className="space-y-4">
        {activeEvents.map((event, index) => (
          <motion.div
            key={event.id}
            className="bg-surface rounded-2xl p-5 shadow-soft border border-gray-100 cursor-pointer hover:shadow-soft-lg transition-all"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + index * 0.05 }}
            whileHover={{ y: -2 }}
            onClick={() => navigate(`/expert/cases/${event.id}`)}
          >
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <span className="text-sm font-medium text-textMain">
                    Event #{event.id.slice(0, 8)}
                  </span>
                  <RiskLevelBadge level={event.riskLevel} />
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                    event.status === 'OPEN'
                      ? 'bg-orange-100 text-orange-700'
                      : 'bg-blue-100 text-blue-700'
                  }`}>
                    {STATUS_LABELS[event.status]}
                  </span>
                </div>
                <p className="text-sm text-textMuted mb-3 line-clamp-2">
                  {event.summary ?? 'No summary available'}
                </p>
                <div className="flex flex-wrap items-center gap-4 text-xs text-textMuted">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    Detected: {new Date(event.createdAt).toLocaleDateString()}
                  </span>
                  <span className="flex items-center gap-1">
                    <FileText className="w-3 h-3" />
                    {event.reviewCount} {event.reviewCount === 1 ? 'review' : 'reviews'}
                  </span>
                  {event.resolvedAt && (
                    <span className="flex items-center gap-1">
                      <MessageSquare className="w-3 h-3" />
                      Resolved: {new Date(event.resolvedAt).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  className="px-4 py-2 bg-primary text-white rounded-xl text-sm font-medium"
                  onClick={e => { e.stopPropagation(); navigate(`/expert/cases/${event.id}`); }}
                >
                  Open Case
                </button>
                <ArrowRight className="w-5 h-5 text-textMuted" />
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {activeEvents.length === 0 && (
        <motion.div
          className="bg-surface rounded-2xl p-8 text-center shadow-soft"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
        >
          <AlertTriangle className="w-10 h-10 mx-auto mb-3 text-green-400" />
          <p className="text-textMuted">No active cases. All caught up!</p>
        </motion.div>
      )}
    </div>
  );
}
