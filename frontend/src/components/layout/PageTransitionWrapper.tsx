import { motion } from 'framer-motion';
import { ReactNode } from 'react';

interface PageTransitionWrapperProps {
  children: ReactNode;
  fullHeight?: boolean;
}

const pageVariants = {
  initial: {
    opacity: 0,
    y: 20,
  },
  in: {
    opacity: 1,
    y: 0,
  },
  out: {
    opacity: 0,
    y: -20,
  },
};

const pageTransition = {
  type: 'tween',
  ease: 'anticipate',
  duration: 0.4,
};

export default function PageTransitionWrapper({ children, fullHeight = false }: PageTransitionWrapperProps) {
  return (
    <motion.div
      initial="initial"
      animate="in"
      exit="out"
      variants={pageVariants}
      transition={pageTransition}
      className={fullHeight ? 'h-full flex flex-col' : 'min-h-full'}
    >
      {children}
    </motion.div>
  );
}
