import { motion } from 'framer-motion';
import { useState, useEffect } from 'react';

interface BreathingOrbProps {
  size?: 'sm' | 'md' | 'lg';
  autoStart?: boolean;
}

export default function BreathingOrb({
  size = 'md',
  autoStart = false
}: BreathingOrbProps) {
  const [isActive, setIsActive] = useState(autoStart);
  const [phase, setPhase] = useState<'inhale' | 'hold' | 'exhale' | 'rest'>('rest');
  const [cycle, setCycle] = useState(0);

  const sizeConfig = {
    sm: { base: 60, ring: 80 },
    md: { base: 100, ring: 130 },
    lg: { base: 160, ring: 200 },
  };

  const phases = [
    { name: 'Breathe In', duration: 4000, next: 'hold' as const },
    { name: 'Hold', duration: 4000, next: 'exhale' as const },
    { name: 'Breathe Out', duration: 4000, next: 'rest' as const },
    { name: 'Rest', duration: 2000, next: 'inhale' as const },
  ];

  useEffect(() => {
    if (!isActive) return;

    let currentPhaseIndex = 0;
    let timeoutId: ReturnType<typeof setTimeout>;

    const runPhase = () => {
      const currentPhase = phases[currentPhaseIndex];
      setPhase(
        currentPhase.name === 'Breathe In' ? 'inhale' :
        currentPhase.name === 'Hold' ? 'hold' :
        currentPhase.name === 'Breathe Out' ? 'exhale' : 'rest'
      );

      timeoutId = setTimeout(() => {
        currentPhaseIndex = phases.findIndex(p => p.name === currentPhase.next);
        if (currentPhase.next === 'inhale') {
          setCycle(c => c + 1);
        }
        runPhase();
      }, currentPhase.duration);
    };

    runPhase();

    return () => clearTimeout(timeoutId);
  }, [isActive]);

  const config = sizeConfig[size];

  const getScale = () => {
    switch (phase) {
      case 'inhale': return 1.2;
      case 'hold': return 1.2;
      case 'exhale': return 1;
      case 'rest': return 1;
    }
  };

  const getOpacity = () => {
    switch (phase) {
      case 'inhale': return 0.9;
      case 'hold': return 0.9;
      case 'exhale': return 0.5;
      case 'rest': return 0.5;
    }
  };

  const currentPhase = phases.find(p => 
    p.name === (phase === 'inhale' ? 'Breathe In' : 
               phase === 'hold' ? 'Hold' : 
               phase === 'exhale' ? 'Breathe Out' : 'Rest'));

  return (
    <div className="flex flex-col items-center gap-4">
      <div className="relative" style={{ width: config.ring, height: config.ring, maxWidth: '100%' }}>
        {/* Outer ring */}
        <motion.div
          className="absolute inset-0 rounded-full border-2 border-primary/20"
          animate={{ scale: getScale(), opacity: getOpacity() }}
          transition={{ duration: 4, ease: 'easeInOut' }}
        />

        {/* Second ring */}
        <motion.div
          className="absolute inset-2 rounded-full border border-primary/30"
          animate={{ 
            scale: getScale() * 0.9,
            opacity: getOpacity() * 0.8 
          }}
          transition={{ duration: 4, ease: 'easeInOut' }}
        />

        {/* Inner glow */}
        <motion.div
          className="absolute inset-4 rounded-full"
          style={{
            background: 'radial-gradient(circle, rgba(95, 158, 151, 0.4) 0%, rgba(95, 158, 151, 0.1) 70%, transparent 100%)',
          }}
          animate={{ 
            scale: getScale() * 0.7,
            opacity: getOpacity(),
          }}
          transition={{ duration: 4, ease: 'easeInOut' }}
        />

        {/* Center orb */}
        <motion.div
          className="absolute inset-8 rounded-full bg-gradient-to-br from-primary/60 to-primaryDark/40"
          animate={{ 
            scale: getScale() * 0.5,
            opacity: getOpacity() + 0.1,
          }}
          transition={{ duration: 4, ease: 'easeInOut' }}
          style={{
            boxShadow: '0 0 40px rgba(95, 158, 151, 0.4)',
          }}
        />

        {/* Phase text */}
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-textMain/60 text-sm font-medium">
            {isActive ? currentPhase?.name : 'Tap to start'}
          </span>
        </div>
      </div>

      {/* Cycle counter */}
      {isActive && (
        <motion.div 
          className="text-textMuted text-sm"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
        >
          Cycle {cycle}
        </motion.div>
      )}

      {/* Start/Stop button */}
      <motion.button
        className="btn-secondary text-sm"
        onClick={() => setIsActive(!isActive)}
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
      >
        {isActive ? 'Stop' : 'Start Breathing'}
      </motion.button>
    </div>
  );
}
