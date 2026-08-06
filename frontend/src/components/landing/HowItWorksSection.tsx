import { motion } from 'framer-motion';
import SectionHeader from '../ui/SectionHeader';
import { UserPlus, ClipboardList, Brain, Heart, Sparkles, TrendingUp } from 'lucide-react';
import { useLanguage } from '../../i18n';

export default function HowItWorksSection() {
  const { t } = useLanguage();

  const steps = [
    {
      icon: UserPlus,
      title: t.landing.howStep1Title,
      description: t.landing.howStep1Desc,
      color: "#5F9E97",
    },
    {
      icon: ClipboardList,
      title: t.landing.howStep2Title,
      description: t.landing.howStep2Desc,
      color: "#6F86A6",
    },
    {
      icon: Brain,
      title: t.landing.howStep3Title,
      description: t.landing.howStep3Desc,
      color: "#D8C7A8",
    },
    {
      icon: Heart,
      title: t.landing.howStep4Title,
      description: t.landing.howStep4Desc,
      color: "#5F9E97",
    },
    {
      icon: Sparkles,
      title: t.landing.howStep5Title,
      description: t.landing.howStep5Desc,
      color: "#6F86A6",
    },
  ];

  return (
    <section id="how-it-works" className="py-24 bg-background relative overflow-hidden">
      {/* Decorative elements */}
      <div className="absolute top-20 left-10 w-64 h-64 bg-primary/5 rounded-full blur-3xl" />
      <div className="absolute bottom-20 right-10 w-80 h-80 bg-secondary/5 rounded-full blur-3xl" />

      <div className="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader
          title={t.landing.howTitle}
          subtitle={t.landing.howSubtitle}
        />

        {/* Steps */}
        <div className="relative">
          {/* Connection line */}
          <div className="hidden lg:block absolute top-1/2 left-0 right-0 h-0.5 bg-gradient-to-r from-primary/20 via-primary/40 to-primary/20 transform -translate-y-1/2" />

          <div className="grid md:grid-cols-2 lg:grid-cols-5 gap-8">
            {steps.map((step, index) => (
              <motion.div
                key={index}
                className="relative"
                initial={{ opacity: 0, y: 30 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: '-50px' }}
                transition={{ duration: 0.5, delay: index * 0.15 }}
              >
                <div className="flex flex-col items-center text-center">
                  {/* Icon circle */}
                  <motion.div
                    className="relative w-20 h-20 rounded-full bg-surface shadow-soft-lg border border-gray-100 flex items-center justify-center mb-6 z-10"
                    whileHover={{ scale: 1.1 }}
                    style={{ 
                      boxShadow: `0 8px 30px ${step.color}20`,
                    }}
                  >
                    <div 
                      className="w-12 h-12 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${step.color}15` }}
                    >
                      <step.icon 
                        className="w-6 h-6" 
                        style={{ color: step.color }}
                      />
                    </div>
                    
                    {/* Step number */}
                    <div 
                      className="absolute -top-2 -right-2 w-8 h-8 rounded-full text-white text-sm font-bold flex items-center justify-center"
                      style={{ backgroundColor: step.color }}
                    >
                      {index + 1}
                    </div>
                  </motion.div>

                  <h3 className="text-lg font-semibold text-textMain mb-2">
                    {step.title}
                  </h3>
                  <p className="text-sm text-textMuted leading-relaxed">
                    {step.description}
                  </p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>

        {/* Final result */}
        <motion.div
          className="mt-16 text-center"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ delay: 0.5 }}
        >
          <div className="inline-flex items-center gap-3 px-6 py-4 bg-gradient-to-r from-primary/10 to-secondary/10 rounded-full">
            <TrendingUp className="w-5 h-5 text-primary" />
            <span className="text-textMain font-medium">
              {t.landing.howResult}
            </span>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
