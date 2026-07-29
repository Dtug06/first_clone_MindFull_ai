import { motion } from 'framer-motion';
import { useMemo } from 'react';

interface AnimatedGradientBlobProps {
  className?: string;
  colors?: string[];
  size?: 'sm' | 'md' | 'lg' | 'xl';
}

const colorSets = {
  calm: ['#5F9E97', '#6F86A6', '#D8C7A8'],
  ocean: ['#243B4A', '#5F9E97', '#6F86A6'],
  warm: ['#D8C7A8', '#C8766B', '#5F9E97'],
  mist: ['#E8E4F2', '#6F86A6', '#5F9E97'],
};

export default function AnimatedGradientBlob({ 
  className = '',
  colors = colorSets.calm,
  size = 'md'
}: AnimatedGradientBlobProps) {
  const sizeClasses = {
    sm: 'w-32 h-32 max-w-full',
    md: 'w-48 h-48 sm:w-64 sm:h-64 max-w-full',
    lg: 'w-64 h-64 sm:w-80 sm:h-80 md:w-96 md:h-96 max-w-full',
    xl: 'w-72 h-72 sm:w-96 sm:h-96 md:w-[420px] md:h-[420px] lg:w-[500px] lg:h-[500px] max-w-full',
  };

  const blobPaths = useMemo(() => [
    'M450,280 C520,320 580,250 550,180 C520,110 420,80 350,120 C280,160 240,240 280,310 C320,380 380,240 450,280',
    'M460,290 C530,330 590,260 560,190 C530,120 430,90 360,130 C290,170 250,250 290,320 C330,390 390,250 460,290',
    'M440,270 C510,310 570,240 540,170 C510,100 410,70 340,110 C270,150 230,230 270,300 C310,370 370,230 440,270',
  ], []);

  return (
    <div className={`relative overflow-hidden ${className}`} style={{ maxWidth: '100%' }}>
      <motion.div
        className={`absolute inset-0 ${sizeClasses[size]} blur-3xl pointer-events-none`}
        animate={{
          background: [
            `radial-gradient(circle, ${colors[0]}40 0%, transparent 70%)`,
            `radial-gradient(circle, ${colors[1]}40 0%, transparent 70%)`,
            `radial-gradient(circle, ${colors[2]}40 0%, transparent 70%)`,
            `radial-gradient(circle, ${colors[0]}40 0%, transparent 70%)`,
          ],
          scale: [1, 1.1, 0.95, 1.05, 1],
          rotate: [0, 10, -5, 8, 0],
        }}
        transition={{
          duration: 15,
          repeat: Infinity,
          ease: 'easeInOut',
        }}
      />
      
      <motion.svg
        viewBox="0 0 800 600"
        className={`w-full h-full ${sizeClasses[size]}`}
        preserveAspectRatio="xMidYMid slice"
      >
        <defs>
          <linearGradient id="blobGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <motion.stop
              offset="0%"
              stopColor={colors[0]}
              stopOpacity="0.3"
              animate={{
                stopColor: [colors[0], colors[1], colors[2], colors[0]],
              }}
              transition={{ duration: 8, repeat: Infinity }}
            />
            <motion.stop
              offset="50%"
              stopColor={colors[1]}
              stopOpacity="0.2"
              animate={{
                stopColor: [colors[1], colors[2], colors[0], colors[1]],
              }}
              transition={{ duration: 8, repeat: Infinity }}
            />
            <motion.stop
              offset="100%"
              stopColor={colors[2]}
              stopOpacity="0.25"
              animate={{
                stopColor: [colors[2], colors[0], colors[1], colors[2]],
              }}
              transition={{ duration: 8, repeat: Infinity }}
            />
          </linearGradient>
          <filter id="blobGlow">
            <feGaussianBlur stdDeviation="8" result="coloredBlur"/>
            <feMerge>
              <feMergeNode in="coloredBlur"/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>
        </defs>

        <motion.path
          d={blobPaths[0]}
          fill="url(#blobGradient)"
          filter="url(#blobGlow)"
          animate={{
            d: blobPaths,
          }}
          transition={{
            duration: 20,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        />
      </motion.svg>
    </div>
  );
}
