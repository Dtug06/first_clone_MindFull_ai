import { motion } from 'framer-motion';

interface SectionHeaderProps {
  title: string;
  subtitle?: string;
  centered?: boolean;
  light?: boolean;
}

export default function SectionHeader({ 
  title, 
  subtitle, 
  centered = true,
  light = false 
}: SectionHeaderProps) {
  return (
    <motion.div 
      className={`mb-12 ${centered ? 'text-center' : ''}`}
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-100px' }}
      transition={{ duration: 0.6, ease: 'easeOut' }}
    >
      <h2 className={`text-3xl md:text-4xl font-semibold mb-4 ${light ? 'text-white' : 'text-textMain'}`}>
        {title}
      </h2>
      {subtitle && (
        <p className={`text-lg max-w-2xl ${centered ? 'mx-auto' : ''} ${light ? 'text-white/80' : 'text-textMuted'}`}>
          {subtitle}
        </p>
      )}
      <motion.div 
        className={`h-1 w-20 mx-auto mt-6 rounded-full bg-gradient-to-r from-primary to-primaryDark`}
        initial={{ width: 0 }}
        whileInView={{ width: 80 }}
        viewport={{ once: true }}
        transition={{ duration: 0.8, delay: 0.2 }}
      />
    </motion.div>
  );
}
