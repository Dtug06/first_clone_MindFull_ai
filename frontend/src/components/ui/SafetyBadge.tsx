import { motion } from 'framer-motion';
import { Shield, CheckCircle } from 'lucide-react';

interface SafetyBadgeProps {
  variant?: 'default' | 'compact';
}

export default function SafetyBadge({ variant = 'default' }: SafetyBadgeProps) {
  if (variant === 'compact') {
    return (
      <motion.div
        className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-primary/10 rounded-full"
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        whileHover={{ scale: 1.05 }}
      >
        <Shield className="w-3.5 h-3.5 text-primary" />
        <span className="text-xs font-medium text-primary">Safety Checked</span>
      </motion.div>
    );
  }

  return (
    <motion.div
      className="inline-flex items-center gap-3 px-4 py-3 bg-primary/5 rounded-2xl border border-primary/10"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ scale: 1.02 }}
    >
      <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
        <Shield className="w-5 h-5 text-primary" />
      </div>
      <div>
        <div className="flex items-center gap-2">
          <span className="font-medium text-textMain">AI Safety Checked</span>
          <CheckCircle className="w-4 h-4 text-primary" />
        </div>
        <p className="text-sm text-textMuted">Responses are reviewed for safety</p>
      </div>
    </motion.div>
  );
}
