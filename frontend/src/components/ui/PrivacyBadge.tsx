import { motion } from 'framer-motion';
import { Lock } from 'lucide-react';

interface PrivacyBadgeProps {
  variant?: 'default' | 'compact';
}

export default function PrivacyBadge({ variant = 'default' }: PrivacyBadgeProps) {
  if (variant === 'compact') {
    return (
      <motion.div
        className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-secondary/10 rounded-full"
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        whileHover={{ scale: 1.05 }}
      >
        <Lock className="w-3.5 h-3.5 text-secondary" />
        <span className="text-xs font-medium text-secondary">Private</span>
      </motion.div>
    );
  }

  return (
    <motion.div
      className="inline-flex items-center gap-3 px-4 py-3 bg-secondary/5 rounded-2xl border border-secondary/10"
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ scale: 1.02 }}
    >
      <div className="w-10 h-10 rounded-xl bg-secondary/10 flex items-center justify-center">
        <Lock className="w-5 h-5 text-secondary" />
      </div>
      <div>
        <div className="font-medium text-textMain">Your Data is Protected</div>
        <p className="text-sm text-textMuted">You control your data at all times</p>
      </div>
    </motion.div>
  );
}
