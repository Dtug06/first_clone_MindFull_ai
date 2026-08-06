import { motion, AnimatePresence } from 'framer-motion';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { useLanguage } from '../../i18n';

interface BreathingOrbProps {
  size?: 'sm' | 'md' | 'lg';
  autoStart?: boolean;
}

export default function BreathingOrb({ size = 'md', autoStart = false }: BreathingOrbProps) {
  const { t } = useLanguage();

  const PHASES_478 = useMemo(() => [
    { id: 'inhale', duration: 4, label: t.user.breathingInhale, next: 'hold'   as const },
    { id: 'hold',   duration: 7, label: t.user.breathingHold,   next: 'exhale' as const },
    { id: 'exhale', duration: 8, label: t.user.breathingExhale, next: 'inhale'  as const },
  ], [t]);

  const SCALE_MIN = 0.62;
  const SCALE_MAX = 1.45;

  const [isActive, setIsActive]     = useState(autoStart);
  const [phase, setPhase]           = useState<'idle' | 'inhale' | 'hold' | 'exhale'>('idle');
  const [countdown, setCountdown]   = useState(PHASES_478[0].duration);
  const [phaseLabel, setPhaseLabel] = useState('');
  const [cycle, setCycle]           = useState(0);

  const sizeConfig = {
    sm: { ring: 160 },
    md: { ring: 240 },
    lg: { ring: 320 },
  };
  const config = sizeConfig[size];

  const stopCycle = useCallback(() => {
    setIsActive(false);
    setPhase('idle');
    setCountdown(PHASES_478[0].duration);
    setPhaseLabel('');
  }, [PHASES_478]);

  const startCycle = useCallback(() => {
    setCycle(0);
    setIsActive(true);
  }, []);

  useEffect(() => {
    if (!isActive) return;

    let tickInterval: ReturnType<typeof setInterval>;
    let phaseTimeout: ReturnType<typeof setTimeout>;

    const runPhase = (phaseIdx: number) => {
      const p = PHASES_478[phaseIdx];
      setPhase(p.id as 'inhale' | 'hold' | 'exhale');
      setPhaseLabel(p.label);
      setCountdown(p.duration);

      let remaining = p.duration;

      clearInterval(tickInterval);
      tickInterval = setInterval(() => {
        remaining -= 1;
        setCountdown(remaining);
        if (remaining <= 0) clearInterval(tickInterval);
      }, 1000);

      phaseTimeout = setTimeout(() => {
        clearInterval(tickInterval);

        const nextIdx = PHASES_478.findIndex(x => x.id === p.next);
        if (p.id === 'exhale') setCycle(c => c + 1);
        runPhase(nextIdx);
      }, p.duration * 1000);
    };

    runPhase(0);

    return () => {
      clearInterval(tickInterval);
      clearTimeout(phaseTimeout);
    };
  }, [isActive, PHASES_478]);

  const getCurrentPhase = () => PHASES_478.find(p => p.id === phase);

  const getOrbScale = () => {
    if (phase === 'idle')   return SCALE_MIN;
    if (phase === 'inhale') return SCALE_MAX;
    if (phase === 'hold')   return SCALE_MAX;
    if (phase === 'exhale') return SCALE_MIN;
    return SCALE_MIN;
  };

  const getOrbDuration = () => {
    if (phase === 'idle') return 0;
    return getCurrentPhase()?.duration ?? 0;
  };

  const getOrbEase = () => {
    if (phase === 'inhale') return [0.25, 0.1, 0.25, 1] as const;
    if (phase === 'exhale') return [0.75, 0.05, 0.85, 0.3] as const;
    if (phase === 'hold')   return 'linear' as const;
    return 'easeInOut' as const;
  };

  const getGlowAlpha = () => {
    if (phase === 'idle')   return 0.25;
    if (phase === 'inhale') return 0.75;
    if (phase === 'hold')   return 0.6;
    if (phase === 'exhale') return 0.4;
    return 0.25;
  };

  const glowAlpha = getGlowAlpha();

  const numFontSize = size === 'sm' ? '2.5rem' : size === 'lg' ? '4.5rem' : '3.75rem';

  return (
    <div className="flex flex-col items-center gap-5">

      {/* ── Technique badge ── */}
      <motion.div
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
        className="inline-flex items-center gap-1.5 px-4 py-1.5 rounded-full"
        style={{ background: 'rgba(95,158,151,0.12)', border: '1px solid rgba(95,158,151,0.25)' }}
      >
        <span className="w-1.5 h-1.5 rounded-full" style={{ background: '#5F9E97' }} />
        <span
          className="text-xs font-semibold tracking-widest uppercase"
          style={{ color: '#3D7A74', letterSpacing: '0.12em' }}
        >
          {t.user.breathingTechnique478}
        </span>
      </motion.div>

      {/* ── Main orb area ── */}
      <div
        className="relative flex items-center justify-center"
        style={{ width: config.ring, height: config.ring }}
      >
        {/* Ripple ring 3 — outermost */}
        <motion.div
          className="absolute rounded-full"
          style={{ border: '1.5px solid rgba(95,158,151,0.18)' }}
          animate={
            phase === 'idle'
              ? { scale: [1, 1.18, 1.38], opacity: [0.55, 0.28, 0] }
              : { scale: getOrbScale() * 1.18, opacity: glowAlpha * 0.65 }
          }
          transition={
            phase === 'idle'
              ? { duration: 3.2, ease: 'easeOut', repeat: Infinity }
              : { duration: getOrbDuration(), ease: getOrbEase() }
          }
        />

        {/* Ripple ring 2 */}
        <motion.div
          className="absolute rounded-full"
          style={{ inset: '9%', border: '1px solid rgba(95,158,151,0.22)' }}
          animate={
            phase === 'idle'
              ? { scale: [1, 1.14, 1.28], opacity: [0.65, 0.35, 0.05] }
              : { scale: getOrbScale() * 1.1, opacity: glowAlpha * 0.8 }
          }
          transition={
            phase === 'idle'
              ? { duration: 3.2, ease: 'easeOut', repeat: Infinity, delay: 0.5 }
              : { duration: getOrbDuration(), ease: getOrbEase() }
          }
        />

        {/* Ripple ring 1 */}
        <motion.div
          className="absolute rounded-full"
          style={{ inset: '18%', border: '1px solid rgba(95,158,151,0.28)' }}
          animate={
            phase === 'idle'
              ? { scale: [1, 1.08, 1.16], opacity: [0.75, 0.45, 0.1] }
              : { scale: getOrbScale() * 1.05, opacity: glowAlpha * 0.9 }
          }
          transition={
            phase === 'idle'
              ? { duration: 3.2, ease: 'easeOut', repeat: Infinity, delay: 1.0 }
              : { duration: getOrbDuration(), ease: getOrbEase() }
          }
        />

        {/* Glow halo */}
        <motion.div
          className="absolute rounded-full"
          style={{ inset: '20%', background: `radial-gradient(circle, rgba(95,158,151,${glowAlpha * 0.55}) 0%, transparent 68%)` }}
          animate={{ scale: getOrbScale() }}
          transition={{ duration: getOrbDuration(), ease: getOrbEase() }}
        />

        {/* Main glass orb */}
        <motion.div
          className="absolute rounded-full flex items-center justify-center overflow-hidden"
          style={{
            inset: '22%',
            background: phase === 'idle'
              ? 'linear-gradient(145deg, rgba(95,158,151,0.42) 0%, rgba(61,122,116,0.35) 100%)'
              : 'linear-gradient(145deg, rgba(95,158,151,0.72) 0%, rgba(61,122,116,0.58) 100%)',
            backdropFilter: 'blur(16px)',
            WebkitBackdropFilter: 'blur(16px)',
            border: '1.5px solid rgba(255,255,255,0.6)',
            boxShadow: `
              0 0 ${Math.round(glowAlpha * 90)}px rgba(95,158,151,${glowAlpha}),
              0 0 ${Math.round(glowAlpha * 140)}px rgba(95,158,151,${glowAlpha * 0.35}),
              inset 0 1px 2px rgba(255,255,255,0.7),
              inset 0 -4px 12px rgba(0,0,0,0.08)
            `,
          }}
          animate={{ scale: getOrbScale() }}
          transition={{ duration: getOrbDuration(), ease: getOrbEase() }}
        >
          {/* Glass sheen */}
          <div
            className="absolute inset-0 rounded-full pointer-events-none"
            style={{
              background: 'radial-gradient(ellipse at 33% 28%, rgba(255,255,255,0.45) 0%, transparent 55%)',
            }}
          />

          {/* Countdown number ONLY */}
          <div className="relative z-10 flex items-center justify-center">
            <AnimatePresence mode="wait">
              <motion.span
                key={isActive ? `${phase}-${countdown}` : 'idle'}
                initial={{ opacity: 0, scale: 0.4 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 1.6 }}
                transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
                className="font-medium leading-none select-none text-white"
                style={{
                  fontSize: numFontSize,
                  fontWeight: 500,
                  letterSpacing: '-0.03em',
                  textShadow: '0 2px 24px rgba(0,0,0,0.22), 0 0 50px rgba(95,158,151,0.4)',
                }}
              >
                {isActive ? countdown : ''}
              </motion.span>
            </AnimatePresence>
          </div>
        </motion.div>
      </div>

      {/* ── Phase label + timing — OUTSIDE the orb ── */}
      <div className="flex flex-col items-center gap-1.5 min-h-[52px]">

        {/* Phase instruction */}
        <AnimatePresence mode="wait">
          {isActive ? (
            <motion.div
              key={phaseLabel}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.3, ease: 'easeOut' }}
              className="flex flex-col items-center"
            >
              <span
                className="font-medium tracking-wide"
                style={{ fontSize: '1.3rem', color: '#263238', letterSpacing: '0.02em' }}
              >
                {phaseLabel}
              </span>
              <span
                className="text-xs"
                style={{ color: 'rgba(95,158,151,0.6)', fontSize: '0.78rem', letterSpacing: '0.04em' }}
              >
                {t.user.breathingCycle} {cycle}
              </span>
            </motion.div>
          ) : (
            <motion.div
              key="idle-hint"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.3 }}
              className="flex flex-col items-center"
            >
              <span
                className="text-sm font-medium"
                style={{ color: '#6E7772', letterSpacing: '0.06em' }}
              >
                {t.user.breathingInhale4s} &middot; {t.user.breathingHold7s} &middot; {t.user.breathingExhale8s}
              </span>
              <span
                className="text-xs"
                style={{ color: 'rgba(95,158,151,0.5)', fontSize: '0.75rem', letterSpacing: '0.04em', marginTop: '2px' }}
              >
                {t.user.breathingPressStart}
              </span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* ── Control button ── */}
      <motion.button
        className="px-10 py-3 rounded-full text-sm font-medium transition-shadow"
        style={
          isActive
            ? {
                background: '#EEF4F1',
                color: '#263238',
                border: '1px solid rgba(95,158,151,0.2)',
                boxShadow: '0 2px 16px rgba(38,50,56,0.06)',
              }
            : {
                background: '#5F9E97',
                color: '#fff',
                boxShadow: '0 0 32px rgba(95,158,151,0.38), 0 4px 16px rgba(95,158,151,0.22)',
              }
        }
        onClick={() => (isActive ? stopCycle() : startCycle())}
        whileHover={{ scale: 1.07, boxShadow: isActive
          ? '0 4px 20px rgba(38,50,56,0.1)'
          : '0 0 52px rgba(95,158,151,0.52), 0 6px 20px rgba(95,158,151,0.3)'
        }}
        whileTap={{ scale: 0.93 }}
      >
        {isActive ? t.user.breathingStop : t.user.breathingStart}
      </motion.button>
    </div>
  );
}
