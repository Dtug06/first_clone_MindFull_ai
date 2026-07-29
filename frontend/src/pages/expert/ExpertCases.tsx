import { motion } from 'framer-motion';
import RiskLevelBadge from '../../components/ui/RiskLevelBadge';
import { riskCases } from '../../data';
import { Clock, MessageSquare, FileText, ArrowRight } from 'lucide-react';

export default function ExpertCases() {
  const activeCases = riskCases.filter(c => c.status !== 'resolved');

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-semibold text-textMain">Assigned Cases</h1>
        <p className="text-textMuted">Manage your assigned user cases.</p>
      </motion.div>

      {/* Cases list */}
      <div className="space-y-4">
        {activeCases.map((caseItem, index) => (
          <motion.div
            key={caseItem.id}
            className="bg-surface rounded-2xl p-5 shadow-soft border border-gray-100 cursor-pointer hover:shadow-soft-lg transition-all"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + index * 0.05 }}
            whileHover={{ y: -2 }}
          >
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <span className="text-sm font-medium text-textMain">{caseItem.anonymousId}</span>
                  <RiskLevelBadge level={caseItem.riskLevel} />
                </div>
                <p className="text-sm text-textMuted mb-3">{caseItem.reason}</p>
                <div className="flex flex-wrap items-center gap-4 text-xs text-textMuted">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    Detected: {new Date(caseItem.detectedAt).toLocaleDateString()}
                  </span>
                  <span className="flex items-center gap-1">
                    <FileText className="w-3 h-3" />
                    3 check-ins recorded
                  </span>
                  <span className="flex items-center gap-1">
                    <MessageSquare className="w-3 h-3" />
                    2 notes
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button className="px-4 py-2 bg-primary text-white rounded-xl text-sm font-medium">
                  Open Case
                </button>
                <ArrowRight className="w-5 h-5 text-textMuted" />
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {activeCases.length === 0 && (
        <motion.div
          className="bg-surface rounded-2xl p-8 text-center shadow-soft"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
        >
          <p className="text-textMuted">No active cases assigned to you.</p>
        </motion.div>
      )}
    </div>
  );
}
