import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import JellyfishMascot from '../ui/JellyfishMascot';
import FloatingJellyfishBackground from '../ui/FloatingJellyfishBackground';
import AnimatedGradientBlob from '../ui/AnimatedGradientBlob';
import CalmCard from '../ui/CalmCard';
import { Heart, MessageCircle, TrendingUp, Clock } from 'lucide-react';

export default function HeroSection() {
  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden bg-gradient-hero w-full">
      {/* Background elements - wrapped in overflow-hidden container */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <FloatingJellyfishBackground count={3} opacity={0.08} />
        <AnimatedGradientBlob 
          className="absolute -top-20 -left-20 sm:top-20 sm:-left-40 opacity-30" 
          size="xl" 
          colors={['#5F9E97', '#6F86A6', '#D8C7A8']}
        />
        <AnimatedGradientBlob 
          className="absolute -bottom-20 -right-20 sm:bottom-20 sm:-right-40 opacity-20" 
          size="lg"
          colors={['#6F86A6', '#5F9E97', '#E8E4F2']}
        />
      </div>

      <div className="relative z-10 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-16 items-center">
          {/* Left: Text content */}
          <motion.div
            className="text-center lg:text-left w-full"
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: 'easeOut' }}
          >
            <motion.div
              className="inline-flex items-center gap-2 px-4 py-2 bg-primary/10 rounded-full mb-6"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.2 }}
            >
              <Heart className="w-4 h-4 text-primary" />
              <span className="text-sm font-medium text-primary">AI-Powered Mental Wellness</span>
            </motion.div>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-semibold leading-tight mb-6">
              <span className="text-textMain">A calmer bridge to your</span>
              <br />
              <span className="bg-gradient-to-r from-primary via-primaryDark to-secondary bg-clip-text text-transparent">
                inner world.
              </span>
            </h1>

            <p className="text-lg text-textMuted max-w-xl mx-auto lg:mx-0 mb-8 leading-relaxed">
              MindBridge AI helps young people check in with their emotions, talk with a gentle AI companion, 
              receive self-help guidance, and connect with experts when they need it most.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
              <motion.div
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <Link to="/app/check-in" className="btn-primary inline-flex items-center gap-2">
                  <Heart className="w-5 h-5" />
                  Start your check-in
                </Link>
              </motion.div>
              <motion.div
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <a href="#how-it-works" className="btn-secondary inline-flex items-center gap-2">
                  <Clock className="w-5 h-5" />
                  Explore how it works
                </a>
              </motion.div>
            </div>

            {/* Trust indicators */}
            <div className="mt-10 flex flex-wrap items-center justify-center lg:justify-start gap-6 text-sm text-textMuted">
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
                <span>AI Safety Checked</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-secondary animate-pulse" />
                <span>Privacy Protected</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-2 h-2 rounded-full bg-accent animate-pulse" />
                <span>Expert Supported</span>
              </div>
            </div>
          </motion.div>

          {/* Right: Visual cards - hidden on mobile, shown on lg+ */}
          <motion.div
            className="relative h-[500px] hidden lg:block w-full"
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, delay: 0.3 }}
          >
            {/* Floating cards */}
            <motion.div
              className="absolute top-0 right-0"
              animate={{ y: [0, -15, 0] }}
              transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
            >
              <CalmCard className="w-64" glass>
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                    <MessageCircle className="w-5 h-5 text-primary" />
                  </div>
                  <span className="font-medium text-textMain">AI Companion</span>
                </div>
                <p className="text-sm text-textMuted">Your gentle guide is here to listen and support you.</p>
              </CalmCard>
            </motion.div>

            <motion.div
              className="absolute top-1/3 left-0"
              animate={{ y: [0, -20, 0] }}
              transition={{ duration: 5, repeat: Infinity, ease: 'easeInOut', delay: 0.5 }}
            >
              <CalmCard className="w-56" glass>
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-8 h-8 rounded-lg bg-softWarning/10 flex items-center justify-center">
                    <span className="text-lg">😊</span>
                  </div>
                  <span className="text-sm font-medium text-textMain">Today's Mood</span>
                </div>
                <div className="text-2xl font-semibold text-primary">7.2</div>
                <p className="text-xs text-textMuted mt-1">Feeling hopeful</p>
              </CalmCard>
            </motion.div>

            <motion.div
              className="absolute bottom-20 right-8"
              animate={{ y: [0, -12, 0] }}
              transition={{ duration: 4.5, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
            >
              <CalmCard className="w-52" glass>
                <div className="flex items-center gap-2 mb-2">
                  <TrendingUp className="w-4 h-4 text-primary" />
                  <span className="text-sm font-medium text-textMain">7-day trend</span>
                </div>
                <div className="h-12 flex items-end gap-1">
                  {[65, 72, 68, 75, 70, 78, 74].map((h, i) => (
                    <motion.div
                      key={i}
                      className="flex-1 bg-primary/30 rounded-t"
                      initial={{ height: 0 }}
                      animate={{ height: `${h}%` }}
                      transition={{ delay: 0.5 + i * 0.1, duration: 0.5 }}
                    />
                  ))}
                </div>
              </CalmCard>
            </motion.div>

            {/* Center jellyfish */}
            <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2">
              <JellyfishMascot size="xl" animated />
            </div>
          </motion.div>
        </div>
      </div>

      {/* Scroll indicator */}
      <motion.div
        className="absolute bottom-8 left-0 right-0 z-20 flex justify-center"
        animate={{ y: [0, 10, 0] }}
        transition={{ duration: 2, repeat: Infinity }}
      >
        <div className="w-6 h-10 border-2 border-textMuted/30 rounded-full flex justify-center">
          <motion.div
            className="w-1.5 h-3 bg-primary/50 rounded-full mt-2"
            animate={{ y: [0, 12, 0], opacity: [1, 0.3, 1] }}
            transition={{ duration: 2, repeat: Infinity }}
          />
        </div>
      </motion.div>
    </section>
  );
}