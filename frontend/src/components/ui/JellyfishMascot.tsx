import { motion } from 'framer-motion';
import { useMemo } from 'react';

interface JellyfishMascotProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  animated?: boolean;
  className?: string;
}

const sizes = {
  sm: { wrapper: 60, head: 30, tentacle: 25 },
  md: { wrapper: 100, head: 50, tentacle: 40 },
  lg: { wrapper: 160, head: 80, tentacle: 65 },
  xl: { wrapper: 220, head: 110, tentacle: 90 },
};

export default function JellyfishMascot({ 
  size = 'md', 
  animated = true,
  className = '' 
}: JellyfishMascotProps) {
  const dimensions = sizes[size];

  const tentaclePaths = useMemo(() => [
    { d: `M${dimensions.wrapper/2 - 15} ${dimensions.head + 5} Q${dimensions.wrapper/2 - 25} ${dimensions.head + dimensions.tentacle/2} ${dimensions.wrapper/2 - 18} ${dimensions.head + dimensions.tentacle}`, delay: 0 },
    { d: `M${dimensions.wrapper/2 - 5} ${dimensions.head + 8} Q${dimensions.wrapper/2 - 12} ${dimensions.head + dimensions.tentacle/2 + 5} ${dimensions.wrapper/2 - 6} ${dimensions.head + dimensions.tentacle + 5}`, delay: 0.2 },
    { d: `M${dimensions.wrapper/2 + 5} ${dimensions.head + 8} Q${dimensions.wrapper/2 + 12} ${dimensions.head + dimensions.tentacle/2 + 5} ${dimensions.wrapper/2 + 6} ${dimensions.head + dimensions.tentacle + 5}`, delay: 0.4 },
    { d: `M${dimensions.wrapper/2 + 15} ${dimensions.head + 5} Q${dimensions.wrapper/2 + 25} ${dimensions.head + dimensions.tentacle/2} ${dimensions.wrapper/2 + 18} ${dimensions.head + dimensions.tentacle}`, delay: 0.6 },
  ], [dimensions]);

  const floatAnimation = animated ? {
    y: [0, -15, -8, -20, 0],
    transition: {
      duration: 6,
      repeat: Infinity,
      ease: "easeInOut"
    }
  } : {};

  const glowAnimation = animated ? {
    opacity: [0.3, 0.6, 0.4, 0.7, 0.3],
    scale: [1, 1.05, 1.02, 1.08, 1],
    transition: {
      duration: 4,
      repeat: Infinity,
      ease: "easeInOut"
    }
  } : {};

  return (
    <motion.div 
      className={`relative ${className}`}
      style={{ width: dimensions.wrapper, height: dimensions.wrapper + dimensions.tentacle }}
      animate={floatAnimation}
    >
      {/* Glow effect */}
      <motion.div
        className="absolute inset-0 rounded-full"
        style={{
          background: 'radial-gradient(circle, rgba(95, 158, 151, 0.3) 0%, transparent 70%)',
          filter: 'blur(20px)',
        }}
        animate={glowAnimation}
      />

      {/* Jellyfish body */}
      <motion.svg
        viewBox={`0 0 ${dimensions.wrapper} ${dimensions.wrapper + dimensions.tentacle}`}
        className="w-full h-full"
        initial={{ opacity: 0, scale: 0.8 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.8, ease: "easeOut" }}
      >
        <defs>
          <radialGradient id={`jellyGrad-${size}`} cx="50%" cy="30%" r="70%">
            <stop offset="0%" stopColor="#B8E4E0" stopOpacity="1" />
            <stop offset="40%" stopColor="#7ABFB8" stopOpacity="0.95" />
            <stop offset="70%" stopColor="#5F9E97" stopOpacity="0.9" />
            <stop offset="100%" stopColor="#3F7470" stopOpacity="0.85" />
          </radialGradient>
          <filter id={`glow-${size}`}>
            <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
            <feMerge>
              <feMergeNode in="coloredBlur"/>
              <feMergeNode in="SourceGraphic"/>
            </feMerge>
          </filter>
          <linearGradient id={`tentacleGrad-${size}`} x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" stopColor="#5F9E97" stopOpacity="0.8" />
            <stop offset="100%" stopColor="#3F7470" stopOpacity="0.3" />
          </linearGradient>
        </defs>

        {/* Head/Bell of jellyfish */}
        <motion.ellipse
          cx={dimensions.wrapper / 2}
          cy={dimensions.head / 2}
          rx={dimensions.head / 2}
          ry={dimensions.head / 2.2}
          fill={`url(#jellyGrad-${size})`}
          filter={`url(#glow-${size})`}
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ duration: 0.6, delay: 0.2 }}
        />

        {/* Inner glow ring */}
        <motion.ellipse
          cx={dimensions.wrapper / 2}
          cy={dimensions.head / 2.3}
          rx={dimensions.head / 3.5}
          ry={dimensions.head / 4}
          fill="none"
          stroke="rgba(255, 255, 255, 0.4)"
          strokeWidth="1"
          initial={{ opacity: 0, scale: 0.5 }}
          animate={{ opacity: 0.4, scale: 1 }}
          transition={{ duration: 0.5, delay: 0.5 }}
        />

        {/* Small highlight */}
        <motion.ellipse
          cx={dimensions.wrapper / 2 - dimensions.head / 6}
          cy={dimensions.head / 3}
          rx={dimensions.head / 10}
          ry={dimensions.head / 15}
          fill="rgba(255, 255, 255, 0.5)"
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.5 }}
          transition={{ duration: 0.4, delay: 0.6 }}
        />

        {/* Tentacles */}
        {tentaclePaths.map((path, index) => (
          <motion.path
            key={index}
            d={path.d}
            fill="none"
            stroke={`url(#tentacleGrad-${size})`}
            strokeWidth={dimensions.wrapper / 30}
            strokeLinecap="round"
            initial={{ pathLength: 0, opacity: 0 }}
            animate={{ 
              pathLength: 1, 
              opacity: 0.6,
              d: animated ? [
                path.d,
                path.d.replace(/Q\d+/g, (match) => `Q${parseInt(match.slice(1)) + (index % 2 === 0 ? 10 : -10)}`),
                path.d,
              ] : path.d
            }}
            transition={{
              pathLength: { duration: 0.8, delay: 0.3 + index * 0.1 },
              opacity: { duration: 0.4, delay: 0.4 + index * 0.1 },
              default: { duration: 3, repeat: Infinity, ease: "easeInOut", delay: path.delay }
            }}
          />
        ))}

        {/* Subtle bubbles */}
        {size !== 'sm' && (
          <>
            <motion.circle
              cx={dimensions.wrapper / 3}
              cy={dimensions.head / 2}
              r={dimensions.wrapper / 40}
              fill="rgba(255, 255, 255, 0.3)"
              animate={{
                y: [-5, -15, -5],
                opacity: [0.3, 0.6, 0.3],
              }}
              transition={{ duration: 3, repeat: Infinity, delay: 0 }}
            />
            <motion.circle
              cx={dimensions.wrapper * 2 / 3}
              cy={dimensions.head / 1.5}
              r={dimensions.wrapper / 50}
              fill="rgba(255, 255, 255, 0.25)"
              animate={{
                y: [-8, -18, -8],
                opacity: [0.25, 0.5, 0.25],
              }}
              transition={{ duration: 4, repeat: Infinity, delay: 1 }}
            />
          </>
        )}
      </motion.svg>
    </motion.div>
  );
}
