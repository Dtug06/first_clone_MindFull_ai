import { motion } from 'framer-motion';
import SectionHeader from '../ui/SectionHeader';
import { useLanguage } from '../../i18n';

export default function ProblemSection() {
  const { t } = useLanguage();

  const painPoints = [
    {
      quote: t.landing.problemQuote1,
      description: t.landing.problemDesc1,
    },
    {
      quote: t.landing.problemQuote2,
      description: t.landing.problemDesc2,
    },
    {
      quote: t.landing.problemQuote3,
      description: t.landing.problemDesc3,
    },
    {
      quote: t.landing.problemQuote4,
      description: t.landing.problemDesc4,
    },
    {
      quote: t.landing.problemQuote5,
      description: t.landing.problemDesc5,
    },
  ];

  return (
    <section id="problem" className="py-24 bg-surfaceMuted relative overflow-hidden">
      {/* Subtle background pattern */}
      <div className="absolute inset-0 opacity-30">
        <svg className="w-full h-full" viewBox="0 0 100 100" preserveAspectRatio="none">
          <pattern id="dots" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
            <circle cx="2" cy="2" r="0.5" fill="#5F9E97" opacity="0.3" />
          </pattern>
          <rect width="100%" height="100%" fill="url(#dots)" />
        </svg>
      </div>

      <div className="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader
          title={t.landing.problemTitle}
          subtitle={t.landing.problemSubtitle}
        />

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {painPoints.map((point, index) => (
            <motion.div
              key={index}
              className="bg-surface rounded-3xl p-6 shadow-soft border border-gray-100"
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-50px' }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
              whileHover={{ y: -4, boxShadow: '0 12px 40px rgba(38, 50, 56, 0.1)' }}
            >
              <div className="text-4xl mb-4 opacity-40">"</div>
              <p className="text-lg font-medium text-textMain mb-2 italic">
                {point.quote}
              </p>
              <p className="text-sm text-textMuted">
                {point.description}
              </p>
            </motion.div>
          ))}
        </div>

        <motion.div
          className="mt-16 text-center"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ delay: 0.5 }}
        >
          <p className="text-lg text-textMuted max-w-2xl mx-auto">
            {t.landing.problemOutro}
          </p>
        </motion.div>
      </div>
    </section>
  );
}
