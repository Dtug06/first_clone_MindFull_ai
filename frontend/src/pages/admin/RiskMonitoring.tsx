import { motion } from 'framer-motion';
import RiskLevelBadge from '../../components/ui/RiskLevelBadge';
import { riskCases } from '../../data';
import { Clock, UserCheck, AlertTriangle } from 'lucide-react';
import { RiskLevel } from '../../types';

const statusColors = {
  new: 'bg-primary/10 text-primary',
  monitoring: 'bg-accent/20 text-accent',
  resolved: 'bg-primary/20 text-primaryDark',
  escalated: 'bg-softWarning/20 text-softWarning',
};

export default function RiskMonitoring() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-semibold text-textMain">Risk Monitoring</h1>
        <p className="text-textMuted">Monitor and manage user risk cases.</p>
      </motion.div>

      {/* Risk level legend */}
      <motion.div
        className="bg-surface rounded-2xl p-4 shadow-soft"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <div className="flex flex-wrap gap-4">
          {[
            { level: 1 as RiskLevel, label: 'Normal', color: '#5F9E97' },
            { level: 2 as RiskLevel, label: 'Monitoring', color: '#D8C7A8' },
            { level: 3 as RiskLevel, label: 'High Risk', color: '#C8766B' },
            { level: 4 as RiskLevel, label: 'Emergency', color: '#B85A50' },
          ].map((item) => (
            <div key={item.level} className="flex items-center gap-2">
              <div 
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: item.color }}
              />
              <span className="text-sm text-textMuted">{item.label}</span>
            </div>
          ))}
        </div>
      </motion.div>

      {/* Cases list */}
      <div className="space-y-4">
        {riskCases.map((caseItem, index) => (
          <motion.div
            key={caseItem.id}
            className="bg-surface rounded-2xl p-5 shadow-soft border border-gray-100"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.15 + index * 0.05 }}
          >
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <span className="text-sm font-medium text-textMain">{caseItem.anonymousId}</span>
                  <RiskLevelBadge level={caseItem.riskLevel} />
                  <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${statusColors[caseItem.status]}`}>
                    {caseItem.status.charAt(0).toUpperCase() + caseItem.status.slice(1)}
                  </span>
                </div>
                <p className="text-sm text-textMuted mb-2">{caseItem.reason}</p>
                <div className="flex items-center gap-4 text-xs text-textMuted">
                  <span className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    {new Date(caseItem.detectedAt).toLocaleDateString()}
                  </span>
                  {caseItem.assignedExpert && (
                    <span className="flex items-center gap-1">
                      <UserCheck className="w-3 h-3" />
                      {caseItem.assignedExpert}
                    </span>
                  )}
                </div>
              </div>
              <div className="flex gap-2">
                {!caseItem.assignedExpert && (
                  <motion.button
                    className="px-4 py-2 bg-primary text-white rounded-xl text-sm font-medium"
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                  >
                    Assign Expert
                  </motion.button>
                )}
                <motion.button
                  className="px-4 py-2 bg-surfaceMuted text-textMain rounded-xl text-sm font-medium hover:bg-gray-200 transition-colors"
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                >
                  View Details
                </motion.button>
              </div>
            </div>
          </motion.div>
        ))}
      </div>

      {/* High risk alert summary */}
      <motion.div
        className="bg-gradient-to-br from-softWarning/10 to-softWarning/5 rounded-2xl p-6 border border-softWarning/20"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
      >
        <div className="flex items-center gap-3 mb-4">
          <AlertTriangle className="w-5 h-5 text-softWarning" />
          <h3 className="font-semibold text-textMain">High Risk Summary</h3>
        </div>
        <div className="grid grid-cols-3 gap-4">
          <div>
            <div className="text-2xl font-semibold text-softWarning">23</div>
            <div className="text-sm text-textMuted">Active cases</div>
          </div>
          <div>
            <div className="text-2xl font-semibold text-softWarning">8</div>
            <div className="text-sm text-textMuted">Pending assignment</div>
          </div>
          <div>
            <div className="text-2xl font-semibold text-softWarning">5</div>
            <div className="text-sm text-textMuted">Escalated today</div>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
