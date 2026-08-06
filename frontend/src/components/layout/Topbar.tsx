import { motion } from 'framer-motion';
import { Menu, Bell, Search } from 'lucide-react';
import { useState } from 'react';
import { useLanguage } from '../../i18n';
import LanguageSwitcher from '../ui/LanguageSwitcher';

interface TopbarProps {
  onMenuClick: () => void;
  title?: string;
}
export default function Topbar({ onMenuClick, title }: TopbarProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const { t } = useLanguage();

  return (
    <header className="sticky top-0 z-20 bg-surface/80 backdrop-blur-lg border-b border-gray-100">
      <div className="flex items-center justify-between px-4 lg:px-6 py-4">
        <div className="flex items-center gap-4">
          <button
            className="lg:hidden p-2 rounded-xl hover:bg-gray-100 transition-colors"
            onClick={onMenuClick}
          >
            <Menu className="w-5 h-5 text-textMain" />
          </button>
          
          {title && (
            <motion.h1 
              className="text-lg font-semibold text-textMain"
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
            >
              {title}
            </motion.h1>
          )}
        </div>

        <div className="hidden md:flex items-center flex-1 max-w-md mx-4">
          <div className="relative w-full">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-textMuted" />
            <input
              type="text"
              placeholder={t.common.search}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-surfaceMuted rounded-xl text-sm text-textMain placeholder:text-textMuted/60 focus:outline-none focus:ring-2 focus:ring-primary/20 border border-transparent focus:border-primary/20 transition-all"
            />
          </div>
        </div>

        <div className="flex items-center gap-3">
          <LanguageSwitcher variant="pill" className="hidden sm:block" />
          <LanguageSwitcher variant="compact" className="sm:hidden" />
          <motion.button
            className="relative p-2 rounded-xl hover:bg-gray-100 transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            <Bell className="w-5 h-5 text-textMuted" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-softWarning rounded-full" />
          </motion.button>
          
          <motion.button
            className="flex items-center gap-2 p-1.5 pr-3 rounded-xl hover:bg-gray-50 transition-colors"
            whileHover={{ scale: 1.02 }}
          >
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary/80 to-primaryDark/80 flex items-center justify-center text-white text-sm font-medium">
              A
            </div>
            <span className="hidden sm:block text-sm font-medium text-textMain">{t.common.admin}</span>
          </motion.button>
        </div>
      </div>
    </header>
  );
}
