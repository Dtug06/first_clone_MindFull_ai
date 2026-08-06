import { motion } from 'framer-motion';
import SectionHeader from '../ui/SectionHeader';
import { useLanguage } from '../../i18n';

export default function SafetyPrivacySection() {
  const { t } = useLanguage();

  const safetyItems = [
    t.landing.safetyList1,
    t.landing.safetyList2,
    t.landing.safetyList3,
    t.landing.safetyList4,
    t.landing.safetyList5,
  ];

  const privacyItems = [
    t.landing.privacyList1,
    t.landing.privacyList2,
    t.landing.privacyList3,
    t.landing.privacyList4,
    t.landing.privacyList5,
  ];

  return (
    <section id="safety" className="py-24 bg-background relative overflow-hidden">
      {/* Subtle gradient orbs */}
      <div className="absolute top-20 right-20 w-96 h-96 bg-primary/5 rounded-full blur-3xl" />
      <div className="absolute bottom-20 left-20 w-80 h-80 bg-secondary/5 rounded-full blur-3xl" />

      <div className="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader
          title={t.landing.safetyTitle}
          subtitle={t.landing.safetySubtitle}
        />

        {/* Main content */}
        <div className="grid lg:grid-cols-2 gap-8 mb-12">
          {/* Safety card */}
          <motion.div
            className="bg-surface rounded-3xl p-8 shadow-soft border border-gray-100"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
          >
            <div className="flex items-center gap-3 mb-6">
              <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                <svg className="w-6 h-6 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-textMain">{t.landing.safetyCardTitle}</h3>
            </div>
            
            <ul className="space-y-4">
              {safetyItems.map((item, i) => (
                <motion.li
                  key={i}
                  className="flex items-start gap-3"
                  initial={{ opacity: 0, x: -10 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                >
                  <div className="w-5 h-5 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <svg className="w-3 h-3 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <span className="text-textMuted">{item}</span>
                </motion.li>
              ))}
            </ul>
          </motion.div>

          {/* Privacy card */}
          <motion.div
            className="bg-surface rounded-3xl p-8 shadow-soft border border-gray-100"
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ delay: 0.1 }}
          >
            <div className="flex items-center gap-3 mb-6">
              <div className="w-12 h-12 rounded-xl bg-secondary/10 flex items-center justify-center">
                <svg className="w-6 h-6 text-secondary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-textMain">{t.landing.privacyCardTitle}</h3>
            </div>
            
            <ul className="space-y-4">
              {privacyItems.map((item, i) => (
                <motion.li
                  key={i}
                  className="flex items-start gap-3"
                  initial={{ opacity: 0, x: -10 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.1 }}
                >
                  <div className="w-5 h-5 rounded-full bg-secondary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <svg className="w-3 h-3 text-secondary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <span className="text-textMuted">{item}</span>
                </motion.li>
              ))}
            </ul>
          </motion.div>
        </div>

        {/* Important disclaimer */}
        <motion.div
          className="bg-lavenderMist/30 rounded-2xl p-6 text-center"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ delay: 0.3 }}
        >
          <p className="text-textMuted max-w-2xl mx-auto">
            <strong className="text-textMain">{t.landing.safetyDisclaimerLabel}</strong> {t.landing.safetyDisclaimer}
          </p>
        </motion.div>
      </div>
    </section>
  );
}
