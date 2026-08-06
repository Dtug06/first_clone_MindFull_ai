import { motion } from 'framer-motion';
import SectionHeader from '../ui/SectionHeader';
import CalmCard from '../ui/CalmCard';
import { MessageCircle, Heart, BarChart2, BookOpen, Sparkles, Shield } from 'lucide-react';
import { useLanguage } from '../../i18n';

export default function FeaturesSection() {
  const { t } = useLanguage();

  const features = [
    {
      icon: MessageCircle,
      title: t.landing.feature2Title,
      description: t.landing.feature2Desc,
      color: "#5F9E97",
      details: [t.landing.feature2Detail1, t.landing.feature2Detail2, t.landing.feature2Detail3],
    },
    {
      icon: Heart,
      title: t.landing.feature1Title,
      description: t.landing.feature1Desc,
      color: "#C8766B",
      details: [t.landing.feature1Detail1, t.landing.feature1Detail2, t.landing.feature1Detail3],
    },
    {
      icon: BarChart2,
      title: t.landing.feature5Title,
      description: t.landing.feature5Desc,
      color: "#6F86A6",
      details: [t.landing.feature5Detail1, t.landing.feature5Detail2, t.landing.feature5Detail3],
    },
    {
      icon: BookOpen,
      title: t.landing.feature3Title,
      description: t.landing.feature3Desc,
      color: "#D8C7A8",
      details: [t.landing.feature3Detail1, t.landing.feature3Detail2, t.landing.feature3Detail3],
    },
    {
      icon: Sparkles,
      title: t.landing.feature4Title,
      description: t.landing.feature4Desc,
      color: "#5F9E97",
      details: [t.landing.feature4Detail1, t.landing.feature4Detail2, t.landing.feature4Detail3],
    },
    {
      icon: Shield,
      title: t.landing.feature6Title,
      description: t.landing.feature6Desc,
      color: "#C8766B",
      details: [t.landing.feature6Detail1, t.landing.feature6Detail2, t.landing.feature6Detail3],
    },
  ];

  return (
    <section id="features" className="py-24 bg-surfaceMuted relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-secondary/20 to-transparent" />

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader
          title={t.landing.featuresTitle}
          subtitle={t.landing.featuresSubtitle}
        />

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <motion.div
              key={index}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-50px' }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
            >
              <CalmCard className="h-full" hover>
                <div className="flex items-start gap-4">
                  <div 
                    className="w-14 h-14 rounded-2xl flex items-center justify-center flex-shrink-0"
                    style={{ backgroundColor: `${feature.color}15` }}
                  >
                    <feature.icon 
                      className="w-7 h-7" 
                      style={{ color: feature.color }}
                    />
                  </div>
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-textMain mb-2">
                      {feature.title}
                    </h3>
                    <p className="text-sm text-textMuted mb-4 leading-relaxed">
                      {feature.description}
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {feature.details.map((detail, i) => (
                        <span
                          key={i}
                          className="px-2 py-1 text-xs font-medium rounded-full"
                          style={{ 
                            backgroundColor: `${feature.color}10`,
                            color: feature.color,
                          }}
                        >
                          {detail}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </CalmCard>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
