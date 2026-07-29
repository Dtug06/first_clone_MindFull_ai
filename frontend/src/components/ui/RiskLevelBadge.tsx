import { motion } from 'framer-motion';
import { RiskLevel } from '../../types';

interface RiskLevelBadgeProps {
  level: RiskLevel;
  showLabel?: boolean;
  size?: 'sm' | 'md';
}

const levelConfig = {
  1: { 
    label: 'Normal', 
    color: '#5F9E97', 
    bgColor: 'rgba(95, 158, 151, 0.1)',
    icon: '○'
  },
  2: { 
    label: 'Monitoring', 
    color: '#D8C7A8', 
    bgColor: 'rgba(216, 199, 168, 0.2)',
    icon: '◐'
  },
  3: { 
    label: 'High Risk', 
    color: '#C8766B', 
    bgColor: 'rgba(200, 118, 107, 0.15)',
    icon: '◑'
  },
  4: { 
    label: 'Emergency', 
    color: '#B85A50', 
    bgColor: 'rgba(200, 118, 107, 0.2)',
    icon: '●'
  },
};

export default function RiskLevelBadge({ 
  level, 
  showLabel = true,
  size = 'md'
}: RiskLevelBadgeProps) {
  const config = levelConfig[level];

  return (
    <motion.div
      className="inline-flex items-center gap-1.5 rounded-full"
      style={{ 
        backgroundColor: config.bgColor,
        padding: size === 'sm' ? '2px 8px' : '4px 12px',
      }}
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      whileHover={{ scale: 1.05 }}
    >
      <span 
        className="font-bold text-sm"
        style={{ color: config.color }}
      >
        {config.icon}
      </span>
      {showLabel && (
        <span 
          className={`font-medium ${size === 'sm' ? 'text-xs' : 'text-sm'}`}
          style={{ color: config.color }}
        >
          {config.label}
        </span>
      )}
    </motion.div>
  );
}
