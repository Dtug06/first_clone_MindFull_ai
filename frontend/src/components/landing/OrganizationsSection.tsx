import { motion } from 'framer-motion';
import { Building2, Users, BarChart2, Shield } from 'lucide-react';
import { useLanguage } from '../../i18n';

export default function OrganizationsSection() {
  const { t } = useLanguage();

  const orgFeatures = [
    {
      icon: BarChart2,
      title: t.landing.orgsFeature1Title,
      description: t.landing.orgsFeature1Desc,
    },
    {
      icon: Shield,
      title: t.landing.orgsFeature2Title,
      description: t.landing.orgsFeature2Desc,
    },
    {
      icon: Users,
      title: t.landing.orgsFeature3Title,
      description: t.landing.orgsFeature3Desc,
    },
  ];

  const topics = [
    { topic: t.admin.resourceAcademic, count: 156 },
    { topic: t.admin.resourceWorkLife, count: 134 },
    { topic: t.admin.resourceMindfulness, count: 98 },
  ];

  return (
    <section className="py-24 bg-surfaceMuted relative overflow-hidden">
      <div className="absolute inset-0">
        <div className="absolute top-20 left-20 w-64 h-64 bg-primary/5 rounded-full blur-3xl" />
        <div className="absolute bottom-20 right-20 w-80 h-80 bg-secondary/5 rounded-full blur-3xl" />
      </div>

      <div className="relative max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left: Content */}
          <motion.div
            initial={{ opacity: 0, x: -30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8 }}
          >
            <div className="inline-flex items-center gap-2 px-4 py-2 bg-secondary/10 rounded-full mb-6">
              <Building2 className="w-4 h-4 text-secondary" />
              <span className="text-sm font-medium text-secondary">{t.landing.orgsBadge}</span>
            </div>
            
            <h2 className="text-3xl md:text-4xl font-semibold text-textMain mb-6">
              {t.landing.orgsTitle}
            </h2>
            
            <p className="text-lg text-textMuted mb-8 leading-relaxed">
              {t.landing.orgsDescription}
            </p>

            <div className="space-y-4">
              {orgFeatures.map((feature, i) => (
                <motion.div
                  key={i}
                  className="flex items-start gap-4 p-4 bg-surface rounded-2xl"
                  initial={{ opacity: 0, y: 10 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: 0.2 + i * 0.1 }}
                >
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <feature.icon className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <h4 className="font-medium text-textMain">{feature.title}</h4>
                    <p className="text-sm text-textMuted">{feature.description}</p>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>

          {/* Right: Dashboard preview */}
          <motion.div
            className="relative"
            initial={{ opacity: 0, x: 30 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            <div className="bg-surface rounded-3xl p-6 shadow-soft-lg border border-gray-100">
              {/* Dashboard header */}
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h4 className="font-semibold text-textMain">{t.landing.orgsDashboardTitle}</h4>
                  <p className="text-sm text-textMuted">{t.landing.orgsDashboardSubtitle}</p>
                </div>
                <div className="px-3 py-1 bg-primary/10 rounded-full text-xs font-medium text-primary">
                  {t.landing.orgsPrivacyBadge}
                </div>
              </div>

              {/* Stats */}
              <div className="grid grid-cols-2 gap-4 mb-6">
                {[
                  { label: t.landing.orgsParticipation, value: '67%', change: '+12%' },
                  { label: t.landing.orgsAvgMood, value: '6.4', change: '+0.3' },
                ].map((stat, i) => (
                  <div key={i} className="bg-surfaceMuted rounded-xl p-4">
                    <div className="text-2xl font-semibold text-textMain">{stat.value}</div>
                    <div className="flex items-center justify-between">
                      <span className="text-xs text-textMuted">{stat.label}</span>
                      <span className="text-xs text-primary font-medium">{stat.change}</span>
                    </div>
                  </div>
                ))}
              </div>

              {/* Mini chart */}
              <div className="bg-surfaceMuted rounded-xl p-4 mb-4">
                <div className="text-sm font-medium text-textMain mb-3">{t.landing.orgsTrendTitle}</div>
                <div className="flex items-end gap-2 h-16">
                  {[65, 68, 72, 70, 75, 78, 74].map((h, i) => (
                    <div
                      key={i}
                      className="flex-1 bg-primary/30 rounded-t transition-all hover:bg-primary/50"
                      style={{ height: `${h}%` }}
                    />
                  ))}
                </div>
                <div className="flex justify-between mt-2 text-xs text-textMuted">
                  <span>Mon</span>
                  <span>Sun</span>
                </div>
              </div>

              {/* Topics */}
              <div>
                <div className="text-sm font-medium text-textMain mb-3">{t.landing.orgsTopicsTitle}</div>
                <div className="space-y-2">
                  {topics.map((item, i) => (
                    <div key={i} className="flex items-center justify-between">
                      <span className="text-sm text-textMuted">{item.topic}</span>
                      <span className="text-sm text-textMuted">{item.count}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Floating badge */}
            <motion.div
              className="absolute -top-4 -right-4 px-4 py-2 bg-primary text-white rounded-full text-sm font-medium shadow-glow"
              animate={{ y: [0, -5, 0] }}
              transition={{ duration: 3, repeat: Infinity }}
            >
              {t.landing.orgsAnonymousBadge}
            </motion.div>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
