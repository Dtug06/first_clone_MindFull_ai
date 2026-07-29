import { motion } from 'framer-motion';
import { ReactNode } from 'react';

interface CalmCardProps {
  children: ReactNode;
  className?: string;
  hover?: boolean;
  glass?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

export default function CalmCard({ 
  children, 
  className = '',
  hover = true,
  glass = false,
  padding = 'md'
}: CalmCardProps) {
  const paddingClasses = {
    none: '',
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8',
  };

  const baseClasses = `
    rounded-3xl 
    ${paddingClasses[padding]}
    ${glass 
      ? 'bg-white/70 backdrop-blur-md border border-white/60 shadow-soft-lg' 
      : 'bg-surface border border-gray-100 shadow-soft'
    }
    ${className}
  `;

  if (!hover) {
    return <div className={baseClasses}>{children}</div>;
  }

  return (
    <motion.div
      className={baseClasses}
      whileHover={{ y: -4, boxShadow: '0 8px 40px rgba(38, 50, 56, 0.1)' }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
    >
      {children}
    </motion.div>
  );
}
