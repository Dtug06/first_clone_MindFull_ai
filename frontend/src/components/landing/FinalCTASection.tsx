import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import FloatingJellyfishBackground from '../ui/FloatingJellyfishBackground';
import AnimatedGradientBlob from '../ui/AnimatedGradientBlob';
import { Heart, ArrowRight } from 'lucide-react';

export default function FinalCTASection() {
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
            <span className="text-sm font-medium text-primary">Begin your journey</span>
          </div>

          <h2 className="text-3xl md:text-4xl lg:text-5xl font-semibold text-textMain mb-6">
            Begin with one gentle check-in.
          </h2>
          
          <p className="text-lg text-textMuted max-w-2xl mx-auto mb-10 leading-relaxed">
            Every journey begins with a single step. Take that step today — 
            your future self will thank you.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <motion.div
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <Link 
                to="/app/check-in" 
                className="btn-primary inline-flex items-center gap-2 text-lg px-8 py-4"
              >
                <Heart className="w-5 h-5" />
                Try MindBridge AI
              </Link>
            </motion.div>
            <motion.div
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              <a 
                href="#features" 
                className="btn-secondary inline-flex items-center gap-2 text-lg px-8 py-4"
              >
                Learn more
                <ArrowRight className="w-5 h-5" />
              </a>
            </motion.div>
          </div>

          <motion.p
            className="mt-8 text-sm text-textMuted"
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            transition={{ delay: 0.5 }}
          >
            Free to start. No credit card required. Your data stays private.
          </motion.p>
        </motion.div>
      </div>
    </section>
  );
}
