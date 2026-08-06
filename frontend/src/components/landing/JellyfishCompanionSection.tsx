import { motion } from 'framer-motion';
import JellyfishMascot from '../ui/JellyfishMascot';
import FloatingJellyfishBackground from '../ui/FloatingJellyfishBackground';
import { useLanguage } from '../../i18n';

export default function JellyfishCompanionSection() {
  const { t } = useLanguage();

  const companionFeatures = [
    { label: t.landing.companionCalm, desc: t.landing.companionCalmDesc },
    { label: t.landing.companionEmpathy, desc: t.landing.companionEmpathyDesc },
    { label: t.landing.companionSafe, desc: t.landing.companionSafeDesc },
  ];

  return (
    <section className="py-24 bg-gradient-to-br from-oceanDeep to-oceanDeep/90 relative overflow-hidden text-white">
      {/* Background elements */}
      <div className="absolute inset-0">
        <FloatingJellyfishBackground count={8} opacity={0.15} className="opacity-30" />
        
        {/* Wave effect at bottom */}
        <svg
          className="absolute bottom-0 left-0 w-full"
          viewBox="0 0 1440 200"
          preserveAspectRatio="none"
        >
          <path
            d="M0,100 C200,150 400,50 600,100 C800,150 1000,50 1200,100 C1300,125 1380,100 1440,100 L1440,200 L0,200 Z"
            fill="rgba(95, 158, 151, 0.1)"
          />
          <path
            d="M0,120 C200,170 400,70 600,120 C800,170 1000,70 1200,120 C1300,145 1380,120 1440,120 L1440,200 L0,200 Z"
            fill="rgba(95, 158, 151, 0.05)"
          />
        </svg>
      </div>

      <div className="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left: Jellyfish visual */}
          <motion.div
            className="flex justify-center lg:justify-start order-2 lg:order-1"
            initial={{ opacity: 0, scale: 0.8 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
          >
            <div className="relative">
              {/* Glow effect behind jellyfish */}
              <div 
                className="absolute inset-0 rounded-full blur-3xl"
                style={{
                  background: 'radial-gradient(circle, rgba(95, 158, 151, 0.4) 0%, transparent 70%)',
                }}
              />
              <JellyfishMascot size="xl" animated />
              
              {/* Floating bubbles around */}
              {[...Array(6)].map((_, i) => (
                <motion.div
                  key={i}
                  className="absolute w-3 h-3 rounded-full bg-white/20"
                  style={{
                    top: `${20 + i * 15}%`,
                    left: i % 2 === 0 ? '-20px' : 'auto',
                    right: i % 2 !== 0 ? '-20px' : 'auto',
                  }}
                  animate={{
                    y: [-10, -30, -10],
                    opacity: [0.2, 0.5, 0.2],
                  }}
                  transition={{
                    duration: 3 + i * 0.5,
                    repeat: Infinity,
                    delay: i * 0.3,
                  }}
                />
              ))}
            </div>
          </motion.div>

          {/* Right: Content */}
          <motion.div
            className="order-1 lg:order-2"
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            <h2 className="text-3xl md:text-4xl font-semibold mb-6 text-white">
              {t.landing.companionTitle}
            </h2>
            <p className="text-lg text-white/80 mb-6 leading-relaxed">
              {t.landing.companionP1}
            </p>
            <p className="text-white/70 mb-8 leading-relaxed">
              {t.landing.companionP2}
            </p>

            <div className="space-y-4">
              {companionFeatures.map((item, i) => (
                <motion.div
                  key={i}
                  className="flex items-center gap-4"
                  initial={{ opacity: 0, x: 20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: 0.4 + i * 0.1 }}
                >
                  <div className="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center">
                    <div className="w-3 h-3 rounded-full bg-primary" />
                  </div>
                  <div>
                    <div className="font-medium text-white">{item.label}</div>
                    <div className="text-sm text-white/60">{item.desc}</div>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
