import { motion } from 'framer-motion';
import { Users, CheckCircle, TrendingUp, AlertTriangle, LucideIcon } from 'lucide-react';

interface DashboardMetricCardProps {
  label: string;
  value: string | number;
  change?: string;
  trend?: 'up' | 'down' | 'stable';
  icon: string;
  delay?: number;
}

const iconMap: Record<string, LucideIcon> = {
  Users,
  CheckCircle,
  TrendingUp,
  AlertTriangle,
};

export default function DashboardMetricCard({ 
  label, 
  value, 
  change, 
  trend, 
  icon,
  delay = 0 
}: DashboardMetricCardProps) {
  const Icon = iconMap[icon] || Users;

  const trendColors = {
    up: 'text-primary',
    down: 'text-softWarning',
    stable: 'text-textMuted',
  };

  return (
    <motion.div
      className="bg-surface rounded-2xl p-6 border border-gray-100 shadow-soft"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: delay * 0.1 }}
      whileHover={{ y: -2, boxShadow: '0 8px 40px rgba(38, 50, 56, 0.08)' }}
    >
      <div className="flex items-start justify-between">
        <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
          <Icon className="w-6 h-6 text-primary" />
        </div>
        {change && (
          <div className={`text-sm font-medium ${trend ? trendColors[trend] : 'text-textMuted'}`}>
            {trend === 'up' && '+'}
            {change}
          </div>
        )}
      </div>
      <div className="mt-4">
        <div className="text-3xl font-semibold text-textMain">{value}</div>
        <div className="text-sm text-textMuted mt-1">{label}</div>
      </div>
    </motion.div>
  );
}
