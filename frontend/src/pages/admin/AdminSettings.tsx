import { motion } from 'framer-motion';
import { Settings, Shield, Clock, FileText } from 'lucide-react';

export default function AdminSettings() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-semibold text-textMain">Settings & Logs</h1>
        <p className="text-textMuted">System configuration and audit logs.</p>
      </motion.div>

      {/* Quick settings */}
      <div className="grid md:grid-cols-2 gap-6">
        {/* Security settings */}
        <motion.div
          className="bg-surface rounded-2xl p-6 shadow-soft"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <div className="flex items-center gap-3 mb-4">
            <Shield className="w-5 h-5 text-primary" />
            <h2 className="font-semibold text-textMain">Security Settings</h2>
          </div>
          <div className="space-y-4">
            {[
              { label: 'Two-factor authentication', enabled: true },
              { label: 'Session timeout (30 min)', enabled: true },
              { label: 'Login attempt limits', enabled: true },
              { label: 'Audit logging', enabled: true },
            ].map((setting, i) => (
              <div key={i} className="flex items-center justify-between">
                <span className="text-textMuted">{setting.label}</span>
                <div className={`w-10 h-6 rounded-full relative cursor-pointer transition-colors ${
                  setting.enabled ? 'bg-primary' : 'bg-gray-200'
                }`}>
                  <div className={`absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-transform ${
                    setting.enabled ? 'translate-x-5' : 'translate-x-1'
                  }`} />
                </div>
              </div>
            ))}
          </div>
        </motion.div>

        {/* Privacy settings */}
        <motion.div
          className="bg-surface rounded-2xl p-6 shadow-soft"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
        >
          <div className="flex items-center gap-3 mb-4">
            <Settings className="w-5 h-5 text-secondary" />
            <h2 className="font-semibold text-textMain">Privacy Settings</h2>
          </div>
          <div className="space-y-4">
            {[
              { label: 'Data retention (90 days)', enabled: true },
              { label: 'Anonymous analytics', enabled: true },
              { label: 'Data export API', enabled: false },
              { label: 'User data deletion', enabled: true },
            ].map((setting, i) => (
              <div key={i} className="flex items-center justify-between">
                <span className="text-textMuted">{setting.label}</span>
                <div className={`w-10 h-6 rounded-full relative cursor-pointer transition-colors ${
                  setting.enabled ? 'bg-primary' : 'bg-gray-200'
                }`}>
                  <div className={`absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-transform ${
                    setting.enabled ? 'translate-x-5' : 'translate-x-1'
                  }`} />
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Audit logs */}
      <motion.div
        className="bg-surface rounded-2xl shadow-soft overflow-hidden"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
      >
        <div className="p-6 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <FileText className="w-5 h-5 text-textMuted" />
            <h2 className="font-semibold text-textMain">Recent Activity Logs</h2>
          </div>
        </div>
        <div className="divide-y divide-gray-100">
          {[
            { action: 'User risk case escalated', user: 'Admin - Minh', time: '2 min ago', type: 'risk' },
            { action: 'Expert account created', user: 'Admin - Linh', time: '15 min ago', type: 'user' },
            { action: 'Content approved', user: 'Admin - Hoang', time: '1 hour ago', type: 'content' },
            { action: 'System settings updated', user: 'Admin - Minh', time: '2 hours ago', type: 'system' },
            { action: 'User data export requested', user: 'User - nva@email.com', time: '3 hours ago', type: 'privacy' },
          ].map((log, i) => (
            <div key={i} className="px-6 py-4 flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className={`w-2 h-2 rounded-full ${
                  log.type === 'risk' ? 'bg-softWarning' :
                  log.type === 'user' ? 'bg-primary' :
                  log.type === 'content' ? 'bg-secondary' :
                  log.type === 'privacy' ? 'bg-accent' : 'bg-gray-300'
                }`} />
                <div>
                  <div className="text-sm text-textMain">{log.action}</div>
                  <div className="text-xs text-textMuted">{log.user}</div>
                </div>
              </div>
              <div className="flex items-center gap-1 text-xs text-textMuted">
                <Clock className="w-3 h-3" />
                {log.time}
              </div>
            </div>
          ))}
        </div>
      </motion.div>

      {/* Safety events */}
      <motion.div
        className="bg-gradient-to-br from-softWarning/5 to-softWarning/10 rounded-2xl p-6 border border-softWarning/20"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
      >
        <h2 className="font-semibold text-textMain mb-4">Recent Safety Events</h2>
        <div className="space-y-3">
          {[
            { event: 'Risk Level 3 case detected', time: '10 min ago', status: 'Assigned to Dr. Minh' },
            { event: 'AI response flagged for review', time: '1 hour ago', status: 'Resolved' },
            { event: 'Emergency keyword detected', time: '3 hours ago', status: 'False positive' },
          ].map((event, i) => (
            <div key={i} className="flex items-center justify-between py-2 border-b border-softWarning/10 last:border-0">
              <div className="text-sm text-textMain">{event.event}</div>
              <div className="text-xs text-textMuted">{event.time}</div>
            </div>
          ))}
        </div>
      </motion.div>
    </div>
  );
}
