import { motion } from 'framer-motion';
import JellyfishMascot from './JellyfishMascot';

interface FloatingJellyfishBackgroundProps {
  count?: number;
  className?: string;
  opacity?: number;
}

export default function FloatingJellyfishBackground({ 
  count = 5,
  className = '',
  opacity = 0.1 
}: FloatingJellyfishBackgroundProps) {
  const jellyfishPositions = [
    { top: '10%', left: '5%', size: 'sm' as const, delay: 0 },
    { top: '25%', right: '8%', size: 'md' as const, delay: 0.5 },
    { top: '60%', left: '3%', size: 'sm' as const, delay: 1 },
    { top: '70%', right: '5%', size: 'md' as const, delay: 1.5 },
    { top: '85%', left: '15%', size: 'sm' as const, delay: 2 },
    { top: '15%', left: '40%', size: 'sm' as const, delay: 0.3 },
    { top: '80%', right: '20%', size: 'sm' as const, delay: 1.8 },
  ];

  return (
    <div className={`absolute inset-0 overflow-hidden pointer-events-none ${className}`}>
      {jellyfishPositions.slice(0, count).map((jelly, index) => (
        <motion.div
          key={index}
          className="absolute"
          style={{
            top: jelly.top,
            left: jelly.left,
            right: jelly.right,
            opacity: opacity,
          }}
          initial={{ opacity: 0, scale: 0.5 }}
          animate={{ opacity: opacity, scale: 1 }}
          transition={{ duration: 1, delay: jelly.delay }}
        >
          <JellyfishMascot size={jelly.size} animated />
        </motion.div>
      ))}
      
      {/* Ambient bubbles */}
      <div className="absolute inset-0">
        {[...Array(12)].map((_, i) => (
          <motion.div
            key={`bubble-${i}`}
            className="absolute w-2 h-2 rounded-full bg-primary/20"
            style={{
              left: `${10 + (i * 7) % 80}%`,
              bottom: `${-5}%`,
            }}
            animate={{
              y: ['0vh', '-100vh'],
              opacity: [0, 0.4, 0],
              x: [0, (i % 2 === 0 ? 20 : -20)],
            }}
            transition={{
              duration: 8 + (i % 4) * 2,
              repeat: Infinity,
              delay: (i * 0.8) % 5,
              ease: 'linear',
            }}
          />
        ))}
      </div>

      {/* Wave effect at bottom */}
      <svg
        className="absolute bottom-0 left-0 w-full"
        viewBox="0 0 1440 120"
        preserveAspectRatio="none"
      >
        <motion.path
          d="M0,64 C320,100 420,30 720,64 C1020,98 1140,34 1440,64 L1440,120 L0,120 Z"
          fill="url(#waveGradient)"
          animate={{
            d: [
              'M0,64 C320,100 420,30 720,64 C1020,98 1140,34 1440,64 L1440,120 L0,120 Z',
              'M0,64 C320,30 420,98 720,64 C1020,30 1140,100 1440,64 L1440,120 L0,120 Z',
              'M0,64 C320,100 420,30 720,64 C1020,98 1140,34 1440,64 L1440,120 L0,120 Z',
            ],
          }}
          transition={{ duration: 10, repeat: Infinity, ease: 'easeInOut' }}
        />
        <defs>
          <linearGradient id="waveGradient" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#5F9E97" stopOpacity="0.05" />
            <stop offset="100%" stopColor="#5F9E97" stopOpacity="0.02" />
          </linearGradient>
        </defs>
      </svg>
    </div>
  );
}
