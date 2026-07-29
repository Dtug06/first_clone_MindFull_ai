import { motion } from 'framer-motion';
import SectionHeader from '../ui/SectionHeader';

export default function ProblemSection() {
  const painPoints = [
    {
      quote: "I don't know how I really feel.",
      description: "Emotions can be complex and hard to name.",
    },
    {
      quote: "I don't know where to start.",
      description: "The journey to better mental health feels overwhelming.",
    },
    {
      quote: "I'm afraid to share with others.",
      description: "Fear of judgment prevents seeking support.",
    },
    {
      quote: "I can't track my emotions over time.",
      description: "Without data, patterns remain invisible.",
    },
    {
      quote: "Professional support feels expensive or far away.",
      description: "Accessibility remains a significant barrier.",
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
          title="You are not alone"
          subtitle="Many young people face similar challenges when it comes to mental health support."
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
            MindBridge AI was designed to address these gaps — providing a safe, accessible, 
            and supportive space to begin your mental wellness journey.
          </p>
        </motion.div>
      </div>
    </section>
  );
}
