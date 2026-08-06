import { motion, AnimatePresence } from 'framer-motion';
import { NavLink, useLocation } from 'react-router-dom';
import { Home, Heart, MessageCircle, BarChart2, BookOpen, AlertCircle, X } from 'lucide-react';
import { useLanguage } from '../../i18n';

interface MobileNavigationProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function MobileNavigation({ isOpen, onClose }: MobileNavigationProps) {
  const location = useLocation();
  const { t } = useLanguage();

  const navItems = [
    { path: '/app', icon: Home, label: t.nav.home, exact: true },
    { path: '/app/daily', icon: Heart, label: t.nav.checkIn },
    { path: '/app/chat', icon: MessageCircle, label: t.nav.aiChat },
    { path: '/app/dashboard', icon: BarChart2, label: t.nav.dashboard },
    { path: '/app/library', icon: BookOpen, label: t.nav.library },
    { path: '/app/emergency', icon: AlertCircle, label: t.nav.emergency },
  ];

  return (
    <>
      {/* Overlay */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            className="fixed inset-0 bg-black/30 backdrop-blur-sm z-40"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
        )}
      </AnimatePresence>

      {/* Bottom Navigation */}
      <motion.nav
        className="fixed bottom-0 left-0 right-0 z-50 bg-surface/95 backdrop-blur-lg border-t border-gray-100 lg:hidden"
        initial={{ y: 100 }}
        animate={{ y: 0 }}
        transition={{ duration: 0.3 }}
      >
        <div className="grid grid-cols-6 gap-1 px-1 py-2 safe-area-inset-bottom max-w-screen-sm mx-auto">
          {navItems.map((item) => {
            const isActive = item.exact 
              ? location.pathname === item.path
              : location.pathname.startsWith(item.path);

            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={`
                  flex flex-col items-center gap-0.5 sm:gap-1 px-1 py-2 rounded-xl transition-all duration-200 min-w-0
                  ${isActive ? 'text-primary' : 'text-textMuted'}
                `}
              >
                <div className="relative flex-shrink-0">
                  <item.icon className="w-5 h-5" />
                  {isActive && (
                    <motion.div
                      className="absolute -inset-2 bg-primary/10 rounded-full"
                      layoutId="mobileActive"
                      initial={false}
                    />
                  )}
                </div>
                <span className={`text-[10px] sm:text-xs font-medium truncate w-full text-center ${isActive ? 'text-primary' : ''}`}>
                  {item.label}
                </span>
              </NavLink>
            );
          })}
        </div>
      </motion.nav>

      {/* Full-screen Menu */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            className="fixed inset-0 z-50 lg:hidden"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <div className="absolute inset-0 bg-oceanDeep/50 backdrop-blur-lg" onClick={onClose} />
            <motion.div
              className="absolute top-0 right-0 w-72 max-w-[80vw] h-full bg-surface shadow-soft-xl"
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            >
              <div className="flex flex-col h-full p-6">
                <div className="flex justify-end mb-6">
                  <button
                    className="p-2 rounded-xl hover:bg-gray-100"
                    onClick={onClose}
                  >
                    <X className="w-5 h-5 text-textMuted" />
                  </button>
                </div>
                
                <div className="space-y-2">
                  {navItems.map((item) => {
                    const isActive = item.exact 
                      ? location.pathname === item.path
                      : location.pathname.startsWith(item.path);

                    return (
                      <NavLink
                        key={item.path}
                        to={item.path}
                        onClick={onClose}
                        className={`
                          flex items-center gap-4 px-4 py-4 rounded-2xl transition-all
                          ${isActive 
                            ? 'bg-primary/10 text-primary' 
                            : 'text-textMain hover:bg-gray-50'
                          }
                        `}
                      >
                        <item.icon className="w-6 h-6 flex-shrink-0" />
                        <span className="font-medium">{item.label}</span>
                      </NavLink>
                    );
                  })}
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
