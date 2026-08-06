import { motion, AnimatePresence } from 'framer-motion';
import { NavLink, useLocation } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Users, 
  AlertTriangle, 
  UserCog, 
  BookOpen, 
  Brain, 
  Building2, 
  Settings,
  LogOut,
  X
} from 'lucide-react';
import LanguageSwitcher from '../ui/LanguageSwitcher';
import { useLanguage } from '../../i18n';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}
export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  const location = useLocation();
  const { t } = useLanguage();

  const navItems = [
    { path: '/admin', icon: LayoutDashboard, label: t.nav.overview, exact: true },
    { path: '/admin/users', icon: Users, label: t.nav.userManagement },
    { path: '/admin/risk', icon: AlertTriangle, label: t.nav.riskMonitoring },
    { path: '/admin/experts', icon: UserCog, label: t.nav.expertManagement },
    { path: '/admin/content', icon: BookOpen, label: t.nav.contentLibrary },
    { path: '/admin/ai', icon: Brain, label: t.nav.aiKnowledgeBase },
    { path: '/admin/organizations', icon: Building2, label: t.nav.organizations },
    { path: '/admin/settings', icon: Settings, label: t.nav.settingsLogs },
  ];

  return (
    <>
      {/* Mobile overlay */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            className="fixed inset-0 bg-black/30 backdrop-blur-sm z-40 lg:hidden"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <motion.aside
        className={`
          fixed top-0 left-0 h-full w-64 bg-surface border-r border-gray-100 z-50
          lg:translate-x-0 lg:z-30
          transition-transform duration-300 ease-out
          ${isOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="p-6 border-b border-gray-100">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary to-primaryDark flex items-center justify-center">
                  <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2C8 2 5 5 5 9c0 3 2 5 3 6v3c0 1 1 2 2 2h4c1 0 2-1 2-2v-3c1-1 3-3 3-6 0-4-3-7-7-7zm-2 16v-2h4v2h-4zm3-12c2 0 4 2 4 4s-2 4-4 4-4-2-4-4 2-4 4-4z"/>
                  </svg>
                </div>
                <div>
                  <h1 className="font-semibold text-textMain">MindBridge</h1>
                  <p className="text-xs text-textMuted">{t.nav.adminPortal}</p>
                </div>
              </div>
              <button
                className="lg:hidden p-2 rounded-lg hover:bg-gray-100"
                onClick={onClose}
              >
                <X className="w-5 h-5 text-textMuted" />
              </button>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
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
                    flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200
                    ${isActive 
                      ? 'bg-primary/10 text-primary' 
                      : 'text-textMuted hover:bg-gray-50 hover:text-textMain'
                    }
                  `}
                >
                  <item.icon className="w-5 h-5" />
                  <span className="font-medium text-sm">{item.label}</span>
                  {isActive && (
                    <motion.div
                      className="ml-auto w-1.5 h-1.5 rounded-full bg-primary"
                      layoutId="activeIndicator"
                    />
                  )}
                </NavLink>
              );
            })}
          </nav>

          {/* Footer */}
          <div className="p-4 border-t border-gray-100 space-y-2">
            <div className="px-2">
              <LanguageSwitcher variant="pill" />
            </div>
            <button className="flex items-center gap-3 px-4 py-3 w-full text-textMuted hover:text-softWarning hover:bg-softWarning/5 rounded-xl transition-all">
              <LogOut className="w-5 h-5" />
              <span className="font-medium text-sm">{t.common.signOut}</span>
            </button>
          </div>
        </div>
      </motion.aside>
    </>
  );
}
