import { motion } from 'framer-motion';
import { MoodOption } from '../../types';
import { useLanguage } from '../../i18n';

interface MoodOrbProps {
  mood: MoodOption;
  selected?: boolean;
  onClick?: () => void;
  size?: 'sm' | 'md' | 'lg';
}
export default function MoodOrb({ mood, selected, onClick, size = 'md' }: MoodOrbProps) {
  const { t } = useLanguage();
  const sizeConfig = {
    sm: { orb: 48, emoji: 'text-lg', label: 'text-xs' },
    md: { orb: 72, emoji: 'text-2xl', label: 'text-sm' },
    lg: { orb: 96, emoji: 'text-3xl', label: 'text-base' },
  };

  const config = sizeConfig[size];
  const label = t.data.mood[mood.i18nKey];

  return (
    <motion.button
      className="flex flex-col items-center gap-2 focus:outline-none"
      onClick={onClick}
      whileHover={{ scale: 1.1 }}
      whileTap={{ scale: 0.95 }}
    >
      <motion.div
        className="relative rounded-full flex items-center justify-center cursor-pointer"
        style={{
          width: config.orb,
          height: config.orb,
          background: selected 
            ? `linear-gradient(135deg, ${mood.color}40, ${mood.color}20)`
            : 'linear-gradient(135deg, rgba(255,255,255,0.9), rgba(255,255,255,0.7))',
          border: `2px solid ${selected ? mood.color : 'rgba(0,0,0,0.08)'}`,
          boxShadow: selected 
            ? `0 0 20px ${mood.color}40, 0 4px 20px rgba(0,0,0,0.08)` 
            : '0 4px 20px rgba(0,0,0,0.06)',
        }}
        animate={selected ? {
          boxShadow: [
            `0 0 20px ${mood.color}40, 0 4px 20px rgba(0,0,0,0.08)`,
            `0 0 30px ${mood.color}60, 0 4px 25px rgba(0,0,0,0.1)`,
            `0 0 20px ${mood.color}40, 0 4px 20px rgba(0,0,0,0.08)`,
          ],
        } : {}}
        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
      >
        <span className={`${config.emoji} select-none`}>
          {mood.emoji}
        </span>
      </motion.div>
      <span className={`${config.label} font-medium ${selected ? 'text-textMain' : 'text-textMuted'}`}>
        {label}
      </span>
    </motion.button>
  );
}
