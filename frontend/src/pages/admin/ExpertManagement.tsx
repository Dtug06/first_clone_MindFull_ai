import { motion } from 'framer-motion';
import { experts } from '../../data';
import { Plus, MoreVertical, Mail, Phone } from 'lucide-react';

const statusColors = {
  available: 'bg-primary/10 text-primary',
  busy: 'bg-softWarning/10 text-softWarning',
  offline: 'bg-gray-100 text-textMuted',
};

export default function ExpertManagement() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        className="flex flex-col sm:flex-row sm:items-center justify-between gap-4"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <div>
          <h1 className="text-2xl font-semibold text-textMain">Expert Management</h1>
          <p className="text-textMuted">Manage psychologist and counselor accounts.</p>
        </div>
        <motion.button
          className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-xl text-sm font-medium"
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          <Plus className="w-4 h-4" />
          Add Expert
        </motion.button>
      </motion.div>

      {/* Expert cards */}
      <div className="grid md:grid-cols-2 gap-4">
        {experts.map((expert, index) => (
          <motion.div
            key={expert.id}
            className="bg-surface rounded-2xl p-5 shadow-soft border border-gray-100"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 + index * 0.05 }}
          >
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary/20 to-primaryDark/20 flex items-center justify-center text-primary font-semibold">
                  {expert.name.split(' ').pop()?.charAt(0)}
                </div>
                <div>
                  <h3 className="font-semibold text-textMain">{expert.name}</h3>
                  <p className="text-sm text-textMuted">{expert.email}</p>
                </div>
              </div>
              <button className="p-2 hover:bg-gray-100 rounded-lg">
                <MoreVertical className="w-4 h-4 text-textMuted" />
              </button>
            </div>

            <div className="flex flex-wrap gap-2 mb-4">
              {expert.specialty.map((spec, i) => (
                <span key={i} className="px-2 py-1 bg-surfaceMuted rounded-full text-xs text-textMuted">
                  {spec}
                </span>
              ))}
            </div>

            <div className="flex items-center justify-between pt-4 border-t border-gray-100">
              <div className="flex items-center gap-4">
                <span className={`px-2 py-1 text-xs font-medium rounded-full ${statusColors[expert.status]}`}>
                  {expert.status.charAt(0).toUpperCase() + expert.status.slice(1)}
                </span>
                <span className="text-sm text-textMuted">
                  {expert.assignedCases} cases
                </span>
              </div>
              <div className="flex gap-2">
                <button className="p-2 hover:bg-gray-100 rounded-lg">
                  <Mail className="w-4 h-4 text-textMuted" />
                </button>
                <button className="p-2 hover:bg-gray-100 rounded-lg">
                  <Phone className="w-4 h-4 text-textMuted" />
                </button>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
