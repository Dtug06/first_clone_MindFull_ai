import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import FloatingJellyfishBackground from '../ui/FloatingJellyfishBackground';
import AnimatedGradientBlob from '../ui/AnimatedGradientBlob';
import { Heart, ArrowRight } from 'lucide-react';
import { useLanguage } from '../../i18n';

export default function FinalCTASection() {
  const { t } = useLanguage();

  return (
    <section className="py-24 bg-gradient-to-br from-primary/5 via-background to-secondary/5 relative overflow-hidden">
      {/* Background elements */}
      <FloatingJellyfishBackground count={4} opacity={0.05} />
      <AnimatedGradientBlob 
        className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" 
        size="xl" 
        colors={['#5F9E97', '#6F86A6', '#D8C7A8']}
      />

      <div className="relative max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
        >
          <div className="inline-flex items-center gap-2 px-4 py-2 bg-primary/10 rounded-full mb-6">
            <Heart className="w-4 h-4 text-primary" />
            <span className="text-sm font-medium text-primary">{t.landing.ctaBadge}</span>
          </div>

          <h2 className="text-3xl md:text-4xl lg:text-5xl font-semibold text-textMain mb-6">
            {t.landing.ctaTitle}
          </h2>
          
          <p className="text-lg text-textMuted max-w-2xl mx-auto mb-10 leading-relaxed">
            {t.landing.ctaDescription}
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <motion.div
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <Link 
                to="/app/daily"
                className="btn-primary inline-flex items-center gap-2 text-lg px-8 py-4"
              >
                <Heart className="w-5 h-5" />
                {t.landing.ctaTryButton}
              </Link>
            </motion.div>
            <motion.div
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <button
                type="button"
                onClick={() => document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' })}
                className="btn-secondary inline-flex items-center gap-2 text-lg px-8 py-4"
              >
                {t.landing.ctaLearnMore}
                <ArrowRight className="w-5 h-5" />
              </button>
            </motion.div>
          </div>

          <motion.p
            className="mt-8 text-sm text-textMuted"
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            transition={{ delay: 0.5 }}
          >
            {t.landing.ctaFootnote}
          </motion.p>
        </motion.div>
      </div>
    </section>
  );
}
