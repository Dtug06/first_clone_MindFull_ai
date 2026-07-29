import { motion } from 'framer-motion';
import SectionHeader from '../ui/SectionHeader';
import CalmCard from '../ui/CalmCard';
import { MessageCircle, Heart, BarChart2, BookOpen, Sparkles, Shield } from 'lucide-react';

const features = [
  {
    icon: MessageCircle,
    title: "AI Companion",
    description: "A gentle, non-judgmental companion that listens, supports, and guides you through difficult moments. Available 24/7.",
    color: "#5F9E97",
    details: ["Emotional support", "Thoughtful responses", "Safe conversations"],
  },
  {
    icon: Heart,
    title: "Mood Check-in",
    description: "Quick daily check-ins to help you understand and track your emotional patterns over time.",
    color: "#C8766B",
    details: ["Quick & simple", "Track trends", "Personal insights"],
  },
  {
    icon: BarChart2,
    title: "Mental Health Dashboard",
    description: "Visualize your emotional journey with beautiful charts and personalized analytics.",
    color: "#6F86A6",
    details: ["Mood trends", "Sleep quality", "Goal tracking"],
  },
  {
    icon: BookOpen,
    title: "Self-help Library",
    description: "A curated collection of evidence-based exercises, techniques, and resources.",
    color: "#D8C7A8",
    details: ["CBT exercises", "Breathing techniques", "Sleep hygiene"],
  },
  {
    icon: Sparkles,
    title: "Recommendation Engine",
    description: "Personalized suggestions based on your unique emotional profile and needs.",
    color: "#5F9E97",
    details: ["Tailored for you", "Adaptive learning", "Smart suggestions"],
  },
  {
    icon: Shield,
    title: "Emergency Support",
    description: "Careful monitoring with safe escalation paths when risk signals are detected.",
    color: "#C8766B",
    details: ["Risk detection", "Expert connection", "Safety first"],
  },
];

export default function FeaturesSection() {
  return (
    <section id="features" className="py-24 bg-surfaceMuted relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-primary/20 to-transparent" />
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-secondary/20 to-transparent" />

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SectionHeader
          title="Your wellness toolkit"
          subtitle="Everything you need to understand, support, and nurture your mental health."
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
